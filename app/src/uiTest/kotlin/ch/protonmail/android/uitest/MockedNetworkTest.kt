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

package ch.protonmail.android.uitest

import androidx.compose.ui.test.junit4.ComposeTestRule
import ch.protonmail.android.uitest.helpers.core.TestIdWatcher
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.login.LoginType
import ch.protonmail.android.uitest.helpers.network.authenticationDispatcher
import ch.protonmail.android.uitest.rule.MainInitializerRule
import ch.protonmail.android.uitest.rule.MockTimeRule
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import javax.inject.Inject

/**
 * A base test class used in UI tests that require complete network isolation.
 *
 * @param loginType the login type to use for a given test suite.
 */
@HiltAndroidTest
internal open class MockedNetworkTest(
    private val loginType: LoginType = LoginTestUserTypes.Deprecated.GrumpyCat
) {

    private val hiltAndroidRule = HiltAndroidRule(this)

    private val composeTestRule: ComposeTestRule = ComposeTestRuleHolder.createAndGetComposeRule()

    @Inject
    lateinit var mockWebServer: MockWebServer

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(
        hiltAndroidRule
    ).around(
        composeTestRule
    ).around(
        MainInitializerRule()
    ).around(
        TestIdWatcher()
    ).around(
        MockTimeRule()
    )

    @Before
    fun setup() {
        hiltAndroidRule.inject()
        mockWebServer.dispatcher = authenticationDispatcher(loginType)
    }
}
