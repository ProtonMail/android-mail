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

package ch.protonmail.android.logging

import android.content.Context
import androidx.webkit.WebViewCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryWebViewInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun setWebViewTags() {
        val packageInfo = runCatching {
            WebViewCompat.getCurrentWebViewPackage(context)
        }.getOrNull()

        val packageName = packageInfo?.packageName ?: "unknown"
        val versionName = packageInfo?.versionName ?: "unknown"

        Sentry.setTag("webview.package", packageName)
        Sentry.setTag("webview.version", versionName)

        Timber.d("WebView package=$packageName, version=$versionName")
    }
}
