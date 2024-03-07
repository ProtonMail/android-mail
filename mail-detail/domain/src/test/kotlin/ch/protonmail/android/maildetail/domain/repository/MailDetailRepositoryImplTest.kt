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

package ch.protonmail.android.maildetail.domain.repository

import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MailDetailRepositoryImplTest {

    private val enqueuer: MarkMessageAndConversationReadWorker.Enqueuer = mockk(relaxed = true)

    private val repository: MailDetailRepository = MailDetailRepositoryImpl(enqueuer)

    @Test
    fun `should enqueue worker with correct parameters`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.AugWeatherForecast
        val conversationId = ConversationIdSample.WeatherForecast

        // When
        repository.markMessageAndConversationReadIfAllRead(userId, messageId, conversationId)

        // Then
        coVerify {
            enqueuer.enqueue(
                userId = userId,
                params = MarkMessageAndConversationReadWorker.params(
                    userId,
                    messageId,
                    conversationId
                ),
                initialDelay = 2.seconds
            )
        }
    }
}
