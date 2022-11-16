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
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
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

internal class ObserverConversationMessagesWithLabelsTest {

    private val messageLabelsFlow = flowOf(DataResult.Success(ResponseSource.Local, listOf(LabelSample.Archive)))
    private val messageFoldersFlow = flowOf(DataResult.Success(ResponseSource.Local, listOf(LabelSample.Document)))
    private val labelRepository: LabelRepository = mockk {
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageLabel) } returns messageLabelsFlow
        every { observeLabels(userId = UserIdSample.Primary, LabelType.MessageFolder) } returns messageFoldersFlow
    }
    private val messageRepository: MessageRepository = mockk {
        every { observeCachedMessages(UserIdSample.Primary, ConversationIdSample.WeatherForecast) } returns
            flowOf(listOf(MessageSample.AugWeatherForecast))
    }
    private val observerConversationMessagesWithLabels = ObserverConversationMessagesWithLabels(
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
        // given
        val expected = nonEmptyListOf(MessageWithLabelsSample.AugWeatherForecast).right()

        // when
        observerConversationMessagesWithLabels(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when messages emits an error, the error is emitted`() = runTest {
        // given
        val expected = DataError.Local.NoDataCached.left()
        every {
            messageRepository.observeCachedMessages(
                UserIdSample.Primary,
                ConversationIdSample.WeatherForecast
            )
        } returns flowOf(emptyList())

        // when
        observerConversationMessagesWithLabels(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when message labels emits an error, the error is emitted`() = runTest {
        // given
        val expected = DataError.Local.NoDataCached.left()
        every { messageLabelsFlow.mapToEither() } returns flowOf(expected)

        // when
        observerConversationMessagesWithLabels(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when message folder emits an error, the error is emitted`() = runTest {
        // given
        val expected = DataError.Local.NoDataCached.left()
        every { messageFoldersFlow.mapToEither() } returns flowOf(expected)

        // when
        observerConversationMessagesWithLabels(
            UserIdSample.Primary,
            ConversationIdSample.WeatherForecast
        ).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }
}
