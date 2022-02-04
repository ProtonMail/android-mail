/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailmailbox.domain.getLabel
import ch.protonmail.android.mailmailbox.domain.getMessage
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetMailboxItemsTest {

    private val userId = UserId("1")

    private val messageRepository = mockk<MessageRepository> {
        coEvery { getMessages(any(), any()) } returns listOf(
            // userId1
            getMessage(userId, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId, "3", time = 3000, labelIds = listOf("0", "1")),
        )
    }
    private val labelRepository = mockk<LabelRepository> {
        coEvery { getLabels(any(), any()) } returns listOf(
            getLabel(userId, LabelType.MessageLabel, "0"),
            getLabel(userId, LabelType.MessageLabel, "1"),
            getLabel(userId, LabelType.MessageLabel, "2"),
            getLabel(userId, LabelType.MessageLabel, "3"),
            getLabel(userId, LabelType.MessageLabel, "4"),
        )
    }

    private lateinit var usecase: GetMailboxItems

    @Before
    fun setUp() {
        usecase = GetMailboxItems(messageRepository, labelRepository)
    }

    @Test
    fun `invoke for Message, getLabels and getMessages`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, pageKey)

        // Then
        coVerify { labelRepository.getLabels(userId, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId, LabelType.MessageFolder) }
        coVerify { messageRepository.getMessages(userId, pageKey) }
        assertEquals(3, mailboxItems.size)
        assertEquals(0, mailboxItems[0].labels.size)
        assertEquals(1, mailboxItems[1].labels.size)
        assertEquals(2, mailboxItems[2].labels.size)
        var previous = mailboxItems.first()
        mailboxItems.forEach { current ->
            assert(current.time >= previous.time)
            previous = current
        }
    }

    @Test(expected = NotImplementedError::class)
    fun `invoke for Conversation, getLabels and getConversations`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Conversation, pageKey)

        // Then
        coVerify { labelRepository.getLabels(userId, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId, LabelType.MessageFolder) }
        //coVerify { conversationRepository.getConversations(userId, pageKey) }
        assertEquals(3, mailboxItems.size)
        assertEquals(0, mailboxItems[0].labels.size)
        assertEquals(1, mailboxItems[1].labels.size)
        assertEquals(2, mailboxItems[2].labels.size)
        var previous = mailboxItems.first()
        mailboxItems.forEach { current ->
            assert(current.time >= previous.time)
            previous = current
        }
    }
}
