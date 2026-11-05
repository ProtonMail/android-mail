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

/** Why connecting a Gmail account failed. Mirrors the SDK's `ByoeConnectErrorType`. */
enum class ByoeConnectErrorType(private val webCode: String?) {
    NoAccess("no-access"),
    AlreadyAdded("already-added"),
    CreateFailed("create-failed"),
    ConvertFailed("convert-failed"),
    WrongAccount("wrong-account"),
    TokenFailed("token-failed"),
    Unknown(webCode = null);

    companion object {

        /**
         * Maps the `code` the web flow sends with `status=error`. The web values are not documented yet, so the code
         * is matched against the telemetry values, ignoring case and `_` versus `-`; anything else is [Unknown].
         */
        fun fromWebCode(code: String?): ByoeConnectErrorType {
            val normalized = code?.lowercase()?.replace('_', '-') ?: return Unknown
            return entries.firstOrNull { it.webCode == normalized } ?: Unknown
        }
    }
}
