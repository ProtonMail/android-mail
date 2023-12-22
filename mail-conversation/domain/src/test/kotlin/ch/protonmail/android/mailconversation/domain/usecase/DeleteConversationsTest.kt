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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteConversationsTest {

    private val userId = UserIdSample.Primary
    private val conversationIds = listOf(ConversationIdSample.Invoices, ConversationIdSample.WeatherForecast)
    private val currentLabel = LabelIdSample.Trash

    private val conversationRepository = mockk<ConversationRepository>()
    private val decrementUnreadCount: DecrementUnreadCount = mockk()

    private val deleteConversations = DeleteConversations(conversationRepository, decrementUnreadCount)

    @Test
    fun `delete conversations calls repository with given parameters`() = runTest {
        // Given
        coEvery {
            conversationRepository.deleteConversations(userId, conversationIds, currentLabel)
        } returns Unit.right()
        coEvery { conversationRepository.observeCachedConversations(userId, conversationIds) } returns flowOf()

        // When
        deleteConversations(userId, conversationIds, currentLabel)

        // Then
        coVerify { conversationRepository.deleteConversations(userId, conversationIds, currentLabel) }
    }

    @Test
    fun `decrement unread count for each conversation's label that has unread messages`() = runTest {
        // given
        val forecastConversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Inbox,
                    numMessages = 2,
                    numUnread = 1
                ),
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Archive,
                    numMessages = 2,
                    numUnread = 0
                )
            )
        )
        val alphaAppConversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Archive,
                    numMessages = 2,
                    numUnread = 1
                ),
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Starred,
                    numMessages = 2,
                    numUnread = 1
                )
            )
        )
        val conversations = listOf(forecastConversation, alphaAppConversation)
        val whetherForecastExpectedLabelIds = listOf(LabelSample.Inbox.labelId)
        val alphaAppExpectedLabelIds = listOf(LabelSample.Archive.labelId, LabelSample.Starred.labelId)
        coEvery {
            conversationRepository.deleteConversations(userId, conversationIds, currentLabel)
        } returns Unit.right()
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(conversations)
        coEvery { decrementUnreadCount(userId, whetherForecastExpectedLabelIds) } just Runs
        coEvery { decrementUnreadCount(userId, alphaAppExpectedLabelIds) } just Runs

        // when
        deleteConversations(userId, conversationIds, currentLabel)

        // then
        coVerify {
            decrementUnreadCount(userId, whetherForecastExpectedLabelIds)
            decrementUnreadCount(userId, alphaAppExpectedLabelIds)
        }
    }

}
