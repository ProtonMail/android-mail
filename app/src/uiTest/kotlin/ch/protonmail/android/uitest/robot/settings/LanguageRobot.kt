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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.presentation.settings.language.TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL

/**
 * [LanguageRobot] class contains actions and verifications for LanguageSettingsScreen
 */
class LanguageRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    fun selectSystemDefault(): LanguageRobot {
        composeTestRule!!
            .onNodeWithText("System default")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        return this
    }

    fun selectSpanish(): LanguageRobot {
        composeTestRule!!
            .onNodeWithText("Español (España)")
            .performClick()
        composeTestRule.waitForIdle()
        return this
    }

    fun selectPortuguese(): LanguageRobot {
        composeTestRule!!
            .onNodeWithTag(TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL)
            .performScrollToNode(hasText("Português (Portugal)"))

        composeTestRule
            .onNodeWithText("Português (Portugal)")
            .performClick()

        composeTestRule.waitForIdle()
        return this
    }

    /**
     * Contains all the validations that can be performed by [LanguageRobot].
     */
    class Verify {

        fun spanishLanguageSelected(composeRule: ComposeContentTestRule) {
            verifyLanguageSelected(composeRule, "Español (España)")
        }

        fun portugueseLanguageSelected(composeRule: ComposeContentTestRule) {
            verifyLanguageSelected(composeRule, "Português (Portugal)")
        }

        fun appLanguageChangedToSpanish(composeRule: ComposeContentTestRule) {
            verifyScreenTitle(composeRule, "Idioma de la aplicación")
        }

        fun appLanguageChangedToPortuguese(composeRule: ComposeContentTestRule) {
            verifyScreenTitle(composeRule, "Idioma do aplicativo")
        }

        fun defaultLanguagesScreenIsShown(composeRule: ComposeContentTestRule) {
            verifyScreenTitle(composeRule, "App language")

            composeRule
                .onNodeWithText("System default")
                .onChild()
                .assertIsDisplayed()
                .assertIsSelected()

            val languages = listOf("Català", "Dansk", "Deutsch", "English", "Français")
            assertLanguagesAreShownButUnselected(composeRule, languages)
        }


        private fun verifyLanguageSelected(
            composeRule: ComposeContentTestRule,
            text: String
        ) {
            composeRule
                .onNodeWithText(text)
                .onChild() // selects the radio button node
                .assertIsSelected()
        }

        private fun verifyScreenTitle(
            composeRule: ComposeContentTestRule,
            text: String
        ) {
            composeRule
                .onNodeWithText(text)
                .assertIsDisplayed()
        }

        private fun assertLanguagesAreShownButUnselected(
            composeRule: ComposeContentTestRule,
            languages: List<String>
        ) {
            languages.forEach { language ->
                composeRule
                    .onNodeWithText(language)
                    .onChild()
                    .assertIsDisplayed()
                    .assertIsNotSelected()
            }
        }
    }

    inline fun verify(block: Verify.() -> Unit) =
        Verify().apply(block)
}

