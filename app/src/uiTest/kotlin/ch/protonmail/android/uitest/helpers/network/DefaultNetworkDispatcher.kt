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

@file:SuppressWarnings("MagicNumber", "MaxLineLength", "LongParameterList")

package ch.protonmail.android.uitest.helpers.network

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode

/**
 * A base top level function that provides a [MockNetworkDispatcher] instance
 * with default values that can be easily overridden.
 */
internal fun mockNetworkDispatcher(
    useDefaultAuth: Boolean = true,
    useDefaultCoreSettings: Boolean = true,
    useDefaultMailSettings: Boolean = true,
    useDefaultContacts: Boolean = true,
    useDefaultFeatures: Boolean = true,
    useDefaultLabels: Boolean = true,
    ignoreEvents: Boolean = true,
    func: MockNetworkDispatcher.() -> Unit = {}
) = MockNetworkDispatcher().apply {
    if (useDefaultAuth) {
        addMockRequests(
            "/auth/v4" respondWith "/auth/v4/auth-v4_base_placeholder.json" withStatusCode 200,
            "/auth/v4/info" respondWith "/auth/v4/info/info_base_placeholder.json" withStatusCode 200,
            "/auth/v4/sessions" respondWith "/auth/v4/sessions/sessions_base_placeholder.json" withStatusCode 200,
            "/core/v4/users" respondWith "/core/v4/users/users_base_placeholder.json" withStatusCode 200,
            "/core/v4/addresses" respondWith "/core/v4/addresses/addresses_base_placeholder.json" withStatusCode 200,
            "/core/v4/keys/salts" respondWith "/core/v4/keys/salts/salts_base_placeholder.json" withStatusCode 200,
            "/auth/v4/scopes" respondWith "/auth/v4/scopes/scopes_base_placeholder.json" withStatusCode 200
        )
    }

    if (useDefaultCoreSettings) {
        addMockRequests(
            "/core/v4/settings" respondWith "/core/v4/settings/core-v4-settings_base_placeholder.json" withStatusCode 200
        )
    }

    if (useDefaultMailSettings) {
        addMockRequests(
            "/mail/v4/settings" respondWith "/mail/v4/settings/mail-v4-settings_base_placeholder.json" withStatusCode 200
        )
    }

    if (useDefaultContacts) {
        addMockRequests(
            "/contacts/v4/contacts" respondWith "/contacts/v4/contacts/contacts_base_placeholder.json" withStatusCode 200 ignoreQueryParams true,
            "/contacts/v4/contacts/emails" respondWith "/contacts/v4/contacts/emails/contacts-emails_base_placeholder.json" withStatusCode 200 ignoreQueryParams true
        )
    }

    if (useDefaultFeatures) {
        addMockRequests(
            "/core/v4/features" respondWith "/core/v4/features/features_empty_placeholder.json" withStatusCode 200 ignoreQueryParams true
        )
    }

    if (useDefaultLabels) {
        addMockRequests(
            "/core/v4/labels?Type=1" respondWith "/core/v4/labels/labels-type1_base_placeholder.json" withStatusCode 200,
            "/core/v4/labels?Type=3" respondWith "/core/v4/labels/labels-type3_base_placeholder.json" withStatusCode 200
        )
    }

    if (ignoreEvents) {
        addMockRequests(
            "/core/v4/events/latest/" respondWith "/core/v4/events/latest/events-latest_base_placeholder.json" withStatusCode 200
        )
    }

    func()
}
