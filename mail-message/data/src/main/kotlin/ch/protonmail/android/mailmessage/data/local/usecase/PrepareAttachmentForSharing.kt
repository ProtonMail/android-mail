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

package ch.protonmail.android.mailmessage.data.local.usecase

import java.io.File
import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcommon.domain.system.ContentValuesProvider
import ch.protonmail.android.mailmessage.data.local.provider.GetUriFromMediaScanner
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import javax.inject.Inject

class PrepareAttachmentForSharing @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    private val buildVersionProvider: BuildVersionProvider,
    private val contentValuesProvider: ContentValuesProvider,
    private val getUriFromMediaScanner: GetUriFromMediaScanner,
    private val sanitizeFileName: SanitizeFullFileName,
    private val generateUniqueFileName: GenerateUniqueFileName,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        decryptedByteArray: ByteArray
    ): Either<PrepareAttachmentForSharingError, Uri> = withContext(ioDispatcher) {
        val message =
            messageRepository.getLocalMessageWithBody(userId, messageId)
                ?: return@withContext PrepareAttachmentForSharingError.MessageNotFound.left()
        val messageAttachment = message.messageBody.attachments.firstOrNull { it.attachmentId == attachmentId }
            ?: return@withContext PrepareAttachmentForSharingError.AttachmentNotFound.left()

        val contentResolver = context.contentResolver
        val fileName = generateUniqueFileName(sanitizeFileName(messageAttachment.name))
        val attachmentMimeType = messageAttachment.mimeType
        try {
            if (buildVersionProvider.sdkInt() >= Build.VERSION_CODES.Q) {
                return@withContext handleAttachmentForQAndLater(
                    fileName,
                    attachmentMimeType,
                    contentResolver,
                    decryptedByteArray
                ).right()
            } else {
                return@withContext handleAttachmentBeforeQ(fileName, attachmentMimeType, decryptedByteArray)
            }
        } catch (t: Throwable) {
            Timber.e(t, "Preparing attachment failed")
            return@withContext PrepareAttachmentForSharingError.PreparingAttachmentFailed.left()
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun handleAttachmentForQAndLater(
        fileName: String,
        attachmentMimeType: String,
        contentResolver: ContentResolver,
        decryptedByteArray: ByteArray
    ): Uri {
        val values = contentValuesProvider.provideContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, attachmentMimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val newUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        Timber.v("saveAttachment attachmentMimeType: $attachmentMimeType, newUri: $newUri")

        newUri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.sink().buffer().apply {
                    writeAll(decryptedByteArray.inputStream().source())
                    close()
                }
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(it, values, null, null)
        } ?: throw IllegalStateException("Media Store insert failed")
        return newUri
    }

    private suspend fun handleAttachmentBeforeQ(
        fileName: String,
        attachmentMimeType: String,
        decryptedByteArray: ByteArray
    ): Either<PrepareAttachmentForSharingError, Uri> {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).let {
            if (!it.exists()) {
                it.createNewFile()
            }
            it.sink().buffer().use { sink -> sink.write(decryptedByteArray) }
            getUriFromMediaScanner(it, attachmentMimeType)?.right()
                ?: PrepareAttachmentForSharingError.PreparingAttachmentFailed.left()
        }
    }
}

sealed interface PrepareAttachmentForSharingError {
    object MessageNotFound : PrepareAttachmentForSharingError
    object AttachmentNotFound : PrepareAttachmentForSharingError
    object PreparingAttachmentFailed : PrepareAttachmentForSharingError
}


