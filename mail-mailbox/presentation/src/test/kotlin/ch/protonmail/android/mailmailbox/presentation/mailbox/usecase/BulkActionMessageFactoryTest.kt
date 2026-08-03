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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class BulkActionMessageFactoryTest {

    private val destination = MailLabelText("My folder")
    private val mailLabelTextMapper = mockk<MailLabelTextMapper> {
        every { mapToString(destination) } returns "My folder"
    }
    private val factory = BulkActionMessageFactory(mailLabelTextMapper)

    @Test
    fun `move result uses the message plural and is undoable when not grouping conversations`() {
        val result = factory.moveResult(destination, itemCount = 3, viewMode = ViewMode.NoConversationGrouping)

        assertEquals(
            UndoableActionResult(
                TextUiModel.PluralisedText(R.plurals.mailbox_action_move_message, 3, listOf("My folder"))
            ),
            result
        )
    }

    @Test
    fun `move result uses the conversation plural when grouping conversations`() {
        val result = factory.moveResult(destination, itemCount = 3, viewMode = ViewMode.ConversationGrouping)

        assertEquals(
            UndoableActionResult(
                TextUiModel.PluralisedText(R.plurals.mailbox_action_move_conversation, 3, listOf("My folder"))
            ),
            result
        )
    }

    @Test
    fun `delete result uses the message plural and is definitive when not grouping conversations`() {
        val result = factory.deleteResult(itemCount = 2, viewMode = ViewMode.NoConversationGrouping)

        assertEquals(DefinitiveActionResult(TextUiModel(R.plurals.mailbox_action_delete_message, 2)), result)
    }

    @Test
    fun `delete result uses the conversation plural when grouping conversations`() {
        val result = factory.deleteResult(itemCount = 2, viewMode = ViewMode.ConversationGrouping)

        assertEquals(DefinitiveActionResult(TextUiModel(R.plurals.mailbox_action_delete_conversation, 2)), result)
    }
}
