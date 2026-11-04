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

package ch.protonmail.android.mailbyoe.presentation.connect

import ch.protonmail.android.mailcommon.presentation.Effect

sealed interface GmailConnectionState {

    data object Loading : GmailConnectionState

    /** The connection page is opened in the browser; [openPage] carries its URL. */
    data class OpeningPage(val openPage: Effect<String>) : GmailConnectionState

    data object Failed : GmailConnectionState

    /** The Gmail account is connected; [email] is its address when the web page reported it. */
    data class Connected(val email: String?, val openInbox: Effect<Unit>) : GmailConnectionState

    data class Finished(val close: Effect<Unit>) : GmailConnectionState
}
