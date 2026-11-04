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

package ch.protonmail.android.mailbyoe.domain.model

/**
 * Outcome of the Gmail connection web flow, as reported back to the app.
 */
sealed interface GmailConnectionResult {

    /** [email] is the connected Gmail address when the web flow reports it, null otherwise. */
    data class Connected(val email: String? = null) : GmailConnectionResult

    /** The user closed the web flow. The connection may still have completed before that. */
    data object Cancelled : GmailConnectionResult

    /** The user is on a free plan at the connection limit and tapped "Upgrade to connect". */
    data object UpsellRequested : GmailConnectionResult

    data class Failed(val code: String?) : GmailConnectionResult
}
