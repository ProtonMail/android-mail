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

package ch.protonmail.android.uitest.rule

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.protonmail.android.MainActivity
import ch.protonmail.android.networkmocks.mockwebserver.MockWebServerDispatcher
import dagger.hilt.android.testing.HiltAndroidRule
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Creates a [RuleChain] that contains a [HiltAndroidRule], a [ComposeTestRule] and a [MockWebServerTestRule].
 *
 * **Required**
 * @param mockWebServer A lambda that returns a [MockWebServer] to be used in the [RuleChain].
 *  The [MockWebServer] must be the one provided by Hilt.
 *  Example:
 *  ```kotlin
 *  @Inject
 *  lateinit var mockWebServer: MockWebServer
 *
 *  @get:Rule
 *  val mockWebServerTestRule = createMockWebServerRuleChain(
 *      mockWebServer = ::mockWebServer,
 *  )
 *  ```
 *
 * **Optional**
 * @param T The type of the test class to be provided to [HiltAndroidRule].
 * @param dispatcher The [Dispatcher] to be used for the [MockWebServer].
 * @param composeTestRule The [ComposeTestRule] to be used in the [RuleChain].
 *  If not provided, a [ComposeTestRule] for [MainActivity] will be created.
 *  Provide an explicit [ComposeTestRule] if you need a reference to it in your test suite or you need a different
 *  [ComposeTestRule].
 * @param hiltRule The [HiltAndroidRule] to be used in the [RuleChain].
 *  It will be used to setup the [MockWebServer] and provide it to the Dagger's graph.
 * @param loginTestRule The [LoginTestRule] to be used in the [RuleChain].
 */
fun <T : Any> T.createMockWebServerRuleChain(
    mockWebServer: () -> MockWebServer,
    dispatcher: Dispatcher = MockWebServerDispatcher(),
    composeTestRule: ComposeTestRule = createAndroidComposeRule<MainActivity>(),
    hiltRule: HiltAndroidRule = HiltAndroidRule(this),
    loginTestRule: LoginTestRule = NoLoginTestRule
): RuleChain {
    val mockWebServerTestRule = MockWebServerTestRule(
        hiltRule = hiltRule,
        mockWebServer = mockWebServer,
        dispatcher = dispatcher
    )

    return RuleChain
        .outerRule(hiltRule)
        .around(createMainInitializerRule())
        .around(composeTestRule)
        .around(mockWebServerTestRule)
        .around(loginTestRule)
}

/**
 * A [TestWatcher] that setup a [MockWebServer] before each test and shuts it down after each test.
 */
class MockWebServerTestRule(
    private val hiltRule: HiltAndroidRule,
    mockWebServer: () -> MockWebServer,
    private val dispatcher: Dispatcher
) : TestWatcher() {

    private val mockWebServer by lazy {
        hiltRule.inject()
        mockWebServer()
    }

    override fun starting(description: Description) {
        super.starting(description)
        mockWebServer.dispatcher = dispatcher
    }

    override fun finished(description: Description) {
        super.finished(description)
        mockWebServer.shutdown()
    }
}
