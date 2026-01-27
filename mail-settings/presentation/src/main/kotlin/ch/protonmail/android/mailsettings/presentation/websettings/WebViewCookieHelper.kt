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

package ch.protonmail.android.mailsettings.presentation.websettings

import android.webkit.CookieManager

internal object WebViewCookieHelper {

    fun setSessionCookie(url: String, sessionId: String) {
        val cookieManager = CookieManager.getInstance()
        val cookie = buildString {
            append("Session-Id=$sessionId; ")
            append("Domain=proton.me; ")
            append("Path=/; ")
            append("HttpOnly; ")
            append("SameSite=None; ")
            append("Secure")
        }

        cookieManager.setAcceptCookie(true)
        cookieManager.setCookie(url, cookie)
        cookieManager.flush()
    }

    fun clearSessionCookie(url: String) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie(url, "Session-Id=; Domain=proton.me; Path=/; Secure; Max-Age=0")
        cookieManager.flush()
    }
}
