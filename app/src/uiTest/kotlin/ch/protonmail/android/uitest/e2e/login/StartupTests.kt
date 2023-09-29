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

package ch.protonmail.android.uitest.e2e.login

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginType
import ch.protonmail.android.uitest.robot.helpers.deviceRobot
import ch.protonmail.android.uitest.robot.helpers.verify
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal open class StartupTests : MockedNetworkTest(loginType = LoginType.LoggedOut) {

    @Test
    @TestId("224907")
    fun testBackButtonNavigationOnMainScreen() {
        // Do not use default values, we only need to serve the sessions API call.
        mockWebServer.dispatcher = MockNetworkDispatcher().apply {
            addMockRequests(
                post("/auth/v4/sessions")
                    respondWith "/auth/v4/sessions/sessions_logged_out_placeholder.json"
                    withStatusCode 200
            )
        }

        navigator { openApp() }

        deviceRobot {
            pressBack()

            verify { isMainActivityNotDisplayed() }
        }
    }
}
