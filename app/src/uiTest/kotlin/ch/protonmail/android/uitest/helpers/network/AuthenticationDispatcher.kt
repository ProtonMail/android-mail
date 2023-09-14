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

package ch.protonmail.android.uitest.helpers.network

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.uitest.helpers.login.LoginType

/**
 * Returns a [MockNetworkDispatcher] instance with valid authenticator mocks depending on the passed [LoginType].
 *
 * @param loginType the login type (logged in, out).
 *
 * @return an instance of [MockNetworkDispatcher] with predefined mock definitions.
 */
internal fun authenticationDispatcher(loginType: LoginType) = MockNetworkDispatcher().apply {
    val id = when (loginType) {
        is LoginType.LoggedIn -> loginType.id
        else -> return@apply
    }

    addMockRequests(
        post("/auth/v4") respondWith "/auth/v4/auth-v4_$id.json" withStatusCode 200,
        post("/auth/v4/info") respondWith "/auth/v4/info/info_$id.json" withStatusCode 200,
        post("/auth/v4/sessions") respondWith "/auth/v4/sessions/sessions_$id.json" withStatusCode 200,
        get("/core/v4/users") respondWith "/core/v4/users/users_$id.json" withStatusCode 200,
        get("/core/v4/addresses") respondWith "/core/v4/addresses/addresses_$id.json" withStatusCode 200,
        get("/core/v4/keys/salts") respondWith "/core/v4/keys/salts/salts_$id.json" withStatusCode 200,
        get("/auth/v4/scopes") respondWith "/auth/v4/scopes/scopes_$id.json" withStatusCode 200
    )
}
