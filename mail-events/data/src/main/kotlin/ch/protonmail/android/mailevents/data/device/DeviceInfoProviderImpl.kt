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

package ch.protonmail.android.mailevents.data.device

import android.os.Build
import ch.protonmail.android.mailcommon.data.mapper.LocalDeviceInfoProvider
import ch.protonmail.android.mailevents.domain.model.DeviceInfo
import ch.protonmail.android.mailevents.domain.repository.DeviceInfoProvider
import javax.inject.Inject

class DeviceInfoProviderImpl @Inject constructor(
    private val localProvider: LocalDeviceInfoProvider
) : DeviceInfoProvider {

    override suspend fun getDeviceInfo(): DeviceInfo {
        val uniffiDeviceInfo = localProvider.getDeviceInfo()

        return DeviceInfo(
            platform = DeviceInfo.PLATFORM_ANDROID,
            osVersion = Build.VERSION.SDK_INT.toString(),
            locale = uniffiDeviceInfo.country,
            languageCode = uniffiDeviceInfo.language,
            make = uniffiDeviceInfo.brand,
            model = uniffiDeviceInfo.model
        )
    }
}
