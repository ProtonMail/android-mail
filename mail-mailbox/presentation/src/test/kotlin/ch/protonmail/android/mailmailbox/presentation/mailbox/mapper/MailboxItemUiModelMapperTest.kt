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

package ch.protonmail.android.mailmailbox.presentation.mailbox.mapper

import ch.protonmail.android.testdata.mailbox.MailboxTestData
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxItemUiModelMapperTest {

    private val mapper = MailboxItemUiModelMapper()

    @Test
    fun `when mailbox message item was replied ui model shows reply icon`() {
        val mailboxItem = MailboxTestData.repliedMailboxItem

        val actual = mapper.toUiModel(mailboxItem)

        assertTrue(actual.showRepliedIcon)
    }

    @Test
    fun `when mailbox message item was replied all ui model shows reply all icon`() {
        val mailboxItem = MailboxTestData.repliedAllMailboxItem

        val actual = mapper.toUiModel(mailboxItem)

        assertTrue(actual.showRepliedAllIcon)
        assertFalse(actual.showRepliedIcon)
    }

    @Test
    fun `when mailbox message item was forwarded ui model shows forwarded icon`() {
        val mailboxItem = MailboxTestData.allActionsMailboxItem

        val actual = mapper.toUiModel(mailboxItem)

        assertTrue(actual.showForwardedIcon)
    }

    @Test
    fun `mailbox items of conversation type never show any of reply, reply-all, forwarded icon`() {
        val mailboxItem = MailboxTestData.mailboxConversationItem

        val actual = mapper.toUiModel(mailboxItem)

        assertFalse(actual.showRepliedIcon)
        assertFalse(actual.showRepliedAllIcon)
        assertFalse(actual.showForwardedIcon)
    }

}
