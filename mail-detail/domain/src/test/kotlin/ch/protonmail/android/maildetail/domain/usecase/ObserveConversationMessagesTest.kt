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

package ch.protonmail.android.maildetail.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveConversationMessagesTest {

    private val conversationMessages = ConversationMessages(
        nonEmptyListOf(MessageSample.AugWeatherForecast),
        MessageIdSample.AugWeatherForecast
    )
    private val conversationRepository: ConversationRepository = mockk()
    private val observeConversationMessages = ObserveConversationMessages(
        conversationRepository = conversationRepository
    )

    @Test
    fun `when messages and labels are emitted, right model is emitted`() = runTest {
        // given
        val expected = ConversationMessages(
            nonEmptyListOf(MessageSample.AugWeatherForecast),
            MessageSample.AugWeatherForecast.messageId
        ).right()
        val labelId = LabelId("1")
        coEvery {
            conversationRepository.observeConversationMessages(
                UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId
            )
        } returns flowOf(conversationMessages.right())

        // when
        observeConversationMessages(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast,
            labelId
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `model contains only the correct labels and folders`() = runTest {
        // given
        val message = MessageSample.Invoice
        val expected = ConversationMessages(
            nonEmptyListOf(message),
            message.messageId
        ).right()
        val labelId = LabelId("1")
        coEvery {
            conversationRepository.observeConversationMessages(
                UserIdSample.Primary, ConversationIdSample.Invoices, labelId
            )
        } returns
            flowOf(ConversationMessages(nonEmptyListOf(message), MessageSample.Invoice.messageId).right())

        // when
        observeConversationMessages(
            UserIdSample.Primary,
            ConversationIdSample.Invoices,
            labelId
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when messages emits an error, the error is emitted`() = runTest {
        // given
        val error = ConversationError.NullValueReturned.left()
        val labelId = LabelId("1")
        coEvery {
            conversationRepository.observeConversationMessages(
                UserIdSample.Primary, ConversationIdSample.WeatherForecast, labelId
            )
        } returns flowOf(error)

        // when
        observeConversationMessages(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast,
            labelId
        ).test {

            // then
            assertEquals(error, awaitItem())
            awaitComplete()
        }
    }
}
