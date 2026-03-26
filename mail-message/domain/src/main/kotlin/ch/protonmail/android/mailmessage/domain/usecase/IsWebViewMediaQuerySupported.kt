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

package ch.protonmail.android.mailmessage.domain.usecase

import android.content.Context
import androidx.webkit.WebViewCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class IsWebViewMediaQuerySupported @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(): Boolean {
        val versionName = runCatching {
            WebViewCompat.getCurrentWebViewPackage(context)?.versionName
        }.getOrNull() ?: return true

        val majorVersion = extractMajorVersion(versionName) ?: return true

        return majorVersion >= MIN_SUPPORTED_VERSION
    }

    // Example: "120.0.6099.144" --> 120
    private fun extractMajorVersion(versionName: String): Int? = versionName.substringBefore(".").toIntOrNull()


    private companion object {

        const val MIN_SUPPORTED_VERSION = 100
    }
}
