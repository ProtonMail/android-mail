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

package ch.protonmail.android.uitest.robot.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.onNodeWithText

/**
 * [ThemeRobot] class contains actions and verifications for ThemeSettingScreen
 */
internal class ThemeRobot : ComposeRobot() {

    fun selectDarkTheme(): ThemeRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_theme_dark)
            .performClick()
        composeTestRule.waitUntil { optionWithTextIsSelected(composeTestRule, string.mail_settings_theme_dark) }
        return this
    }

    fun selectSystemDefault(): ThemeRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_system_default)
            .performClick()
        composeTestRule.waitUntil { optionWithTextIsSelected(composeTestRule, string.mail_settings_system_default) }
        return this
    }

    private fun optionWithTextIsSelected(
        composeTestRule: ComposeTestRule,
        @StringRes text: Int
    ): Boolean {
        try {
            composeTestRule
                .onNodeWithText(text)
                .assertIsSelected()
        } catch (ignored: AssertionError) {
            return false
        }
        return true
    }

    @VerifiesOuter
    inner class Verify {

        fun darkThemeIsSelected() {
            composeTestRule
                .onNodeWithText(string.mail_settings_theme_dark)
                .assertIsSelected()
        }

        fun defaultThemeSettingIsSelected() {
            composeTestRule
                .onNodeWithText(string.mail_settings_theme)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(string.mail_settings_system_default)
                .assertIsDisplayed()
                .assertIsSelected()

            composeTestRule
                .onNodeWithText(string.mail_settings_theme_light)
                .assertIsDisplayed()
                .assertIsNotSelected()

            composeTestRule
                .onNodeWithText(string.mail_settings_theme_dark)
                .assertIsDisplayed()
                .assertIsNotSelected()
        }
    }
}
