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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteMessagesTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.AugWeatherForecast)
    private val currentLabel = SystemLabelId.Trash.labelId

    private val messageRepository = mockk<MessageRepository>()
    private val decrementUnreadCount = mockk<DecrementUnreadCount>()

    private val deleteMessages = DeleteMessages(messageRepository, decrementUnreadCount)

    @Test
    fun `delete messages calls repository with given parameters`() = runTest {
        // Given
        coEvery { messageRepository.deleteMessages(userId, messageIds, currentLabel) } returns Unit.right()
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf()

        // When
        deleteMessages(userId, messageIds, currentLabel)

        // Then
        coVerify { messageRepository.deleteMessages(userId, messageIds, currentLabel) }
    }

    @Test
    fun `decrement unread messages count for each unread message that is deleted`() = runTest {
        // given
        val unreadMessage = MessageSample.UnreadInvoice
        val messages = listOf(MessageSample.Invoice, MessageSample.HtmlInvoice, unreadMessage).right()
        coEvery { messageRepository.deleteMessages(userId, messageIds, currentLabel) } returns Unit.right()
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf(messages)
        coEvery { decrementUnreadCount(userId, unreadMessage.labelIds) } returns Unit.right()

        // when
        deleteMessages(userId, messageIds, currentLabel)

        // then
        coVerifySequence { decrementUnreadCount(userId, unreadMessage.labelIds) }
    }

    @Test
    fun `delete messages with label calls repository with given parameters`() = runTest {
        // Given
        coJustRun { messageRepository.deleteMessages(userId, currentLabel) }

        // When
        deleteMessages(userId, currentLabel)

        // Then
        coVerify { messageRepository.deleteMessages(userId, currentLabel) }
    }
}
