/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest.test.settings

import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import org.junit.Before
import org.junit.Ignore
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

        languageSettingsRobot
            .selectSystemDefault()
            .verify { defaultLanguagesScreenIsShown(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    @Ignore("Ignored till functionality from MAILANDR-102 is implemented")
    fun openPasswordManagementSettings() {
        menuRobot
            .settings()
            .openUserAccountSettings()
            .passwordManagement()
            .verify { passwordManagementElementsDisplayed() }
    }
}
