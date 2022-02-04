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

import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test

class MarkAsStaleMailboxItemsTest {

    private val userId1 = UserId("1")
    private val userId2 = UserId("2")
    private val userIds = listOf(userId1, userId2)
    private val labelId = LabelId("3")

    private val messageRepository = mockk<MessageRepository> {
        coEvery { this@mockk.markAsStale(any(), any()) } just Runs
    }

    private lateinit var usecase: MarkAsStaleMailboxItems

    @Before
    fun setUp() {
        usecase = MarkAsStaleMailboxItems(messageRepository)
    }

    @Test
    fun `invoke for Message, markAsStale Message`() = runTest {
        // When
        usecase.invoke(userIds, MailboxItemType.Message, labelId)

        // Then
        coVerify { messageRepository.markAsStale(userId1, labelId) }
        coVerify { messageRepository.markAsStale(userId2, labelId) }
    }

    @Test(expected = NotImplementedError::class)
    fun `invoke for Conversation, markAsStale Conversation`() = runTest {
        // When
        usecase.invoke(userIds, MailboxItemType.Conversation, labelId)

        // Then
        //coVerify { conversationRepository.markAsStale(userId1, labelId) }
        //coVerify { conversationRepository.markAsStale(userId2, labelId) }
    }
}
