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

package ch.protonmail.android.uitest.e2e.settings

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import ch.protonmail.android.uitest.robot.settings.account.verify
import ch.protonmail.android.uitest.robot.settings.swipeactions.verify
import ch.protonmail.android.uitest.robot.settings.verify
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@RegressionTest
@UninstallModules(ServerProofModule::class)
@HiltAndroidTest
internal class SettingsFlowTest : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private val menuRobot = MenuRobot()

    @Before
    fun setupDispatcher() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher()
        navigator { navigateTo(Destination.Inbox) }
    }

    @Test
    fun openAccountSettings() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openUserAccountSettings()
            .verify { accountSettingsScreenIsDisplayed() }
    }

    @Test
    fun openConversationModeSetting() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openUserAccountSettings()
            .verify { accountSettingsScreenIsDisplayed() }
            .openConversationMode()
            .verify { conversationModeToggleIsDisplayedAndEnabled() }
    }

    @Test
    fun openSettingAndChangePreferredTheme() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openThemeSettings()
            .selectSystemDefault()
            .verify { defaultThemeSettingIsSelected() }
            .selectDarkTheme()
            .verify { darkThemeIsSelected() }
    }

    @Test
    fun openSettingAndChangePreferredLanguage() {
        val languageSettingsRobot = menuRobot
            .openSidebarMenu()
            .openSettings()
            .openLanguageSettings()
            .selectSystemDefault()
            .verify { defaultLanguageIsSelected() }
            .selectSpanish()
            .verify {
                spanishLanguageIsSelected()
                appLanguageChangedToSpanish()
            }
            .selectBrazilianPortuguese()
            .verify {
                brazilianPortugueseLanguageIsSelected()
                appLanguageChangedToPortuguese()
            }

        ComposeTestRuleHolder.rule.waitForIdle()

        /*
         * Once Brazilian was selected, we can't just use `selectSystemDefault` to go back to default language,
         * since the values returned from `string.mail_settings_system_default` are still the default language ones
         * while the app is now in Brazilian, which causes a failure as "System default" string is not found.
         * The assumption is that this happens because the Instrumentation's context is not updated when changing lang
         */
        languageSettingsRobot
            .selectSystemDefaultFromBrazilian()
            .verify { defaultLanguageIsSelected() }
    }

    @Test
    fun openPasswordManagementSettings() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openUserAccountSettings()
            .openPasswordManagement()
            .verify { passwordManagementElementsDisplayed() }
    }

    @Test
    fun openSettingsAndChangeLeftSwipeAction() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openSwipeActions()
            .openSwipeLeft()
            .selectArchive()
            .navigateUpToSwipeActions()
            .verify { swipeLeft { isArchive() } }
    }

    @Test
    fun openSettingsAndChangeRightSwipeAction() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openSwipeActions()
            .openSwipeRight()
            .selectMarkRead()
            .navigateUpToSwipeActions()
            .verify { swipeRight { isMarkRead() } }
    }

    @Test
    fun openSettingsAndChangeCombinedContactsSetting() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openCombinedContactsSettings()
            .turnOnCombinedContacts()
            .verify { combinedContactsSettingIsToggled() }
    }

    @Test
    fun openSettingsAndChangeAlternativeRoutingSetting() {
        menuRobot
            .openSidebarMenu()
            .openSettings()
            .openAlternativeRoutingSettings()
            .turnOffAlternativeRouting()
            .verify { alternativeRoutingSettingIsToggled() }
    }
}
