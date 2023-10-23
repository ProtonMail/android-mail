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

package ch.protonmail.android.mailnotifications.data.remote

import java.net.UnknownHostException
import java.util.UUID
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailnotifications.data.remote.resource.device.RegisterDeviceRequest
import ch.protonmail.android.mailnotifications.data.remote.resource.device.RegisterDeviceResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RegisterDeviceWorkerTest {

    private val environment = 4 // Android for the API
    private val token = UUID.randomUUID().toString()
    private val userId = UserIdSample.Primary
    private val workManager: WorkManager = mockk {
        coEvery { enqueue(ofType<OneTimeWorkRequest>()) } returns mockk()
    }
    private val nonRetryableException = SerializationException()
    private val retryableException = UnknownHostException()
    private val deviceServiceApi: DeviceServiceApi = mockk()
    private val apiProvider: ApiProvider = mockk {
        coEvery { get<DeviceServiceApi>(userId).invoke<RegisterDeviceResponse>(block = any()) } coAnswers {
            val block = firstArg<suspend DeviceServiceApi.() -> RegisterDeviceResponse>()
            try {
                ApiResult.Success(block(deviceServiceApi))
            } catch (e: Exception) {
                when (e) {
                    nonRetryableException -> ApiResult.Error.Parse(e)
                    retryableException -> ApiResult.Error.Connection()
                    else -> throw e
                }
            }
        }
    }
    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(RegisterDeviceWorker.RawUserIdKey) } returns userId.id
        every { inputData.getString(RegisterDeviceWorker.TokenKey) } returns token
    }
    private val worker = RegisterDeviceWorker(
        context = mockk(),
        workerParameters = params,
        apiProvider = apiProvider
    )

    @Test
    fun `Should enqueue worker with the correct constraints`() {
        // given
        val expectedNetworkType = NetworkType.CONNECTED

        // when
        Enqueuer(workManager).enqueue<RegisterDeviceWorker>(userId, RegisterDeviceWorker.params(userId, token))

        // then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        assertEquals(expectedNetworkType, requestSlot.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `Should call the API with the correct parameters`() = runTest {
        // given
        coEvery { deviceServiceApi.registerDevice(any()) } returns mockk()

        // when
        worker.doWork()

        // then
        coVerify {
            deviceServiceApi.registerDevice(RegisterDeviceRequest(token, environment))
        }
    }

    @Test
    fun `Should return error when there is no token stored`() = runTest {
        // given
        every { params.inputData.getString(RegisterDeviceWorker.TokenKey) } returns ""
        coEvery { deviceServiceApi.registerDevice(any()) } returns mockk()

        // when
        val result = worker.doWork()

        // then
        assertTrue { result is Result.Failure }
    }

    @Test
    fun `Should return success when the API call is successful`() = runTest {
        // given
        coEvery { deviceServiceApi.registerDevice(any()) } returns mockk()

        // when
        val result = worker.doWork()

        // then
        assertEquals(Result.success(), result)
    }

    @Test
    fun `Should return retry when the API call is not successful and the error is retryable`() = runTest {
        // given
        coEvery { deviceServiceApi.registerDevice(any()) } throws retryableException

        // when
        val result = worker.doWork()

        // then
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `Should return error if the API fails with a non retryable error`() = runTest {
        // given
        coEvery { deviceServiceApi.registerDevice(any()) } throws nonRetryableException

        // when
        val result = worker.doWork()

        // then
        assertEquals(Result.failure(), result)
    }
}
