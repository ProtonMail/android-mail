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

package ch.protonmail.android.mailcontentsearch.presentation.reducer

import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailattachments.presentation.R as AttachmentR
import ch.protonmail.android.mailattachments.presentation.reducer.AttachmentDownloadReducer
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchOperation
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ContentSearchReducerTest {

    private val maxSelectionMessage = DefinitiveActionResult(TextUiModel.Text("Maximum selection reached"))
    private val bulkActionMessageFactory = mockk<BulkActionMessageFactory> {
        every { maxSelectionReachedResult() } returns maxSelectionMessage
    }
    private val reducer = ContentSearchReducer(
        selectionReducer = SelectionStateReducer(),
        attachmentDownloadReducer = AttachmentDownloadReducer(),
        bulkActionMessageFactory = bulkActionMessageFactory,
        bottomBarReducer = BottomBarReducer()
    )

    private fun item(id: String) = MailboxItemUiModelPreviewData.Message.WeatherForecastAug.copy(id = id)

    @Test
    fun `entering selection mode selects the tapped item`() {
        val result = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        assertEquals(setOf("1"), result.selectionState.selectedItems.map { it.id }.toSet())
        assertTrue(result.inSelectionMode)
    }

    @Test
    fun `toggling an unselected item adds it to the selection`() {
        val selecting = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val result = reducer.newStateFrom(selecting, ContentSearchOperation.ToggleSelection(item("2")))

        assertEquals(setOf("1", "2"), result.selectionState.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `toggling an already-selected item removes it`() {
        val selecting = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val result = reducer.newStateFrom(selecting, ContentSearchOperation.ToggleSelection(item("1")))

        assertEquals(emptySet(), result.selectionState.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `toggling a new item at the selection limit reports the cap instead of adding it`() {
        val entering = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("0"))
        )
        val atLimit = (1 until SelectionState.MaxItemSelectionLimit).fold(entering) { state, i ->
            reducer.newStateFrom(state, ContentSearchOperation.ToggleSelection(item("$i")))
        }
        assertEquals(SelectionState.MaxItemSelectionLimit, atLimit.selectionState.selectedItems.size)

        val result = reducer.newStateFrom(atLimit, ContentSearchOperation.ToggleSelection(item("overflow")))

        assertEquals(SelectionState.MaxItemSelectionLimit, result.selectionState.selectedItems.size)
        assertEquals(maxSelectionMessage, result.actionMessage.consume())
    }

    @Test
    fun `items removed from selection drop only those ids`() {
        val selecting = reducer.newStateFrom(
            reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.EnterSelectionMode(item("1"))),
            ContentSearchOperation.ToggleSelection(item("2"))
        )

        val result = reducer.newStateFrom(selecting, ContentSearchOperation.ItemsRemovedFromSelection(listOf("1")))

        assertEquals(setOf("2"), result.selectionState.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `exiting selection mode clears the selection and the delete dialog`() {
        val selecting = reducer.newStateFrom(
            ContentSearchState.Initial.copy(showDeleteDialog = true),
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val result = reducer.newStateFrom(selecting, ContentSearchOperation.ExitSelectionMode)

        assertEquals(SelectionState.None, result.selectionState)
        assertEquals(false, result.showDeleteDialog)
    }

    @Test
    fun `exiting selection mode hides the bottom bar instead of leaving the stale selection's actions shown`() {
        val shownWithStaleActions = reducer.newStateFrom(
            reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.EnterSelectionMode(item("1"))),
            ContentSearchOperation.BottomBarUpdated(
                BottomBarState.Data.Shown(BottomBarTarget.Mailbox, persistentListOf())
            )
        )

        val result = reducer.newStateFrom(shownWithStaleActions, ContentSearchOperation.ExitSelectionMode)

        assertEquals(
            BottomBarState.Data.Hidden(BottomBarTarget.Mailbox, persistentListOf()),
            result.bottomBarState
        )
    }

    @Test
    fun `bottom bar updated replaces the bottom bar state`() {
        val newState = BottomBarState.Data.Shown(BottomBarTarget.Mailbox, persistentListOf())

        val result = reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.BottomBarUpdated(newState))

        assertEquals(newState, result.bottomBarState)
    }

    @Test
    fun `show and dismiss delete dialog toggle the flag`() {
        val shown = reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.ShowDeleteDialog)
        assertTrue(shown.showDeleteDialog)

        val dismissed = reducer.newStateFrom(shown, ContentSearchOperation.DismissDeleteDialog)
        assertEquals(false, dismissed.showDeleteDialog)
    }

    @Test
    fun `avatar images updated replaces the avatar images map`() {
        val avatarImages = AvatarImagesUiModel(mapOf("a" to mockk()))

        val result = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AvatarImagesUpdated(avatarImages)
        )

        assertEquals(avatarImages, result.avatarImages)
    }

    @Test
    fun `attachment download started marks the pill as downloading`() {
        val attachmentId = AttachmentIdUiModel("attachment-1")

        val result = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AttachmentDownloadStarted(attachmentId)
        )

        assertEquals(attachmentId, result.downloadingAttachmentId)
    }

    @Test
    fun `attachment ready clears the spinner and exposes the file to open`() {
        val downloading = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AttachmentDownloadStarted(AttachmentIdUiModel("attachment-1"))
        )
        val intentValues = OpenAttachmentIntentValues(
            openMode = AttachmentOpenMode.Open,
            name = "invoice.pdf",
            mimeType = "application/pdf",
            uri = mockk()
        )

        val result = reducer.newStateFrom(downloading, ContentSearchOperation.AttachmentReady(intentValues))

        assertNull(result.downloadingAttachmentId)
        assertEquals(intentValues, result.openAttachment.consume())
    }

    @Test
    fun `attachment download cancelled clears the spinner silently`() {
        val downloading = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AttachmentDownloadStarted(AttachmentIdUiModel("attachment-1"))
        )

        val result = reducer.newStateFrom(downloading, ContentSearchOperation.AttachmentDownloadCancelled)

        assertNull(result.downloadingAttachmentId)
        assertNull(result.errorMessage.consume())
    }

    @Test
    fun `attachment download in progress warns without disturbing the running download`() {
        val downloading = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AttachmentDownloadStarted(AttachmentIdUiModel("attachment-1"))
        )

        val result = reducer.newStateFrom(downloading, ContentSearchOperation.AttachmentDownloadInProgress)

        assertEquals(AttachmentIdUiModel("attachment-1"), result.downloadingAttachmentId)
        assertEquals(
            TextUiModel.TextRes(AttachmentR.string.attachment_download_in_progress),
            result.errorMessage.consume()
        )
    }

    @Test
    fun `attachment download failed clears the spinner and reports the error`() {
        val downloading = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.AttachmentDownloadStarted(AttachmentIdUiModel("attachment-1"))
        )

        val result = reducer.newStateFrom(downloading, ContentSearchOperation.AttachmentDownloadFailed)

        assertNull(result.downloadingAttachmentId)
        assertEquals(
            TextUiModel.TextRes(AttachmentR.string.attachment_download_error),
            result.errorMessage.consume()
        )
    }

    @Test
    fun `mark selection as read and unread flip the cached read flag`() {
        val selecting = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val read = reducer.newStateFrom(selecting, ContentSearchOperation.MarkSelectionAsRead)
        assertTrue(read.selectionState.selectedItems.all { it.isRead })

        val unread = reducer.newStateFrom(read, ContentSearchOperation.MarkSelectionAsUnread)
        assertTrue(unread.selectionState.selectedItems.none { it.isRead })
    }

    @Test
    fun `star and unstar selection flip the cached starred flag`() {
        val selecting = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val starred = reducer.newStateFrom(selecting, ContentSearchOperation.StarSelection)
        assertTrue(starred.selectionState.selectedItems.all { it.isStarred })

        val unstarred = reducer.newStateFrom(starred, ContentSearchOperation.UnStarSelection)
        assertTrue(unstarred.selectionState.selectedItems.none { it.isStarred })
    }

    @Test
    fun `show action message surfaces the given result`() {
        val actionResult = DefinitiveActionResult(TextUiModel.Text("3 moved"))

        val result = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.ShowActionMessage(actionResult)
        )

        assertEquals(actionResult, result.actionMessage.consume())
    }

    @Test
    fun `show error surfaces the given message`() {
        val message = TextUiModel.Text("Something went wrong")

        val result = reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.ShowError(message))

        assertEquals(message, result.errorMessage.consume())
    }
}
