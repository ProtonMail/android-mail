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

import java.io.IOException
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.mapper.MessageWithLabelsMapper
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessageWithLabelsTest {

    private val userId = UserId("userId")
    private val messageId = MessageId("messageId")
    private val message = MessageTestData.buildMessage(id = messageId.id)
    private val labels = listOf(LabelTestData.buildLabel(id = "customLabel"))
    private val folders = listOf(LabelTestData.buildLabel(id = "customFolder"))
    private val labelsAndFolders = labels + folders
    private val localErrorFlow = flowOf(DataResult.Error.Local("error message", IOException()))

    private val observeMessage: ObserveMessage = mockk {
        every { this@mockk(userId, messageId) } returns flowOf(message.right())
    }
    private val labelRepository: LabelRepository = mockk {
        every { observeLabels(userId, LabelType.MessageLabel) } returns flowOf(
            DataResult.Success(
                ResponseSource.Remote,
                labels
            )
        )
        every { observeLabels(userId, LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(
                ResponseSource.Remote,
                folders
            )
        )
    }
    private val messageWithLabelsMapper: MessageWithLabelsMapper = mockk {
        every { toUiModel(message, labelsAndFolders) } returns MessageWithLabels(message, labelsAndFolders)
    }

    private val observeMessageWithLabels =
        ObserveMessageWithLabels(observeMessage, labelRepository, messageWithLabelsMapper)

    @BeforeTest
    fun setUp() {
        mockkStatic("ch.protonmail.android.mailcommon.domain.mapper.DataResultEitherMappingsKt")
        every { localErrorFlow.mapToEither() } returns flowOf(DataError.Local.NoDataCached.left())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when all calls are successful, return a message with labels`() = runTest {
        // Given
        val expectedResult = MessageWithLabels(message, labelsAndFolders).right()
        // When
        observeMessageWithLabels.invoke(userId, messageId).test {
            // Then
            assertEquals(expectedResult, awaitItem())
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
        every { labelRepository.observeLabels(userId, LabelType.MessageLabel) } returns localErrorFlow
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
        every { labelRepository.observeLabels(userId, LabelType.MessageFolder) } returns localErrorFlow
        val expectedResult = DataError.Local.NoDataCached.left()
        // When
        observeMessageWithLabels.invoke(userId, messageId).test {
            // Then
            assertEquals(expectedResult, awaitItem())
            awaitComplete()
        }
    }
}
