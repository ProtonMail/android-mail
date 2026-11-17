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

package ch.protonmail.android.mailbyoe.presentation.navigation

import java.net.URI
import java.net.URISyntaxException

/**
 * The links that open the Gmail connection from outside the app, e.g. from a web page or an email. They open the Gmail
 * connection settings on the web, like Settings does.
 *
 * There are two kinds: the app's own [LINK], and the web pages of the account settings that list the connected
 * addresses, which the app is verified to open instead of the browser.
 *
 * The `connect-gmail` host is shared with the redirect that completes the connection, `/complete`, which the
 * connection screen reads instead.
 */
object ConnectGmailDeepLink {

    const val LINK = "protonmail://connect-gmail/start"

    private const val WEB_SCHEME = "https"
    private const val WEB_HOST = "account.proton.me"

    // The same pages the iOS app opens. The manifest lists them too: keep both in sync.
    private val WebPaths = listOf("/mail/easy-switch", "/mail/identity-addresses")

    /**
     * Only the exact [LINK]: a different case, path, query or fragment is not it. A web page matches with anything
     * below it and with any query or fragment, since links to it carry campaign parameters.
     */
    fun matches(link: String?): Boolean = link == LINK || isWebPage(link)

    /**
     * Whether the link is one of the web pages. Unlike [LINK], such a link also works in a browser, so one the app
     * does not act on is passed on to it.
     */
    fun isWebPage(link: String?): Boolean {
        val uri = try {
            URI(link ?: return false)
        } catch (_: URISyntaxException) {
            return false
        }
        val isAccountSite = uri.scheme == WEB_SCHEME && uri.host == WEB_HOST && uri.userInfo == null && uri.port == -1
        val path = uri.rawPath.orEmpty()
        return isAccountSite && WebPaths.any { page -> path == page || path.startsWith("$page/") }
    }
}
