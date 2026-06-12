/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnotifications.data.local

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.repository.DeviceRegistrationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals

internal class RegisterDeviceTokenWorkerTest {

    private val context = mockk<Context>(relaxUnitFun = true)
    private val deviceRegistrationRepository = mockk<DeviceRegistrationRepository>()

    private val params: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
        every { inputData.getString(RegisterDeviceTokenWorker.KeyDeviceToken) } returns RawToken
    }

    private val worker = RegisterDeviceTokenWorker(
        context = context,
        workerParameters = params,
        deviceRegistrationRepository = deviceRegistrationRepository
    )

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `given a null token, when the worker runs, then it fails without registering`() = runTest {
        // Given
        every { params.inputData.getString(RegisterDeviceTokenWorker.KeyDeviceToken) } returns null

        // When
        val result = worker.doWork()

        // Then
        assertEquals(MissingInputData, result)
        coVerify(exactly = 0) { deviceRegistrationRepository.registerDeviceToken(any()) }
    }

    @Test
    fun `given registration succeeds, when the worker runs, then it returns success`() = runTest {
        // Given
        coEvery { deviceRegistrationRepository.registerDeviceToken(RawToken) } returns Unit.right()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { deviceRegistrationRepository.registerDeviceToken(RawToken) }
    }

    @Test
    fun `given registration fails, when the worker runs, then it schedules a retry`() = runTest {
        // Given
        coEvery { deviceRegistrationRepository.registerDeviceToken(RawToken) } returns DataError.Remote.Unknown.left()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { deviceRegistrationRepository.registerDeviceToken(RawToken) }
    }

    private companion object {

        const val RawToken = "token"

        val MissingInputData = ListenableWorker.Result.failure(
            workDataOf(RegisterDeviceTokenWorker.KeyDeviceTokenError to "Input data is missing")
        )
    }
}
