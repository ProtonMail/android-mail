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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.usecase.GetLabels
import ch.protonmail.android.mailmailbox.domain.mapper.ConversationMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetMailboxItemsTest {

    private val messageRepository = mockk<MessageRepository> {
        coEvery { getLocalMessages(any<UserId>(), any<PageKey>()) } returns listOf(
            // userId1
            buildMessage(userId, "1", time = 1000, labelIds = emptyList()),
            buildMessage(userId, "2", time = 2000, labelIds = listOf("4")),
            buildMessage(userId, "3", time = 3000, labelIds = listOf("0", "1"))
        )
    }
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { getLocalConversations(any(), any()) } returns listOf(
            // userId1
            ConversationWithContextTestData.conversation1Labeled,
            ConversationWithContextTestData.conversation2Labeled,
            ConversationWithContextTestData.conversation3Labeled
        )
    }
    private val getLabels = mockk<GetLabels> {
        coEvery { this@mockk(any(), any()) } returns listOf(
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "0"),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "1"),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "2"),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "3"),
            buildLabel(userId = userId, type = LabelType.MessageLabel, id = "4")
        ).right()
    }

    private val messageMailboxItemMapper = MessageMailboxItemMapper()
    private val conversationMailboxItemMapper = ConversationMailboxItemMapper()

    private lateinit var usecase: GetMailboxItems

    @Before
    fun setUp() {
        usecase = GetMailboxItems(
            getLabels,
            messageRepository,
            conversationRepository,
            messageMailboxItemMapper,
            conversationMailboxItemMapper
        )
    }

    @Test
    fun `invoke for Message, getLabels and loadMessages`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, pageKey)
            .getOrHandle(::error)

        // Then
        coVerify { getLabels(userId, LabelType.MessageLabel) }
        coVerify { getLabels(userId, LabelType.MessageLabel) }
        coVerify { messageRepository.getLocalMessages(userId, pageKey) }
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

    @Test
    fun `invoke for Conversation, getLabels and getConversations`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Conversation, pageKey)
            .getOrHandle(::error)

        // Then
        coVerify { getLabels(userId, LabelType.MessageLabel) }
        coVerify { getLabels(userId, LabelType.MessageFolder) }
        coVerify { conversationRepository.getLocalConversations(userId, pageKey) }
        assertEquals(3, mailboxItems.size)
        assertEquals(1, mailboxItems[0].labels.size)
        assertEquals(1, mailboxItems[1].labels.size)
        assertEquals(2, mailboxItems[2].labels.size)
        var previous = mailboxItems.first()
        mailboxItems.forEach { current ->
            assert(current.time >= previous.time)
            previous = current
        }
    }

    @Test
    fun `return data error when failing to get folders`() = runTest {
        // Given
        val remoteError = DataError.Remote.Http(NetworkError.NoNetwork)
        coEvery { getLabels.invoke(userId, LabelType.MessageFolder) } returns remoteError.left()

        // When
        val actual = usecase.invoke(userId, MailboxItemType.Message)

        // Then
        assertEquals(remoteError.left(), actual)
    }

    @Test
    fun `return data error when failing to get labels`() = runTest {
        // Given
        val remoteError = DataError.Remote.Http(NetworkError.ServerError)
        coEvery { getLabels.invoke(userId, LabelType.MessageLabel) } returns remoteError.left()

        // When
        val actual = usecase.invoke(userId, MailboxItemType.Conversation)

        // Then
        assertEquals(remoteError.left(), actual)
    }
}
