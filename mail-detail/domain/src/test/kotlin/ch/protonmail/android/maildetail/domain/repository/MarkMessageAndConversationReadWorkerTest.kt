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

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MarkMessageAndConversationReadWorkerTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.AugWeatherForecast
    private val conversationId = ConversationIdSample.WeatherForecast

    // Mock dependencies
    private val markMessageAndConversationRead: MarkMessageAndConversationReadIfAllMessagesRead =
        mockk(relaxed = true)

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(MarkMessageAndConversationReadWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(MarkMessageAndConversationReadWorker.RawMessageIdKey) } returns messageId.id
        every {
            inputData.getString(MarkMessageAndConversationReadWorker.RawConversationIdKey)
        } returns conversationId.id
    }

    private lateinit var worker: MarkMessageAndConversationReadWorker
    private val context: Context = mockk()

    @Before
    fun setUp() {

        worker = MarkMessageAndConversationReadWorker(
            context,
            parameters,
            markMessageAndConversationRead
        )
    }

    @Test
    fun `should enqueue the worker with correct parameters`() = runBlockingTest {

        // When
        MarkMessageAndConversationReadWorker.Enqueuer(workManager)
            .enqueue(userId, MarkMessageAndConversationReadWorker.params(userId, messageId, conversationId))

        // Then
        val slot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(slot)) }
        val workSpec = slot.captured.workSpec
        val inputData = workSpec.input
        val actualUserId = inputData.getString(MarkMessageAndConversationReadWorker.RawUserIdKey)
        val actualMessageId = inputData.getString(MarkMessageAndConversationReadWorker.RawMessageIdKey)
        val actualConversationId = inputData.getString(MarkMessageAndConversationReadWorker.RawConversationIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageId)
        assertEquals(conversationId.id, actualConversationId)
    }

    @Test
    fun `worker should return success if marking was successful`() = runTest {
        // Given
        coEvery { markMessageAndConversationRead.invoke(userId, messageId, conversationId) } returns Unit.right()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify {
            markMessageAndConversationRead.invoke(userId, messageId, conversationId)
        }
    }

    @Test
    fun `worker should return failure if marking was failed`() = runTest {
        // Given
        val error = MarkConversationReadError.ConversationHasUnreadMessages
        coEvery { markMessageAndConversationRead.invoke(userId, messageId, conversationId) } returns error.left()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify {
            markMessageAndConversationRead.invoke(userId, messageId, conversationId)
        }
    }
}
