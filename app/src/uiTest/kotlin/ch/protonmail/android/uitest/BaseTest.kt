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

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.MainActivity
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.rule.GrantNotificationsPermissionRule
import ch.protonmail.android.uitest.rule.HiltInjectRule
import ch.protonmail.android.uitest.rule.MainInitializerRule
import ch.protonmail.android.uitest.rule.MockOnboardingRuntimeRule
import ch.protonmail.android.uitest.rule.SpotlightSeenRule
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import me.proton.core.auth.domain.entity.SessionInfo
import me.proton.core.auth.domain.testing.LoginTestHelper
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.mailsettings.domain.repository.getMailSettingsOrNull
import me.proton.core.test.android.instrumented.utils.Shell.setupDeviceForAutomation
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import timber.log.Timber
import javax.inject.Inject

/**
 * @param logoutUsersOnTearDown by default, revoke the user's session against the API
 * and logout the user after each test.
 * If using orchestrator with `clearPackageData` option this might be redundant (might still be
 * beneficial as it doesn't leave open sessions but doesn't impact the test itself)
 */
internal open class BaseTest(
    private val logoutUsersOnTearDown: Boolean = true
) {

    @get:Rule(order = RuleOrder_00_First)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = RuleOrder_10_Initialization)
    val mainInitializerRule = MainInitializerRule()

    @get:Rule(order = RuleOrder_11_Initialized)
    val grantNotificationsPermissionRule = GrantNotificationsPermissionRule()

    @get:Rule(order = RuleOrder_20_Injection)
    val hiltInjectRule = HiltInjectRule(hiltRule)

    @get:Rule(order = RuleOrder_30_ActivityLaunch)
    val composeTestRule: ComposeTestRule = ComposeTestRuleHolder.createAndGetComposeRule()

    @Inject
    lateinit var loginTestHelper: LoginTestHelper

    @Inject
    lateinit var mailSettingsRepo: MailSettingsRepository

    @Inject
    lateinit var mockOnboardingRuntimeRule: MockOnboardingRuntimeRule

    @Inject
    lateinit var spotlightSeenRule: SpotlightSeenRule

    @Before
    open fun setup() {
        setupDeviceForAutomation(true)
        loginTestHelper.logoutAll()
        mockOnboardingRuntimeRule(shouldForceShow = false)
        spotlightSeenRule.invoke(seen = true)

        ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    open fun cleanup() {
        if (logoutUsersOnTearDown) {
            Timber.d("Finishing Testing: Revoking user sessions and logging out")
            loginTestHelper.logoutAll()
        }
    }

    fun loginAndAwaitData(user: User) {
        val sessionInfo = login(user)

        composeTestRule.waitUntil(5_000) {
            runBlocking { mailSettingsRepo.getMailSettingsOrNull(sessionInfo.userId) != null }
        }
    }

    fun login(user: User): SessionInfo {
        Timber.d("Login user: ${user.name}")
        return loginTestHelper.login(user.name, user.password)
    }

    companion object {

        const val RuleOrder_00_First = 0
        const val RuleOrder_10_Initialization = 10
        const val RuleOrder_11_Initialized = 11
        const val RuleOrder_20_Injection = 20
        const val RuleOrder_21_Injected = 21
        const val RuleOrder_30_ActivityLaunch = 30
        const val RuleOrder_31_ActivityLaunched = 31
        const val RuleOrder_99_Last = 99

        private val context: Context
            get() = InstrumentationRegistry.getInstrumentation().context

        val users = User.Users.fromJson(
            json = context.assets.open("users.json").bufferedReader().use { it.readText() }
        )

        private val envConfig: EnvironmentConfiguration = EnvironmentConfiguration.fromClass()

        val quark = Quark.fromJson(
            json = context.assets.open("internal_api.json").bufferedReader().use { it.readText() },
            host = envConfig.host,
            proxyToken = BuildConfig.PROXY_TOKEN
        )

        @JvmStatic
        @BeforeClass
        fun prepare() {
            setupDeviceForAutomation(true)
        }
    }
}
