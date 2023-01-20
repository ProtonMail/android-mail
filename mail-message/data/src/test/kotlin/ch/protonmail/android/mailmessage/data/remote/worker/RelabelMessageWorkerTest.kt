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
import ch.protonmail.android.mailmessage.data.remote.resource.PostRelabelBody
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
import kotlin.test.assertFailsWith

internal class RelabelMessageWorkerTest {

    private val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
    private val labelList = listOf(LabelId("10"), LabelId("11"), LabelId("12"))

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }

    private val parameters: WorkerParameters = mockk {
        every { getTaskExecutor() } returns mockk(relaxed = true)
        every { inputData.getString(RelabelMessageWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(RelabelMessageWorker.RawMessageIdKey) } returns messageId.id
        every { inputData.getStringArray(RelabelMessageWorker.RawLabelListKey) } returns labelList.map { it.id }
            .toTypedArray()
    }

    private val context: Context = mockk()
    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }
    private val messageApi = mockk<MessageApi> {
        coEvery { relabel(any(), any()) } returns mockk()
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), MessageApi::class) } returns TestApiManager(messageApi)
    }
    private val messageLocalDataSource = mockk<MessageLocalDataSource> {
        coEvery { relabel(userId, messageId, labelList) } returns MessageTestData.message.right()
    }
    private lateinit var apiProvider: ApiProvider
    private lateinit var relabelMessageWorker: RelabelMessageWorker

    @Before
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        relabelMessageWorker = RelabelMessageWorker(
            context = context,
            workerParameters = parameters,
            apiProvider = apiProvider,
            messageLocalDataSource = messageLocalDataSource
        )
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        RelabelMessageWorker.Enqueuer(workManager).enqueue(
            userId = userId,
            message = messageId,
            labelList = labelList
        )

        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workspecs = requestSlot.captured.workSpec
        val constraints = workspecs.constraints
        val inputData = workspecs.input
        val actualUserId = inputData.getString(RelabelMessageWorker.RawUserIdKey)
        val actualMessageId = inputData.getString(RelabelMessageWorker.RawMessageIdKey)
        val actualLabelList = inputData.getStringArray(RelabelMessageWorker.RawLabelListKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageId)
        assertEquals(labelList.map { it.id }, actualLabelList?.toList())
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `when worker is started then api is called with the given parameters`() = runTest {
        // When
        relabelMessageWorker.doWork()

        // Then
        coVerify { messageApi.relabel(messageId.id, PostRelabelBody(labelList.map { it.id })) }
    }

    @Test
    fun `worker fails when userid worker parameter is missing`() = runTest {
        // Given
        every { parameters.inputData.getString(RelabelMessageWorker.RawUserIdKey) } returns null

        // When
        assertFailsWith<IllegalArgumentException> { relabelMessageWorker.doWork() }

        // Then
        coVerify { messageApi wasNot Called }
    }

    @Test
    fun `worker fails when messageId worker parameter is empty`() = runTest {
        // Given
        every { parameters.inputData.getString(RelabelMessageWorker.RawMessageIdKey) } returns ""

        // When
        assertFailsWith<IllegalArgumentException> { relabelMessageWorker.doWork() }

        // Then
        coVerify { messageApi wasNot Called }
    }

    @Test
    fun `worker fails when labelList worker parameter is empty`() = runTest {
        // Given
        every { parameters.inputData.getStringArray(RelabelMessageWorker.RawLabelListKey) } returns emptyArray()

        // When
        val result = relabelMessageWorker.doWork()

        // Then
        assertEquals(Result.failure(), result)
        coVerify { messageApi wasNot Called }
    }

    @Test
    fun `worker return success when api call succeeds`() = runTest {
        // When
        val result = relabelMessageWorker.doWork()

        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `worker returns retry when api call fails to do connection error`() = runTest {
        // Given
        coEvery { messageApi.relabel(any(), any()) } throws UnknownHostException()

        // When
        val result = relabelMessageWorker.doWork()

        // Then
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `worker returns failure when api call fails to do non-retryable error`() = runTest {
        // Given
        coEvery { messageApi.relabel(any(), any()) } throws SerializationException()

        // When
        val result = relabelMessageWorker.doWork()

        // Then
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `worker rolls back changes when api call fails with non-retryable error`() = runTest {
        // Given
        coEvery { messageApi.relabel(any(), any()) } throws SerializationException()

        // When
        relabelMessageWorker.doWork()

        // Then
        coVerify { messageLocalDataSource.relabel(userId, messageId, labelList) }
    }

}
