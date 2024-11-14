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
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.put
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode

/**
 * A base top level function that provides a [MockNetworkDispatcher] instance
 * with default values that can be easily overridden.
 */
internal fun mockNetworkDispatcher(
    useDefaultCoreSettings: Boolean = true,
    useDefaultMailSettings: Boolean = true,
    useDefaultContacts: Boolean = true,
    useDefaultFeatures: Boolean = true,
    useDefaultUnleashToggles: Boolean = true,
    useDefaultLabels: Boolean = true,
    useDefaultContactGroups: Boolean = true,
    useDefaultCustomFolders: Boolean = true,
    useDefaultSystemFolders: Boolean = true,
    useDefaultPaymentSettings: Boolean = true,
    useDefaultMailReadResponses: Boolean = true,
    useDefaultDeviceRegistration: Boolean = true,
    useDefaultCounters: Boolean = true,
    ignoreEvents: Boolean = true,
    additionalMockDefinitions: MockNetworkDispatcher.() -> Unit = {}
) = MockNetworkDispatcher().apply {

    if (useDefaultCoreSettings) {
        addMockRequests(
            get("/core/v4/settings")
                respondWith "/core/v4/settings/core-v4-settings_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultMailSettings) {
        addMockRequests(
            get("/mail/v4/settings")
                respondWith "/mail/v4/settings/mail-v4-settings_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultContacts) {
        addMockRequests(
            get("/contacts/v4/contacts")
                respondWith "/contacts/v4/contacts/contacts_base_placeholder.json"
                withStatusCode 200 ignoreQueryParams true,
            get("/contacts/v4/contacts/emails")
                respondWith "/contacts/v4/contacts/emails/contacts-emails_base_placeholder.json"
                withStatusCode 200 ignoreQueryParams true
        )
    }

    if (useDefaultFeatures) {
        addMockRequests(
            get("/core/v4/features")
                respondWith "/core/v4/features/features_empty_placeholder.json"
                withStatusCode 200 ignoreQueryParams true
        )
    }

    if (useDefaultUnleashToggles) {
        addMockRequests(
            get("/feature/v2/frontend")
                respondWith "/feature/v2/frontend/frontend_empty_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultLabels) {
        addMockRequests(
            get("/core/v4/labels?Type=1")
                respondWith "/core/v4/labels/labels-type1_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultContactGroups) {
        addMockRequests(
            get("/core/v4/labels?Type=2")
                respondWith "/core/v4/labels/labels-type2_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultCustomFolders) {
        addMockRequests(
            get("/core/v4/labels?Type=3")
                respondWith "/core/v4/labels/labels-type3_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultSystemFolders) {
        addMockRequests(
            get("/core/v4/labels?Type=4")
                respondWith "/core/v4/labels/labels-type4_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultPaymentSettings) {
        addMockRequests(
            get("/payments/v4/status/google")
                respondWith "/payments/v4/status/google/payments_empty.json"
                withStatusCode 200
        )
    }

    if (useDefaultDeviceRegistration) {
        addMockRequests(
            post("/core/v4/devices")
                respondWith "/core/v4/devices/devices_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultMailReadResponses) {
        addMockRequests(
            put("/mail/v4/messages/read")
                respondWith "/mail/v4/messages/read/read_base_placeholder.json"
                withStatusCode 200 withPriority MockPriority.Highest,
            put("/mail/v4/conversations/read")
                respondWith "/mail/v4/conversations/read/conversations_read_base_placeholder.json"
                withStatusCode 200 withPriority MockPriority.Highest
        )
    }

    if (ignoreEvents) {
        addMockRequests(
            get("/core/v5/events/*")
                respondWith "/core/v5/events/event-id/event-v5_base_placeholder.json"
                withStatusCode 200 matchWildcards true,
            get("/core/v5/events/latest")
                respondWith "/core/v5/events/latest/events-v5-latest_base_placeholder.json"
                withStatusCode 200,
            get("/core/v4/events/*")
                respondWith "/core/v4/events/event-id/event_base_placeholder.json"
                withStatusCode 200 matchWildcards true,
            get("/core/v4/events/latest")
                respondWith "/core/v4/events/latest/events-latest_base_placeholder.json"
                withStatusCode 200
        )
    }

    if (useDefaultCounters) {
        addMockRequests(
            get("/mail/v4/conversations/count")
                respondWith "/mail/v4/conversations/count/conversations-count_base_placeholder.json"
                withStatusCode 200,
            get("/mail/v4/messages/count")
                respondWith "/mail/v4/messages/count/messages-count_base_placeholder.json"
                withStatusCode 200
        )
    }

    additionalMockDefinitions()
}
