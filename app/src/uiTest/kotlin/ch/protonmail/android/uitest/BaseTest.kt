/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest

import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.AppDatabaseModule
import ch.protonmail.android.test.BuildConfig
import kotlinx.coroutines.runBlocking
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.test.android.instrumented.ProtonTest.Companion.getTargetContext
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

open class BaseTest(
    private val clearAppDatabaseOnTearDown: Boolean = true
) {

    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Rule
    @JvmField
    val ruleChain: RuleChain = RuleChain
        .outerRule(TestName())
        .around(composeTestRule)

    @After
    fun cleanup() {
        if (clearAppDatabaseOnTearDown) {
            Timber.d("Finishing Testing: Clearing all users from database")
            runBlocking {
                appDatabase.accountDao().deleteAll()
            }
        }
    }

    fun login(user: User) {
        Timber.d("Login user: ${user.name}")
        authHelper.login(user.name, user.password)
    }

    companion object {
        val users = Users("users.json")
        val quark = Quark(BuildConfig.HOST, BuildConfig.PROXY_TOKEN, "internal_api.json")
        val appDatabase = AppDatabaseModule.provideAppDatabase(getTargetContext())
        val authHelper = ProtonTestEntryPoint.provide(
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
