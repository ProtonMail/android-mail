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

package ch.protonmail.android.uitest.robot.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * [ThemeRobot] class contains actions and verifications for ThemeSettingScreen
 */
class ThemeRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    fun selectDarkTheme(): ThemeRobot {
        composeTestRule!!
            .onNodeWithText("Dark")
            .performClick()
        return this
    }

    /**
     * Contains all the validations that can be performed by [ThemeRobot].
     */
    class Verify {

        fun darkThemeSelected(composeRule: ComposeContentTestRule) {
            composeRule
                .onNodeWithText("Dark")
                .assertIsSelected()
        }

        fun defaultThemeSettingShown(composeRule: ComposeContentTestRule) {
            composeRule
                .onNodeWithText("Theme")
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("System default")
                .assertIsDisplayed()
                .assertIsSelected()

            composeRule
                .onNodeWithText("Light")
                .assertIsDisplayed()
                .assertIsNotSelected()

            composeRule
                .onNodeWithText("Dark")
                .assertIsDisplayed()
                .assertIsNotSelected()
        }
    }

    inline fun verify(block: Verify.() -> Unit) =
        Verify().apply(block)
}

