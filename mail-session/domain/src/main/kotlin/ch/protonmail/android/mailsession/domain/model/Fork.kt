/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailsession.domain.model

data class Fork(
    val selector: Selector,
    val sessionId: CookieSessionId
)

data class ForkWithKeyPayload(
    val selector: Selector,
    val sessionId: CookieSessionId,
    val payloadKey: String,
    val payloadVersion: Int,
    val payloadType: String
) {

    // The payload key unlocks the forked key password, so keep it out of logs and crash reports.
    override fun toString(): String = "ForkWithKeyPayload(selector=$selector, sessionId=$sessionId, " +
        "payloadKey=<redacted>, payloadVersion=$payloadVersion, payloadType=$payloadType)"
}

data class Selector(
    val value: String
)

data class CookieSessionId(
    val value: String
)
