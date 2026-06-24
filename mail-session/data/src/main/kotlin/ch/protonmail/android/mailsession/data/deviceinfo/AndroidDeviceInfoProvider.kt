/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsession.data.deviceinfo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.util.android.device.appLanguage
import me.proton.core.util.android.device.deviceFontSize
import me.proton.core.util.android.device.deviceInputMethods
import me.proton.core.util.android.device.deviceRegion
import me.proton.core.util.android.device.deviceStorage
import me.proton.core.util.android.device.deviceTimezone
import me.proton.core.util.android.device.deviceTimezoneOffset
import me.proton.core.util.android.device.isDeviceRooted
import me.proton.core.util.android.device.nightMode
import uniffi.mail_uniffi.DeviceInfo
import uniffi.mail_uniffi.DeviceInfoProvider
import javax.inject.Inject

@SuppressLint("HardwareIds")
class AndroidDeviceInfoProvider @Inject constructor(
    @ApplicationContext val context: Context
) : DeviceInfoProvider {

    private val appId: String by lazy {
        runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }.getOrDefault("")
    }

    override suspend fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        // The language code of this Locale.
        language = appLanguage(),
        // Time zone id, such as "Asia/Calcutta", "GMT+5:30" or "PST".
        timezone = deviceTimezone(),
        // Time zone raw offset in minutes from GMT including daylight saving.
        timezoneOffset = deviceTimezoneOffset(),
        // The end-user-visible name for the end product.
        model = Build.MODEL,
        // The consumer-visible brand with which the product/hardware will be associated.
        brand = Build.BRAND,
        // The name of the industrial design.
        codename = Build.DEVICE,
        // The app's UUID given by the OS.
        uuid = appId,
        // The country/region code, in ISO 3166 2-letter code, or a UN M.49 3-digit code.
        country = context.deviceRegion(),
        // If device/OS is rooted/jailbroken.
        rooted = isDeviceRooted(context),
        // The current scaling factor for fonts, relative to the base density scaling.
        fontScale = context.deviceFontSize().toString(),
        // The total size of the device storage in GB.
        storage = context.deviceStorage(),
        // If the device (or current context) is using dark mode.
        darkMode = context.nightMode(),
        // List of enabled input methods application name (e.g. packageName, bundle id).
        keyboards = context.deviceInputMethods()
    )
}
