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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import ch.protonmail.android.maillabel.presentation.R as LabelR

internal class ActionResultMapperTest {

    private val mailLabelTextMapper = mockk<MailLabelTextMapper>()
    private val actionResultMapper = ActionResultMapper(mailLabelTextMapper)

    @Test
    fun `returns undoable result when operation is Archive`() {
        // Given
        val operation = ConversationDetailViewAction.MoveToArchive
        val expectedResult = UndoableActionResult(TextUiModel(R.string.conversation_moved_to_archive))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns undoable result when operation is MoveToSpam`() {
        // Given
        val operation = ConversationDetailViewAction.MoveToSpam
        val expectedResult = UndoableActionResult(TextUiModel(R.string.conversation_moved_to_spam))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns undoable result when operation is MoveToTrash`() {
        // Given
        val operation = ConversationDetailViewAction.MoveToTrash
        val expectedResult = UndoableActionResult(TextUiModel(R.string.conversation_moved_to_trash))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns undoable result when operation is MoveToInbox`() {
        // Given
        val operation = ConversationDetailViewAction.MoveToInbox
        val expectedResult = UndoableActionResult(TextUiModel(R.string.conversation_moved_to_inbox))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns undoable result with label text when operation is MoveToDestinationConfirmed`() {
        // Given
        val mailLabelText = MailLabelText("Inbox")
        val operation = ConversationDetailViewAction.MoveToCompleted(
            mailLabelText,
            MoveToBottomSheetEntryPoint.Conversation,
            isCategory = false
        )
        val expectedResult = UndoableActionResult(
            TextUiModel(R.string.conversation_moved_to_selected_destination, "Inbox")
        )
        every { mailLabelTextMapper.mapToString(mailLabelText) } returns "Inbox"

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns category result when operation is MoveToDestinationConfirmed for a category`() {
        // Given
        val mailLabelText = MailLabelText("Social")
        val operation = ConversationDetailViewAction.MoveToCompleted(
            mailLabelText,
            MoveToBottomSheetEntryPoint.Conversation,
            isCategory = true
        )
        val expectedResult = UndoableActionResult(
            TextUiModel(LabelR.string.move_to_category_action_message, "Social")
        )
        every { mailLabelTextMapper.mapToString(mailLabelText) } returns "Social"

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns definitive result when operation is LabelAsCompleted`() {
        // Given
        val operation = ConversationDetailViewAction.LabelAsCompleted(
            true,
            entryPoint = LabelAsBottomSheetEntryPoint.Conversation
        )
        val expectedResult = UndoableActionResult(TextUiModel(R.string.conversation_moved_to_archive))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns definitive result when operation is DeleteConfirmed`() {
        // Given
        val operation = ConversationDetailViewAction.DeleteConfirmed
        val expectedResult = DefinitiveActionResult(TextUiModel(R.string.conversation_deleted))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns null for unknown operation`() {
        // Given
        val operation: ConversationDetailOperation = mockk()

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `returns definitive result when operation is SnoozeCompleted`() {
        // Given
        val operation = ConversationDetailViewAction.SnoozeCompleted("snooze completed")
        val expectedResult = DefinitiveActionResult(TextUiModel("snooze completed"))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns definitive result when operation is LastMessageDeleted`() {
        // Given
        val operation = ConversationDetailEvent.LastMessageDeleted
        val expectedResult = DefinitiveActionResult(TextUiModel(R.string.message_deleted))

        // When
        val result = actionResultMapper.toActionResult(operation)

        // Then
        assertEquals(expectedResult, result)
    }
}
