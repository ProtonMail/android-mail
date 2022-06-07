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

import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SettingsFlowTest : BaseTest() {

    private val user = quark.userCreate()
    private val menuRobot = MenuRobot(composeTestRule)

    @Before
    fun setUp() {
        login(user)
    }

    @Test
    @Category(SmokeTest::class)
    fun openAccountSettings() {

        menuRobot
            .settings()
            .openUserAccountSettings()
            .verify { accountSettingsOpened(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openConversationModeSetting() {
        menuRobot
            .settings()
            .openUserAccountSettings()
            .also { it.verify { accountSettingsOpened(composeTestRule) } }
            .conversationMode()
            .also { it.verify { conversationModeToggleShown(composeTestRule) } }
    }

    @Test
    @Category(SmokeTest::class)
    fun openSettingAndChangePreferredTheme() {
        val themeSettingsRobot = menuRobot
            .settings()
            .selectThemeSettings()

        themeSettingsRobot
            .selectSystemDefault()
            .verify { defaultThemeSettingShown(composeTestRule) }
        themeSettingsRobot
            .selectDarkTheme()
            .verify { darkThemeSelected(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openSettingAndChangePreferredLanguage() {
        val languageSettingsRobot = menuRobot
            .settings()
            .selectLanguageSettings()

        languageSettingsRobot
            .selectSystemDefault()
            .verify { defaultLanguagesScreenIsShown(composeTestRule) }

        languageSettingsRobot
            .selectSpanish()
            .verify {
                spanishLanguageSelected(composeTestRule)
                appLanguageChangedToSpanish(composeTestRule)
            }

        languageSettingsRobot
            .selectBrazilianPortuguese()
            .verify {
                brazilianPortugueseLanguageSelected(composeTestRule)
                appLanguageChangedToPortuguese(composeTestRule)
            }

        composeTestRule.waitForIdle()

        /*
         * Once Brazilian was selected, we can't just use `selectSystemDefault` to go back to default language,
         * since the values returned from `string.mail_settings_system_default` are still the default language ones
         * while the app is now in Brazilian, which causes a failure as "System default" string is not found.
         * The assumption is that this happens because the Instrumentation's context is not updated when changing lang
         */
        languageSettingsRobot
            .fromBrazilianToSystemDefault()
            .verify { defaultLanguagesScreenIsShown(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openPasswordManagementSettings() {
        menuRobot
            .settings()
            .openUserAccountSettings()
            .passwordManagement()
            .verify { passwordManagementElementsDisplayed() }
    }
}
