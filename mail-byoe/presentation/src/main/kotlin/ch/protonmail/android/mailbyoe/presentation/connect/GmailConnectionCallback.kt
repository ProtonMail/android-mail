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

import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult

/**
 * The link the web flow sends back when it is done, e.g. `protonmail://connect-gmail/complete?status=success`.
 * It comes from a web page, so anything unexpected is treated as a failure.
 */
internal object GmailConnectionCallback {

    const val SCHEME = "protonmail"
    const val STATUS_PARAM = "status"
    const val CODE_PARAM = "code"
    const val EMAIL_PARAM = "email"

    private const val HOST = "connect-gmail"
    private const val PATH = "/complete"

    /** Where the web flow sends the user back to; the page appends the status and the other params. */
    const val REDIRECT_URI = "$SCHEME://$HOST$PATH"
    private val validCode = Regex("^[A-Za-z0-9_-]{1,64}$")
    private const val MAX_EMAIL_LENGTH = 254
    private val validEmail = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun parse(
        host: String?,
        path: String?,
        status: String?,
        code: String?,
        email: String? = null
    ): GmailConnectionResult {
        if (host != HOST || path != PATH) {
            return GmailConnectionResult.Failed(code = null)
        }

        return when (status) {
            "success" -> GmailConnectionResult.Connected(
                email = email?.takeIf { it.length <= MAX_EMAIL_LENGTH && validEmail.matches(it) }
            )
            "cancel" -> GmailConnectionResult.Cancelled
            "upsell" -> GmailConnectionResult.UpsellRequested
            "error" -> GmailConnectionResult.Failed(code = code?.takeIf { validCode.matches(it) })
            else -> GmailConnectionResult.Failed(code = null)
        }
    }
}
