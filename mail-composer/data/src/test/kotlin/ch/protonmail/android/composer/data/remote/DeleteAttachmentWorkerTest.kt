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

package ch.protonmail.android.composer.data.remote

import java.net.UnknownHostException
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeleteAttachmentWorkerTest {

    private val userId = UserIdSample.Primary
    private val attachmentId = AttachmentId("attachment_id")

    private val workerManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }

    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(DeleteAttachmentWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(DeleteAttachmentWorker.RawAttachmentIdKey) } returns attachmentId.id
    }
    private val context: Context = mockk()

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }

    private val attachmentApi = mockk<AttachmentApi> {
        coEvery { deleteAttachment(attachmentId.id) } returns "ok".toResponseBody()
    }
    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), AttachmentApi::class) } returns TestApiManager(attachmentApi)
    }

    private lateinit var apiProvider: ApiProvider
    private lateinit var worker: DeleteAttachmentWorker

    @BeforeTest
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        worker = DeleteAttachmentWorker(context, parameters, apiProvider)
    }

    @Test
    fun `worker is enqueued with given parameters`() {
        // When
        Enqueuer(workerManager).enqueue<DeleteAttachmentWorker>(
            userId,
            DeleteAttachmentWorker.params(
                userId,
                attachmentId
            )
        )

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workerManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(DeleteAttachmentWorker.RawUserIdKey)
        val actualAttachmentId = inputData.getString(DeleteAttachmentWorker.RawAttachmentIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(attachmentId.id, actualAttachmentId)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `when delete attachment worker is executed then api is called with given parameters`() = runTest {
        // When
        worker.doWork()

        // Then
        coEvery { attachmentApi.deleteAttachment(attachmentId.id) }
    }

    @Test
    fun `delete attachment worker fails when userId parameter is missing`() = runTest {
        // Given
        every { parameters.inputData.getString(DeleteAttachmentWorker.RawUserIdKey) } returns null

        // When - Then
        assertFailsWith<IllegalArgumentException> { worker.doWork() }
        coVerify { attachmentApi wasNot Called }
    }

    @Test
    fun `delete attachment worker fails when attachmentId parameter is missing`() = runTest {
        // Given
        every { parameters.inputData.getString(DeleteAttachmentWorker.RawAttachmentIdKey) } returns null

        // When - Then
        assertFailsWith<IllegalArgumentException> { worker.doWork() }
        coVerify { attachmentApi wasNot Called }
    }

    @Test
    fun `delete attachment worker returns success when api call was successful`() = runTest {
        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `delete attachment worker returns retry when api call was not successful`() = runTest {
        // Given
        coEvery { attachmentApi.deleteAttachment(attachmentId.id) } throws UnknownHostException()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `delete attachment worker returns failure when api call was not successful`() = runTest {
        // Given
        coEvery { attachmentApi.deleteAttachment(attachmentId.id) } throws SerializationException()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(Result.failure(), result)
    }

}
