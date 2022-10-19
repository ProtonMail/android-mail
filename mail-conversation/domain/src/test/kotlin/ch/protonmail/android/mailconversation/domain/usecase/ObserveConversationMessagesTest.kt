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

package ch.protonmail.android.mailconversation.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.conversation.ConversationIdTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveConversationMessagesTest {

    private val messageRepository: MessageRepository = mockk()
    private val observeConversationMessages = ObserveConversationMessages(messageRepository)

    @Test
    fun `calls the repository with correct parameters`() = runTest {
        // given
        val userId = UserIdTestData.Primary
        val conversationId = ConversationIdTestData.WeatherForecast
        val messages = listOf(
            MessageTestData.AugWeatherForecast,
            MessageTestData.SepWeatherForecast
        )
        every { messageRepository.observeCachedMessages(userId, conversationId) } returns flowOf(messages)

        // when
        observeConversationMessages(userId, conversationId).test {

            // then
            assertEquals(messages, awaitItem())
            verify { messageRepository.observeCachedMessages(userId, conversationId) }
            awaitComplete()
        }
    }
}
