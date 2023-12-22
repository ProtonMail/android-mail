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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
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
import kotlin.test.assertEquals

class MarkConversationsAsReadTest {

    private val userId = UserIdSample.Primary
    private val conversationIds = listOf(ConversationIdSample.WeatherForecast, ConversationIdSample.AlphaAppFeedback)

    private val conversationRepository: ConversationRepository = mockk()
    private val decrementUnreadCount: DecrementUnreadCount = mockk()

    private val markRead = MarkConversationsAsRead(conversationRepository, decrementUnreadCount)

    @Test
    fun `returns error when repository fails`() = runTest {
        // given
        val error = DataErrorSample.NoCache.left()
        coEvery { conversationRepository.markRead(userId, conversationIds) } returns error
        coEvery { conversationRepository.observeCachedConversations(userId, conversationIds) } returns flowOf()

        // when
        val result = markRead(userId, conversationIds)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `returns updated conversation when repository succeeds`() = runTest {
        // given
        val conversation = listOf(ConversationSample.WeatherForecast, ConversationSample.AlphaAppFeedback).right()
        coEvery { conversationRepository.markRead(userId, conversationIds) } returns conversation
        coEvery { conversationRepository.observeCachedConversations(userId, conversationIds) } returns flowOf()

        // when
        val result = markRead(userId, conversationIds)

        // then
        assertEquals(conversation, result)
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
        coEvery { conversationRepository.markRead(userId, conversationIds) } returns conversations.right()
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(conversations)
        coEvery { decrementUnreadCount(userId, whetherForecastExpectedLabelIds) } just Runs
        coEvery { decrementUnreadCount(userId, alphaAppExpectedLabelIds) } just Runs

        // when
        markRead(userId, conversationIds)

        // then
        coVerify {
            decrementUnreadCount(userId, whetherForecastExpectedLabelIds)
            decrementUnreadCount(userId, alphaAppExpectedLabelIds)
        }
    }

}
