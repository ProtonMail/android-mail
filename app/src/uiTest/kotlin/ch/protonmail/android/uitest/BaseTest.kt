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

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.MailTestEntryPoint
import ch.protonmail.android.test.BuildConfig
import kotlinx.coroutines.runBlocking
import me.proton.core.auth.domain.entity.SessionInfo
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.mailsettings.domain.repository.getMailSettingsOrNull
import me.proton.core.test.android.instrumented.utils.Shell.setupDeviceForAutomation
import me.proton.core.test.android.plugins.Quark
import me.proton.core.test.android.plugins.data.User
import me.proton.core.test.android.plugins.data.User.Users
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestName
import timber.log.Timber

/**
 * @param logoutUsersOnTearDown by default, revoke the user's session against the API
 * and logout the user after each test.
 * If using orchestrator with `clearPackageData` option this might be redundant (might still be
 * beneficial as it doesn't leave open sessions but doesn't impact the test itself)
 */
open class BaseTest(
    private val logoutUsersOnTearDown: Boolean = true
) {

    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Rule
    @JvmField
    val ruleChain: RuleChain = RuleChain
        .outerRule(TestName())
        .around(composeTestRule)

    @After
    fun cleanup() {
        if (logoutUsersOnTearDown) {
            Timber.d("Finishing Testing: Revoking user sessions and logging out")
            authHelper.logoutAll()
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
        return authHelper.login(user.name, user.password)
    }

    companion object {
        val users = Users("users.json")
        val quark = Quark(BuildConfig.HOST, BuildConfig.PROXY_TOKEN, "internal_api.json")
        val authHelper = ProtonTestEntryPoint.provide(
            ApplicationProvider.getApplicationContext<Application>()
        )
        val mailSettingsRepo = MailTestEntryPoint.provide(
            ApplicationProvider.getApplicationContext<Application>()
        )

        @JvmStatic
        @BeforeClass
        fun prepare() {
            setupDeviceForAutomation(true)
            authHelper.logoutAll()
        }
    }
}
