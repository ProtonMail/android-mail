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
import kotlin.test.assertEquals

class MailboxItemUiModelMapperTest {

    private val mapper = MailboxItemUiModelMapper()

    @Test
    fun `when mailbox message item was replied ui model shows reply icon`() {
        val mailboxItem = MailboxTestData.repliedMailboxItem

        val actual = mapper.toUiModel(mailboxItem)

        assertEquals(true, actual.showRepliedIcon)
    }

    @Test
    fun `mailbox conversation items are always mapped to not show reply icon`() {
        val mailboxItem = MailboxTestData.mailboxConversationItem

        val actual = mapper.toUiModel(mailboxItem)

        assertEquals(false, actual.showRepliedIcon)
    }
}
