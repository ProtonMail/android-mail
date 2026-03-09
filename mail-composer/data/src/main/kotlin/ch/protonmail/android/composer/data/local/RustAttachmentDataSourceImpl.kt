/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.composer.data.local

import android.net.Uri
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toAttachmentMetaDataWithState
import ch.protonmail.android.composer.data.mapper.toConvertError
import ch.protonmail.android.composer.data.mapper.toDeleteAttachmentError
import ch.protonmail.android.composer.data.mapper.toObserveAttachmentsError
import ch.protonmail.android.composer.data.wrapper.AttachmentsWrapper
import ch.protonmail.android.mailattachments.data.mapper.toAttachmentError
import ch.protonmail.android.mailattachments.data.mapper.toLocalAttachmentId
import ch.protonmail.android.mailattachments.domain.model.AddAttachmentError
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.ConvertAttachmentError
import ch.protonmail.android.mailcommon.data.file.FileInformation
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.AttachmentDeleteError
import ch.protonmail.android.mailmessage.data.local.AttachmentFileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import uniffi.mail_uniffi.AttachmentListAddInlineResult
import uniffi.mail_uniffi.AttachmentListAddResult
import uniffi.mail_uniffi.AttachmentListAttachmentsResult
import uniffi.mail_uniffi.AttachmentListRemoveResult
import uniffi.mail_uniffi.AttachmentListRemoveWithCidResult
import uniffi.mail_uniffi.AttachmentListWatcherStreamResult
import uniffi.mail_uniffi.VoidDraftAttachmentDispositionSwapResult
import uniffi.mail_uniffi.VoidProtonResult
import javax.inject.Inject

class RustAttachmentDataSourceImpl @Inject constructor(
    private val rustDraftDataSource: RustDraftDataSource,
    private val attachmentFileStorage: AttachmentFileStorage,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : RustAttachmentDataSource {

    override suspend fun observeAttachments(): Flow<Either<DataError, List<AttachmentMetadataWithState>>> =
        callbackFlow {
            Timber.d("rust-draft-attachments: Starting attachment observation")

            rustDraftDataSource.attachmentList().onLeft { error ->
                Timber.e("rust-draft-attachments: Failed to get attachment list: $error")
                send(error.left())
                close()
                return@callbackFlow

            }.onRight { attachmentListWrapper ->
                send(attachmentListWrapper.getAttachments())

                Timber.d("rust-draft-attachments: Got attachment list, creating watcher")
                when (val watcherResult = attachmentListWrapper.watcherStream()) {
                    is AttachmentListWatcherStreamResult.Error -> {
                        Timber.e("rust-draft-attachments: Failed to create watcher: ${watcherResult.v1}")
                        send(watcherResult.v1.toObserveAttachmentsError().left())
                        close()
                        return@callbackFlow
                    }

                    is AttachmentListWatcherStreamResult.Ok -> {
                        Timber.d("rust-draft-attachments: Created attachment list watcher")

                        val watcherStream = watcherResult.v1
                        while (true) {
                            when (watcherStream.nextAsync()) {
                                is VoidProtonResult.Error -> {
                                    Timber.w("rust-draft-attachments: received new watcher error")
                                    break
                                }
                                VoidProtonResult.Ok -> {
                                    val update = attachmentListWrapper.getAttachments()
                                    Timber.d("rust-draft-attachments: received new watcher event $update")
                                    send(update)
                                }
                            }
                        }
                    }
                }
            }

            awaitClose {
                Timber.d("rust-draft-attachments: Closing watcher")
            }
        }

    override suspend fun addAttachment(fileUri: Uri): Either<AddAttachmentError, Unit> =
        storeAttachmentToCache(fileUri) { attachmentListWrapper, fileInfo ->
            when (val addResult = attachmentListWrapper.addAttachment(fileInfo.path, fileInfo.name)) {
                is AttachmentListAddResult.Ok -> {
                    Timber.d("rust-draft-attachments: Added attachment: ${fileInfo.path}")
                    Unit.right()
                }

                is AttachmentListAddResult.Error -> {
                    Timber.e("rust-draft-attachments: Failed to add attachment: ${addResult.v1}")
                    addResult.v1.toAttachmentError().left()
                }
            }
        }

    override suspend fun addInlineAttachment(fileUri: Uri): Either<AddAttachmentError, String> =
        storeAttachmentToCache(fileUri) { attachmentListWrapper, fileInfo ->
            when (val addResult = attachmentListWrapper.addInlineAttachment(fileInfo.path, fileInfo.name)) {
                is AttachmentListAddInlineResult.Ok -> {
                    Timber.d("rust-draft-attachments: Added inline attachment: ${fileInfo.path}")
                    addResult.v1.right()
                }

                is AttachmentListAddInlineResult.Error -> {
                    Timber.e("rust-draft-attachments: Failed to add inline attachment: ${addResult.v1}")
                    addResult.v1.toAttachmentError().left()
                }
            }
        }

    override suspend fun removeAttachment(attachmentId: AttachmentId): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val listResult = rustDraftDataSource.attachmentList()

            return@withContext listResult.fold(
                ifLeft = { error ->
                    Timber.e("rust-draft-attachments: Failed to get attachment list: $error")
                    error.left()
                },
                ifRight = { attachmentListWrapper ->

                    when (
                        val removeResult =
                            attachmentListWrapper.removeAttachment(attachmentId.toLocalAttachmentId())
                    ) {
                        is AttachmentListRemoveResult.Ok -> {
                            Timber.d("rust-draft-attachments: Removed attachment: $attachmentId")
                            Unit.right()
                        }

                        is AttachmentListRemoveResult.Error -> {
                            Timber.e("rust-draft-attachments: Failed to remove attachment: ${removeResult.v1}")
                            removeResult.v1.toDataError().left()
                        }
                    }
                }
            )
        }

    override suspend fun removeInlineAttachment(cid: String): Either<AttachmentDeleteError, Unit> =
        withContext(ioDispatcher) {
            val listResult = rustDraftDataSource.attachmentList()

            return@withContext listResult.fold(
                ifLeft = { error ->
                    Timber.e("rust-draft-attachments: Failed to get attachment list: $error")
                    AttachmentDeleteError.Other(error).left()
                },
                ifRight = { attachmentListWrapper ->

                    when (val removeResult = attachmentListWrapper.removeInlineAttachment(cid)) {
                        is AttachmentListRemoveWithCidResult.Ok -> {
                            Timber.d("rust-draft-attachments: Removed inline attachment: $cid")
                            Unit.right()
                        }

                        is AttachmentListRemoveWithCidResult.Error -> {
                            Timber.e("rust-draft-attachments: Failed to remove inline attachment: ${removeResult.v1}")
                            removeResult.v1.toDeleteAttachmentError().left()
                        }
                    }
                }
            )
        }

    override suspend fun convertToAttachment(cid: String): Either<ConvertAttachmentError, Unit> =
        withContext(ioDispatcher) {
            val listResult = rustDraftDataSource.attachmentList()

            return@withContext listResult.fold(
                ifLeft = {
                    ConvertAttachmentError.Other(it).left()
                },
                ifRight = {
                    when (val result = it.transformToAttachment(cid)) {
                        is VoidDraftAttachmentDispositionSwapResult.Error -> {
                            Timber.e("rust-draft-attachments: Failed to convert attachment: $result")
                            result.v1.toConvertError().left()
                        }
                        VoidDraftAttachmentDispositionSwapResult.Ok -> {
                            Timber.d("rust-draft-attachments: converted inline img to attachment")
                            Unit.right()
                        }
                    }
                }
            )
        }

    private suspend fun AttachmentsWrapper.getAttachments(): Either<DataError, List<AttachmentMetadataWithState>> {
        return when (val result = this.attachments()) {
            is AttachmentListAttachmentsResult.Ok -> {
                val attachments = result.v1.map { it.toAttachmentMetaDataWithState() }
                attachments.right()
            }

            is AttachmentListAttachmentsResult.Error -> {
                result.v1.toObserveAttachmentsError().left()
            }
        }
    }

    private suspend fun <T> storeAttachmentToCache(
        fileUri: Uri,
        closure: suspend (AttachmentsWrapper, FileInformation) -> Either<AddAttachmentError, T>
    ): Either<AddAttachmentError, T> = withContext(ioDispatcher) {
        val listResult = rustDraftDataSource.attachmentList()

        return@withContext listResult.fold(
            ifLeft = { error ->
                Timber.e("rust-draft-attachments: Failed to get attachment list: $error")
                AddAttachmentError.Other(error).left()
            },
            ifRight = { attachmentListWrapper ->
                // Save attachment to local storage
                val fileInfo = attachmentFileStorage.saveAttachment(
                    attachmentListWrapper.attachmentUploadDirectory(),
                    fileUri
                ) ?: return@withContext AddAttachmentError.Other(DataError.Local.FailedToStoreFile).left()

                closure(attachmentListWrapper, fileInfo)
            }
        )
    }

}
