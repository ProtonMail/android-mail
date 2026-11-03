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

package ch.protonmail.android.debug.environment

/**
 * Turns what a tester types or pastes into an environment host: a proton.black environment, or production.
 *
 * "maury.proton.black" and "https://account.maury.proton.black/login" both give "maury.proton.black", and
 * "https://mail.proton.me/inbox" gives "proton.me". The tester types the full host: there is no list of environments
 * to complete a bare name from.
 */
internal object EnvironmentHost {

    private val VALID = Regex("""^(([a-z0-9-]+\.)?proton\.black|proton\.me)$""")
    private val SERVICE_LABELS = setOf("account", "mail", "mail-api", "calendar", "drive", "pass")

    sealed interface Input {

        /** Empty or the build's own environment: saving it clears the override. */
        data object Default : Input

        data class Valid(val host: String) : Input

        data object Invalid : Input
    }

    /** [defaultHost] is the build's own environment, such as proton.black for dev and proton.me for alpha. */
    fun parse(input: String, defaultHost: String): Input {
        val text = input.trim().lowercase()
        if (text.isEmpty()) return Input.Default

        val host = text.substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .withoutServiceLabel()

        return when {
            host == defaultHost -> Input.Default
            VALID.matches(host) -> Input.Valid(host)
            else -> Input.Invalid
        }
    }

    /** Sign-in page that leads into Proton Mail on the web, where most account set-up is quicker than in the app. */
    fun accountLoginUrl(host: String): String = "https://account.$host/login"

    /** "account.maury.proton.black" points at the same environment as "maury.proton.black". */
    private fun String.withoutServiceLabel(): String {
        val label = substringBefore('.')
        val rest = substringAfter('.')
        val isServiceHost = label in SERVICE_LABELS && VALID.matches(rest)
        return if (isServiceHost) rest else this
    }
}
