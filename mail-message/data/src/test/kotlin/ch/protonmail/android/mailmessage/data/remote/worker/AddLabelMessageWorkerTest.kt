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

package ch.protonmail.android.mailmessage.data.remote.worker

import java.net.UnknownHostException
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.right
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.resource.PutLabelBody
import ch.protonmail.android.mailmessage.data.sample.PutLabelResponseSample
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class AddLabelMessageWorkerTest {

    private val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
    private val labelId = LabelId("10")

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { getTaskExecutor() } returns mockk(relaxed = true)
        every { inputData.getString(AddLabelMessageWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(AddLabelMessageWorker.RawMessageIdKey) } returns messageId.id
        every { inputData.getString(AddLabelMessageWorker.RawLabelIdKey) } returns labelId.id
    }
    private val context: Context = mockk()

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }
    private val messageApi = mockk<MessageApi> {
        coEvery { addLabel(any()) } returns PutLabelResponseSample.putLabelResponseForOneMessage
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), MessageApi::class) } returns TestApiManager(messageApi)
    }
    private val messageLocalDataSource = mockk<MessageLocalDataSource> {
        coEvery { removeLabel(userId, messageId, labelId) } returns MessageTestData.message.right()
    }

    private lateinit var apiProvider: ApiProvider
    private lateinit var addLabelMessageWorker: AddLabelMessageWorker

    @Before
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        addLabelMessageWorker = AddLabelMessageWorker(
            context,
            parameters,
            apiProvider,
            messageLocalDataSource
        )
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        AddLabelMessageWorker.Enqueuer(workManager).enqueue(
            userId,
            messageId,
            labelId
        )
        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(AddLabelMessageWorker.RawUserIdKey)
        val actualMessageId = inputData.getString(AddLabelMessageWorker.RawMessageIdKey)
        val actualLabelId = inputData.getString(AddLabelMessageWorker.RawLabelIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageId)
        assertEquals(labelId.id, actualLabelId)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `when worker is started then api is called with the given parameters`() = runTest {
        // When
        addLabelMessageWorker.doWork()
        // Then
        coVerify { messageApi.addLabel(PutLabelBody(labelId.id, listOf(messageId.id))) }
    }

    @Test
    fun `worker returns failure when userid worker parameter is missing`() = runTest {
        // Given
        every { parameters.inputData.getString(AddLabelMessageWorker.RawUserIdKey) } returns null
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        coVerify { messageApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns failure when messageId worker parameter is empty`() = runTest {
        // Given
        every { parameters.inputData.getString(AddLabelMessageWorker.RawMessageIdKey) } returns ""
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        coVerify { messageApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns failure when labelId worker parameter is blank`() = runTest {
        // Given
        every { parameters.inputData.getString(AddLabelMessageWorker.RawLabelIdKey) } returns " "
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        coVerify { messageApi wasNot Called }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker returns success when api call was successful`() = runTest {
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `worker returns retry when api call fails due to connection error`() = runTest {
        // Given
        coEvery { messageApi.addLabel(any()) } throws UnknownHostException()
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `worker returns failure when api call fails due to serializationException error`() = runTest {
        // Given
        coEvery { messageApi.addLabel(any()) } throws SerializationException()
        // When
        val result = addLabelMessageWorker.doWork()
        // Then
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `roll back changes to message when api call fails with a non-retryable error`() = runTest {
        // Given
        coEvery { messageApi.addLabel(any()) } throws SerializationException()
        // When
        addLabelMessageWorker.doWork()
        // Then
        coVerify { messageLocalDataSource.removeLabel(userId, messageId, labelId) }
    }
}
