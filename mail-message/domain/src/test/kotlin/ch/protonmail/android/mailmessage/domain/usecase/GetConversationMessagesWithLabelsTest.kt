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
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetConversationMessagesWithLabelsTest {

    private val messageLabelsFlow = flowOf(DataResult.Success(ResponseSource.Local, listOf(LabelSample.Archive)))
    private val messageFoldersFlow = flowOf(DataResult.Success(ResponseSource.Local, listOf(LabelSample.Document)))
    private val labelRepository: LabelRepository = mockk {
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns messageLabelsFlow
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns messageFoldersFlow
    }
    private val messageRepository: MessageRepository = mockk {
        every {
            observeCachedMessagesForConversations(UserIdSample.Primary, listOf(ConversationIdSample.WeatherForecast))
        } returns flowOf(nonEmptyListOf(MessageSample.AugWeatherForecast))
    }
    private val getConversationMessagesWithLabels = GetConversationMessagesWithLabels(
        labelRepository = labelRepository,
        messageRepository = messageRepository
    )

    @BeforeTest
    fun setUp() {
        mockkStatic("ch.protonmail.android.mailcommon.domain.mapper.DataResultEitherMappingsKt")
        every { messageLabelsFlow.mapToEither() } returns flowOf(emptyList<Label>().right())
        every { messageFoldersFlow.mapToEither() } returns flowOf(emptyList<Label>().right())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when messages and labels are emitted, right model is emitted`() = runTest {
        // Given
        val expected = nonEmptyListOf(MessageWithLabelsSample.AugWeatherForecast).right()

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
        val message = MessageSample.Invoice.copy(
            labelIds = listOf(LabelIdSample.Archive, LabelIdSample.Document)
        )
        val messageWithLabels = MessageWithLabelsSample.build(
            message = message,
            labels = listOf(LabelSample.Archive, LabelSample.Document).sortedBy { it.order }
        )
        val allLabels = listOf(LabelSample.Document, LabelSample.News)
        val allFolders = listOf(LabelSample.Archive, LabelSample.Inbox)
        val expected = nonEmptyListOf(messageWithLabels).right()

        every { messageLabelsFlow.mapToEither() } returns flowOf(allLabels.right())
        every { messageFoldersFlow.mapToEither() } returns flowOf(allFolders.right())
        every {
            messageRepository.observeCachedMessagesForConversations(
                UserIdSample.Primary,
                listOf(ConversationIdSample.Invoices)
            )
        } returns
            flowOf(nonEmptyListOf(message))

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
        val emptyMessageList = emptyList<Message>()
        val expected = DataError.Local.NoDataCached.left()
        every {
            messageRepository.observeCachedMessagesForConversations(
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
        every { messageLabelsFlow.mapToEither() } returns flowOf(error)

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
        every { messageFoldersFlow.mapToEither() } returns flowOf(error)

        // When
        val actual = getConversationMessagesWithLabels(
            UserIdSample.Primary,
            listOf(ConversationIdSample.WeatherForecast)
        )

        // Then
        assertEquals(error, actual)
    }
}
