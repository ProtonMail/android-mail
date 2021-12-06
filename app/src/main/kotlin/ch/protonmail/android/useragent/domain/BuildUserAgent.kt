/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.useragent.domain

import ch.protonmail.android.useragent.data.GetAndroidVersion
import ch.protonmail.android.useragent.data.GetAppVersion
import ch.protonmail.android.useragent.data.GetDefaultLocale
import ch.protonmail.android.useragent.data.GetDeviceData
import javax.inject.Inject

class BuildUserAgent @Inject constructor(
    val getAppVersion: GetAppVersion,
    val getAndroidVersion: GetAndroidVersion,
    val getDeviceData: GetDeviceData,
    val getDefaultLocale: GetDefaultLocale
) {
    operator fun invoke(): String {
        val device = getDeviceData()

        val protonMailAppVersion = "ProtonMail/${getAppVersion()}"
        val deviceSpecs = "${device.model}; ${device.brand}; ${device.device};"
        val androidInfo = "Android ${getAndroidVersion()}; $deviceSpecs"
        val language = getDefaultLocale().language

        return "$protonMailAppVersion ($androidInfo $language)"
    }
}
