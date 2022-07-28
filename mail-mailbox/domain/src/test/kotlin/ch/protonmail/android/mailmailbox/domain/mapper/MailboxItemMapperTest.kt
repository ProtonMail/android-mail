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

package ch.protonmail.android.mailmailbox.domain.mapper

import ch.protonmail.android.mailmailbox.domain.model.toMailboxItem
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MailboxItemMapperTest {

    @Test
    fun `when mapping message to mailbox item num messages is always 1`() {
        val message = MessageTestData.buildMessage(userId, "id")

        val actual = message.toMailboxItem(emptyMap())

        assertEquals(1, actual.numMessages)
    }

    @Test
    fun `when mapping conversation to mailbox item 'replied' 'replied all' and 'forwarded' flags are always false`() {
        val message = ConversationTestData.buildConversation(userId, "id")

        val actual = message.toMailboxItem(emptyMap())

        assertFalse(actual.isReplied)
        assertFalse(actual.isRepliedAll)
        assertFalse(actual.isForwarded)
    }
}
