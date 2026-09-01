/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailattachments.domain.usecase.GetDecryptedAttachmentUri
import ch.protonmail.android.mailattachments.presentation.ExternalAttachmentErrorResult
import ch.protonmail.android.mailattachments.presentation.ExternalAttachmentsHandler
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.model.FileSaveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FileSaverViewModel @Inject constructor(
    private val externalAttachmentsHandler: ExternalAttachmentsHandler,
    private val getDecryptedAttachmentUri: GetDecryptedAttachmentUri
) : ViewModel() {

    private val mutableSaveState = MutableStateFlow<FileSaveState>(FileSaveState.Idle)
    val saveState = mutableSaveState.asStateFlow()

    fun requestSave(content: FileContent) {
        mutableSaveState.value = FileSaveState.RequestingSave(content)
    }

    fun performSave(destinationUri: Uri) {
        val content = (mutableSaveState.value as? FileSaveState.WaitingForUser)?.content ?: run {
            resetState()
            return
        }

        viewModelScope.launch {
            mutableSaveState.value = FileSaveState.Saving

            val sourceUri = resolveSourceUri(content) ?: return@launch

            externalAttachmentsHandler.copyUriToDestination(sourceUri = sourceUri, destinationUri = destinationUri)
                .onLeft { mutableSaveState.value = FileSaveState.Error(it) }
                .onRight { mutableSaveState.value = FileSaveState.Saved.UserPicked }
        }
    }

    fun performSaveToDownloadFolder(fileContent: FileContent) {
        viewModelScope.launch {
            mutableSaveState.value = FileSaveState.Saving

            val sourceUri = resolveSourceUri(fileContent) ?: return@launch

            externalAttachmentsHandler.saveFileToDownloadsFolder(
                sourceUri = sourceUri,
                fileName = fileContent.name,
                mimeType = fileContent.mimeType
            )
                .onLeft { mutableSaveState.value = FileSaveState.Error(it) }
                .onRight { mutableSaveState.value = FileSaveState.Saved.FallbackLocation }
        }
    }

    /**
     * Resolves the source Uri at the moment of the save.
     *
     * The Uri resolved when the save is requested cannot be reused reliably: the user may have left the picker
     * open for a long while and as the decrypted file lives in the app cache, it may have been evicted in the
     * meantime. Re-resolving re-downloads it when needed and is a no-op when it is still cached.
     *
     * Returns `null` and moves the state to [FileSaveState.Error] when the attachment can no longer be obtained.
     */
    private suspend fun resolveSourceUri(content: FileContent): Uri? =
        getDecryptedAttachmentUri(content.userId, content.attachmentId).fold(
            ifLeft = {
                Timber.w("Unable to resolve the attachment to save: $it")
                mutableSaveState.value = FileSaveState.Error(ExternalAttachmentErrorResult.SourceUnavailable)
                null
            },
            ifRight = { it }
        )

    fun resetState() {
        mutableSaveState.value = FileSaveState.Idle
    }

    fun markLaunchAsConsumed() {
        val current = mutableSaveState.value
        if (current is FileSaveState.RequestingSave) {
            mutableSaveState.value = FileSaveState.WaitingForUser(current.content)
        }
    }
}
