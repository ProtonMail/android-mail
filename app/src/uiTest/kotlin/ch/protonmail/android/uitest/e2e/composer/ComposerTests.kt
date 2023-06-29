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

package ch.protonmail.android.uitest.e2e.composer

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.MockPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withPriority
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher

interface ComposerTests {

    fun composerMockNetworkDispatcher(
        composerEnabled: Boolean = true,
        useDefaultMessagesList: Boolean = true,
        mockDefinitions: MockNetworkDispatcher.() -> Unit = {}
    ) = mockNetworkDispatcher {

        if (composerEnabled) {
            addMockRequests(
                "/core/v4/features?Code=HideComposerAndroid&Type=boolean"
                    respondWith "/core/v4/features/composer/hide_composer_disabled.json"
                    withStatusCode 200 withPriority MockPriority.Highest
            )
        }

        if (useDefaultMessagesList) {
            addMockRequests(
                "/mail/v4/messages"
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }
    }
}
