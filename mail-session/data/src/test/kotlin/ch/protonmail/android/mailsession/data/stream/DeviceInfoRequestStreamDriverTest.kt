/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.stream

import ch.protonmail.android.mailcommon.data.mapper.LocalDeviceInfoProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.DeviceInfo
import uniffi.mail_uniffi.DeviceInfoRequest
import uniffi.mail_uniffi.DeviceInfoRequestStream
import uniffi.mail_uniffi.DeviceInfoRequestStreamNextAsyncResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test

class DeviceInfoRequestStreamDriverTest {

    private val deviceInfoProvider = mockk<LocalDeviceInfoProvider>()
    private val stream = mockk<DeviceInfoRequestStream>()

    @Test
    fun `responds with the provided device info then disposes the request`() = runTest {
        // Given
        val request = mockk<DeviceInfoRequest>(relaxed = true)
        val deviceInfo = mockk<DeviceInfo>()
        coEvery { deviceInfoProvider.getDeviceInfo() } returns deviceInfo
        coEvery { stream.nextAsync() } returnsMany listOf(
            DeviceInfoRequestStreamNextAsyncResult.Ok(request),
            DeviceInfoRequestStreamNextAsyncResult.Error(mockk<ProtonError>())
        )

        // When
        DeviceInfoRequestStreamDriver(stream, deviceInfoProvider).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { request.respond(deviceInfo) }
        verify(exactly = 1) { request.close() }
    }

    @Test
    fun `does not request device info when the stream is closed`() = runTest {
        // Given
        coEvery { stream.nextAsync() } returns
            DeviceInfoRequestStreamNextAsyncResult.Error(mockk<ProtonError>())

        // When
        DeviceInfoRequestStreamDriver(stream, deviceInfoProvider).loop(this)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { deviceInfoProvider.getDeviceInfo() }
    }
}
