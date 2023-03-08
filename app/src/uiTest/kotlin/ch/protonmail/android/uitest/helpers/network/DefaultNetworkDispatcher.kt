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
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode

/**
 * Base [MockNetworkDispatcher] to handle the existing scenarios without copy-pasting the same
 * base dispatcher over and over. Once more tests are in place, this can be further generalized.
 */
@SuppressWarnings("MagicNumber")
fun defaultNetworkDispatcher() = MockNetworkDispatcher().apply {
    addMockRequests(
        "/auth/v4" respondWith "/auth/v4/auth-v4_base_placeholder.json" withStatusCode 200,
        "/core/v4/addresses" respondWith "/core/v4/addresses/addresses_base_placeholder.json" withStatusCode 200,
        "/core/v4/labels?Type=1" respondWith "/core/v4/labels/labels-type1_base_placeholder.json" withStatusCode 200,
        "/core/v4/labels?Type=3" respondWith "/core/v4/labels/labels-type3_base_placeholder.json" withStatusCode 200,
        "/core/v4/settings" respondWith "/core/v4/settings/core-v4-settings_base_placeholder.json" withStatusCode 200,
        "/mail/v4/settings" respondWith "/mail/v4/settings/mail-v4-settings_base_placeholder.json" withStatusCode 200
    )
}
