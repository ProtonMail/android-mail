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

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithLabels
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetConversationsWithLabelsTest {

    private val messageLabelsFlow = flowOf(Either.Right(listOf(LabelSample.Archive)))
    private val messageFoldersFlow = flowOf(Either.Right(listOf(LabelSample.Document)))
    private val observeLabels: ObserveLabels = mockk {
        every { this@mockk.invoke(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns messageLabelsFlow
        every { this@mockk.invoke(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns messageFoldersFlow
    }
    private val conversationRepository: ConversationRepository = mockk {
        every {
            observeCachedConversations(UserIdSample.Primary, listOf(ConversationIdSample.WeatherForecast))
        } returns flowOf(nonEmptyListOf(ConversationSample.WeatherForecast))
    }
    private val getConversationMessagesWithLabels = GetConversationsWithLabels(
        observeLabels = observeLabels,
        conversationRepository = conversationRepository
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when messages and labels are emitted, right model is emitted`() = runTest {
        // Given
        val expected = listOf(
            ConversationWithLabels(
                conversation = ConversationSample.WeatherForecast,
                labels = emptyList()
            )
        ).right()

        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns
            flowOf(emptyList<Label>().right())
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns
            flowOf(emptyList<Label>().right())

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.WeatherForecast)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `model contains only the correct labels and folders`() = runTest {
        // Given
        val conversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.build(labelId = LabelIdSample.Archive),
                ConversationLabelSample.build(labelId = LabelIdSample.Document)
            )
        )
        val conversationWithLabels = ConversationWithLabels(
            conversation = conversation,
            labels = listOf(LabelSample.Archive, LabelSample.Document).sortedBy { it.order }
        )
        val allLabels = listOf(LabelSample.Document, LabelSample.News)
        val allFolders = listOf(LabelSample.Archive, LabelSample.Inbox)
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns
            flowOf(allLabels.right())
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns
            flowOf(allFolders.right())

        val expected = listOf(conversationWithLabels).right()

        every {
            conversationRepository.observeCachedConversations(
                UserIdSample.Primary,
                listOf(ConversationIdSample.Invoices)
            )
        } returns flowOf(nonEmptyListOf(conversation))

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.Invoices)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when messages are empty, the error is emitted`() = runTest {
        // Given
        val emptyMessageList = emptyList<Conversation>()
        val expected = DataError.Local.NoDataCached.left()
        every {
            conversationRepository.observeCachedConversations(
                UserIdSample.Primary,
                listOf(ConversationIdSample.WeatherForecast)
            )
        } returns flowOf(emptyMessageList)

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.WeatherForecast)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when message labels emits an error, the error is emitted`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns flowOf(error)

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.WeatherForecast)
        )

        // Then
        assertEquals(error, actual)

    }

    @Test
    fun `when message folder emits an error, the error is emitted`() = runTest {
        // Given
        val error = DataError.Local.NoDataCached.left()
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns flowOf(error)

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.WeatherForecast)
        )

        // Then
        assertEquals(error, actual)
    }
}
