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

import android.net.Uri
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.R
import ch.protonmail.android.mailattachments.presentation.model.AttachmentDownloadState
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AttachmentDownloadReducerTest {

    private val attachmentId = AttachmentIdUiModel("attachment-1")
    private val intentValues = OpenAttachmentIntentValues(
        openMode = AttachmentOpenMode.Open,
        name = "invoice.pdf",
        mimeType = "application/pdf",
        uri = mockk<Uri>()
    )

    private val reducer = AttachmentDownloadReducer()

    @Test
    fun `should mark the tapped attachment as downloading`() {
        val result = reducer.downloadStarted(AttachmentDownloadState.Initial, attachmentId)

        assertEquals(attachmentId, result.downloadingAttachmentId)
    }

    @Test
    fun `should warn without disturbing the running download when another is requested`() {
        val downloading = reducer.downloadStarted(AttachmentDownloadState.Initial, attachmentId)

        val result = reducer.downloadAlreadyInProgress(downloading)

        // The in-flight download keeps its spinner; only the warning is raised.
        assertEquals(attachmentId, result.downloadingAttachmentId)
        assertEquals(
            TextUiModel.TextRes(R.string.attachment_download_in_progress),
            result.error.consume()
        )
    }

    @Test
    fun `should clear the spinner and hand over the file when the download is ready`() {
        val downloading = reducer.downloadStarted(AttachmentDownloadState.Initial, attachmentId)

        val result = reducer.downloadReady(downloading, intentValues)

        assertNull(result.downloadingAttachmentId)
        assertEquals(intentValues, result.openAttachment.consume())
    }

    @Test
    fun `should clear the spinner and report the error when the download fails`() {
        val downloading = reducer.downloadStarted(AttachmentDownloadState.Initial, attachmentId)

        val result = reducer.downloadFailed(downloading)

        assertNull(result.downloadingAttachmentId)
        assertEquals(
            TextUiModel.TextRes(R.string.attachment_download_error),
            result.error.consume()
        )
    }

    @Test
    fun `should clear the spinner silently when the download is cancelled`() {
        val downloading = reducer.downloadStarted(AttachmentDownloadState.Initial, attachmentId)

        val result = reducer.downloadCancelled(downloading)

        assertNull(result.downloadingAttachmentId)
        assertNull(result.error.consume())
    }
}
