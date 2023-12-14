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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkMessagesAsUnreadTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.PlainTextMessage)
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val messageRepository = mockk<MessageRepository>()
    private val incrementUnreadCount = mockk<IncrementUnreadCount>()
    private val observeExclusiveMailLabels = mockk<ObserveExclusiveMailLabels> {
        every { this@mockk(userId) } returns flowOf(
            MailLabels(systemLabels = exclusiveMailLabels, folders = emptyList(), labels = emptyList())
        )
    }

    private val markUnread = MarkMessagesAsUnread(
        messageRepository,
        incrementUnreadCount,
        observeExclusiveMailLabels
    )

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // given
        val error = DataError.Local.NoDataCached.left()
        coEvery { messageRepository.markUnread(userId, messageIds) } returns error
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf()

        // when
        val result = markUnread(userId, messageIds)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `when repository succeed then list of messages is returned`() = runTest {
        // given
        val messages = listOf(MessageSample.Invoice, MessageSample.HtmlInvoice).right()
        coEvery { messageRepository.markUnread(userId, messageIds) } returns messages
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf()

        // when
        val result = markUnread(userId, messageIds)

        // then
        assertEquals(messages, result)
    }

    @Test
    fun `increment unread messages count for each read message that is being marked unread`() = runTest {
        // given
        val invoiceMessageExclusiveLabel = LabelIdSample.Inbox
        val htmlInvoiceMessageExclusiveLabel = LabelIdSample.Archive
        val messages = listOf(MessageSample.Invoice, MessageSample.HtmlInvoice, MessageSample.UnreadInvoice).right()
        coEvery { messageRepository.markUnread(userId, messageIds) } returns messages
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf(messages)
        coEvery { incrementUnreadCount(userId, invoiceMessageExclusiveLabel) } returns Unit.right()
        coEvery { incrementUnreadCount(userId, htmlInvoiceMessageExclusiveLabel) } returns Unit.right()

        // when
        markUnread(userId, messageIds)

        // then
        coVerifySequence {
            incrementUnreadCount(userId, htmlInvoiceMessageExclusiveLabel)
            incrementUnreadCount(userId, invoiceMessageExclusiveLabel)
        }
    }
}
