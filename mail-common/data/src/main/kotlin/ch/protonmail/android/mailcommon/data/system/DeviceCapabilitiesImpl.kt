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

package ch.protonmail.android.mailcommon.data.system

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DeviceCapabilitiesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceCapabilities {

    override fun getCapabilities(): DeviceCapabilities.Capabilities = DeviceCapabilities.Capabilities(
        hasWebView = hasWebView()
    )

    private fun hasWebView(): Boolean = hasWebViewPackageEnabled() || hasWebViewGooglePackageEnabled()

    private fun hasWebViewPackageEnabled(): Boolean =
        context.packageManager.getPackageInfoCompat(WEB_VIEW_PACKAGE)?.applicationInfo?.enabled ?: false

    private fun hasWebViewGooglePackageEnabled(): Boolean =
        context.packageManager.getPackageInfoCompat(WEB_VIEW_GOOGLE_PACKAGE)?.applicationInfo?.enabled ?: false

    internal companion object {

        const val WEB_VIEW_PACKAGE = "com.android.webview"
        const val WEB_VIEW_GOOGLE_PACKAGE = "com.google.android.webview"

        @Suppress("SwallowedException")
        fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo? =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
                }
            } catch (e: NameNotFoundException) {
                null
            }
    }
}
