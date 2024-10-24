/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and Proton Mail.
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

package ch.protonmail.android.uitest.e2e.menu

import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.MainActivity
import ch.protonmail.android.initializer.MainInitializer
import ch.protonmail.android.test.annotations.suite.CoreLibraryTest
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.di.LocalhostApi
import ch.protonmail.android.uitest.di.LocalhostApiModule
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.verify
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.rule.GrantNotificationsPermissionRule
import ch.protonmail.android.uitest.rule.MockOnboardingRuntimeRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.plan.test.MinimalSubscriptionTests
import me.proton.core.plan.test.robot.SubscriptionRobot
import me.proton.core.test.rule.extension.protonAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

@CoreLibraryTest
@HiltAndroidTest
@UninstallModules(LocalhostApiModule::class)
internal class SidebarSubscriptionFlowTest : MinimalSubscriptionTests() {

    @JvmField
    @BindValue
    @LocalhostApi
    val localhostApi = false

    @Inject
    lateinit var mockOnboardingRuntimeRule: MockOnboardingRuntimeRule

    @get:Rule
    val protonTestRule = protonAndroidComposeRule<MainActivity>(
        composeTestRule = ComposeTestRuleHolder.createAndGetComposeRule(),
        logoutBefore = false,
        fusionEnabled = false,
        additionalRules = linkedSetOf(GrantNotificationsPermissionRule()),
        afterHilt = { mainInitializer() }
    )

    @Before
    fun setup() {
        mockOnboardingRuntimeRule(false)
    }

    override fun startSubscription(): SubscriptionRobot {
        navigator { navigateTo(Destination.Inbox, performLoginViaUI = false) }
        mailboxRobot { verify { isShown() } }

        menuRobot {
            openSidebarMenu()
            openSubscription()
        }

        return SubscriptionRobot
    }

    private fun mainInitializer() = runBlocking {
        withContext(Dispatchers.Main) { MainInitializer.init(ApplicationProvider.getApplicationContext()) }
    }
}
