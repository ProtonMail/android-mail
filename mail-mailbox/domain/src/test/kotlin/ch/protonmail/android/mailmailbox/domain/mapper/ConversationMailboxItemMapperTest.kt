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

import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationMailboxItemMapperTest {

    private val mapper = ConversationMailboxItemMapper()

    @Test
    fun `when mapping conversation to mailbox item 'replied' 'replied all' and 'forwarded' flags are always false`() {
        // Given
        val conversation = ConversationTestData.buildConversation(userId, "id")
        // When
        val actual = mapper.toMailboxItem(conversation, emptyMap())
        // Then
        assertFalse(actual.isReplied)
        assertFalse(actual.isRepliedAll)
        assertFalse(actual.isForwarded)
    }

    @Test
    fun `when mapping conversation to mailbox item all labelIds are preserved`() {
        // Given
        val labelIds = listOf("0", "5", "10", "customLabel")
        val conversation = ConversationTestData.buildConversation(userId, "id", labelIds = labelIds)
        // When
        val actual = mapper.toMailboxItem(conversation, emptyMap())
        // Then
        val expected = labelIds.map { LabelId(it) }
        assertEquals(expected, actual.labelIds)
    }

    @Test
    fun `when mapping conversation to mailbox item labels are sorted according to their order`() {
        // Given
        val labelIds = listOf("5", "0", "10")
        val labels = labelIds.associate { value ->
            LabelId(value) to buildLabel(value)
        }
        val conversation = ConversationTestData.buildConversation(userId, "id", labelIds = labelIds)
        // When
        val actual = mapper.toMailboxItem(conversation = conversation, labels = labels)
        // Then
        val expected = listOf("0", "5", "10").map(::buildLabel)
        assertEquals(expected, actual.labels)
    }

    @Test
    fun `when mapping conversation with 1 or more attachments to mailbox item then has attachments is true`() {
        // Given
        val conversation = ConversationTestData.buildConversation(userId, "id", numAttachments = 3)
        // When
        val actual = mapper.toMailboxItem(conversation, emptyMap())
        // Then
        assertTrue(actual.hasAttachments)
    }

    @Test
    fun `when mapping conversation with 0 attachments to mailbox item then has attachments is false`() {
        // Given
        val conversation = ConversationTestData.buildConversation(userId, "id", numAttachments = 0)
        // When
        val actual = mapper.toMailboxItem(conversation, emptyMap())
        // Then
        assertFalse(actual.hasAttachments)
    }

    @Test
    fun `when mapping conversation the expiration time is preserved in the mailbox item`() {
        // Given
        val expirationTime = 1000L
        val conversation = ConversationTestData.buildConversation(userId, "id", expirationTime = expirationTime)
        // When
        val mailboxItem = mapper.toMailboxItem(conversation, emptyMap())
        // Then
        assertEquals(expirationTime, mailboxItem.expirationTime)
    }

    private fun buildLabel(value: String) = LabelTestData.buildLabel(
        userId = userId,
        id = value,
        type = LabelType.MessageLabel,
        order = value.hashCode()
    )
}
