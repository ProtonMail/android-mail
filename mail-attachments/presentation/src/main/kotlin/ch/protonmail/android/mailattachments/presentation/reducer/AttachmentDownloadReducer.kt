/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailattachments.presentation.reducer

import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.R
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailattachments.presentation.model.AttachmentDownloadState
import javax.inject.Inject

/**
 * Pure transitions for [AttachmentDownloadState]. Only one download runs at a time: the tapped pill spins
 * until the file is ready or the download fails, and a tap landing on top of a running download is refused
 * with a message rather than queued — the caller decides which case applies and calls the matching method.
 *
 * Holds no use cases and no coroutine scope; owning the download job stays with the caller, where it
 * legitimately differs (the mailbox drops it on a location change, content search on a new query).
 */
class AttachmentDownloadReducer @Inject constructor() {

    fun downloadStarted(current: AttachmentDownloadState, attachmentId: AttachmentIdUiModel) =
        current.copy(downloadingAttachmentId = attachmentId)

    /** A second tap while a download is running: warn, and leave the running download alone. */
    fun downloadAlreadyInProgress(current: AttachmentDownloadState) =
        current.copy(error = Effect.of(TextUiModel.TextRes(R.string.attachment_download_in_progress)))

    fun downloadReady(current: AttachmentDownloadState, intentValues: OpenAttachmentIntentValues) = current.copy(
        downloadingAttachmentId = null,
        openAttachment = Effect.of(intentValues)
    )

    fun downloadFailed(current: AttachmentDownloadState) = current.copy(
        downloadingAttachmentId = null,
        error = Effect.of(TextUiModel.TextRes(R.string.attachment_download_error))
    )

    fun downloadCancelled(current: AttachmentDownloadState) = current.copy(downloadingAttachmentId = null)
}
