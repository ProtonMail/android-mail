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
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessageWithLabelsTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.AugWeatherForecast
    private val message = MessageSample.AugWeatherForecast.copy(
        labelIds = listOf(LabelIdSample.Folder2022, LabelIdSample.News)
    )
    private val labels = listOf(LabelSample.Document, LabelSample.News)
    private val folders = listOf(LabelSample.Folder2021, LabelSample.Folder2022)
    private val localErrorFlow: Flow<Either.Left<DataError>> = flowOf(Either.Left(DataError.Local.NoDataCached))

    private val observeMessage: ObserveMessage = mockk {
        every { this@mockk(userId, messageId) } returns flowOf(message.right())
    }
    private val observeLabels: ObserveLabels = mockk {
        every { this@mockk.invoke(userId, LabelType.MessageLabel) } returns flowOf(
            Either.Right(
                labels
            )
        )
        every { this@mockk.invoke(userId, LabelType.MessageFolder) } returns flowOf(
            Either.Right(
                folders
            )
        )
    }

    private val observeMessageWithLabels =
        ObserveMessageWithLabels(observeMessage, observeLabels)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when all calls are successful, return a message with its labels`() = runTest {
        // Given
        val messageLabels = listOf(
            LabelSample.Folder2022,
            LabelSample.News
        )
        val expected = MessageWithLabels(message, messageLabels).right()

        // When
        observeMessageWithLabels.invoke(userId, messageId).test {

            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when observing the message fails, return an error`() = runTest {
        // Given
        every { observeMessage(userId, messageId) } returns flowOf(DataError.Local.NoDataCached.left())
        val expectedResult = DataError.Local.NoDataCached.left()
        // When
        observeMessageWithLabels.invoke(userId, messageId).test {
            // Then
            assertEquals(expectedResult, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when observing the labels fails, return an error`() = runTest {
        // Given
        every { observeLabels(userId, LabelType.MessageLabel) } returns localErrorFlow
        val expectedResult = DataError.Local.NoDataCached.left()
        // When
        observeMessageWithLabels.invoke(userId, messageId).test {
            // Then
            assertEquals(expectedResult, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `when observing the folders fails, return an error`() = runTest {
        // Given
        every { observeLabels(userId, LabelType.MessageFolder) } returns localErrorFlow
        val expectedResult = DataError.Local.NoDataCached.left()
        // When
        observeMessageWithLabels.invoke(userId, messageId).test {
            // Then
            assertEquals(expectedResult, awaitItem())
            awaitComplete()
        }
    }
}
