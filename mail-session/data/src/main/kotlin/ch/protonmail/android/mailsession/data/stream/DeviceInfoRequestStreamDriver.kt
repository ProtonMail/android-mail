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
import uniffi.mail_uniffi.DeviceInfoRequest
import uniffi.mail_uniffi.DeviceInfoRequestStream
import uniffi.mail_uniffi.DeviceInfoRequestStreamNextAsyncResult

/**
 * Answers Rust's device-info requests with the current device snapshot from
 * [LocalDeviceInfoProvider].
 */
internal class DeviceInfoRequestStreamDriver(
    private val stream: DeviceInfoRequestStream,
    private val deviceInfoProvider: LocalDeviceInfoProvider
) : RustRequestStreamDriver<DeviceInfoRequest>(name = "device-info") {

    override suspend fun awaitRequest(): Poll<DeviceInfoRequest> = when (val result = stream.nextAsync()) {
        is DeviceInfoRequestStreamNextAsyncResult.Ok -> Poll.Request(result.v1)
        is DeviceInfoRequestStreamNextAsyncResult.Error -> Poll.Closed(result.v1.toString())
    }

    override suspend fun answer(request: DeviceInfoRequest) {
        request.respond(deviceInfoProvider.getDeviceInfo())
    }
}
