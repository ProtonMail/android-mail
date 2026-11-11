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

/**
 * The link that opens the Gmail connection from outside the app, e.g. from a web page or an email. It opens the Gmail
 * connection settings on the web, like Settings does.
 *
 * The `connect-gmail` host is shared with the redirect that completes the connection, `/complete`, which the
 * connection screen reads instead.
 */
object ConnectGmailDeepLink {

    const val LINK = "protonmail://connect-gmail/start"

    /** Only the exact link: a different case, path, query or fragment is not it. */
    fun matches(link: String?): Boolean = link == LINK
}
