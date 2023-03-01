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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.protonmail.android.MainActivity
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.rule.MainInitializerRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.user.domain.UserManager
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import javax.inject.Inject

/**
 * A base test class used in UI tests that require complete network isolation.
 *
 * @param loginStrategy the login strategy to follow for a given test suite.
 */
@HiltAndroidTest
internal open class MockedNetworkTest(
    private val loginStrategy: LoginStrategy = LoginStrategy.LoggedIn.PrimaryUser
) {

    private val hiltAndroidRule = HiltAndroidRule(this)

    // To be defined here as long as Robots need the rule to be injected.
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var mockWebServer: MockWebServer

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(
        hiltAndroidRule
    ).around(
        composeTestRule
    ).around(
        MainInitializerRule()
    )

    @Before
    fun setup() {
        hiltAndroidRule.inject()

        handleLogin()
    }

    @After
    fun tearDown() {
        handleLogout()
    }

    private fun handleLogin() {
        when (loginStrategy) {
            is LoginStrategy.LoggedIn.PrimaryUser -> {
                runBlocking(Dispatchers.IO) {
                    accountManager.addAccount(loginStrategy.account, loginStrategy.session)
                    userManager.addUser(loginStrategy.user, emptyList())
                }
            }

            else -> Unit
        }
    }

    private fun handleLogout() {
        when (loginStrategy) {
            is LoginStrategy.LoggedIn.PrimaryUser -> {
                runBlocking(Dispatchers.IO) {
                    accountManager.removeAccount(loginStrategy.account.userId)
                }
            }

            else -> Unit
        }
    }
}
