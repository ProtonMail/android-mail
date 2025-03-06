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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.domain.model.AppLanguage
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.language.TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.onNodeWithText

internal class LanguageRobot : ComposeRobot() {

    fun selectBrazilianPortuguese(): LanguageRobot {
        composeTestRule
            .onNodeWithTag(TEST_TAG_LANG_SETTINGS_SCREEN_SCROLL_COL)
            .performScrollToNode(hasText(AppLanguage.PORTUGUESE_BRAZILIAN.langName))

        composeTestRule
            .onNodeWithText(AppLanguage.PORTUGUESE_BRAZILIAN.langName)
            .performClick()

        composeTestRule.waitForIdle()
        return this
    }

    fun selectSpanish(): LanguageRobot {
        composeTestRule
            .onNodeWithText(AppLanguage.SPANISH.langName)
            .performClick()
        composeTestRule.waitForIdle()
        return this
    }

    fun selectSystemDefault(): LanguageRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_system_default)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        return this
    }

    fun selectSystemDefaultFromBrazilian(): LanguageRobot {
        composeTestRule
            .onNodeWithText("Padrão do sistema")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        return this
    }

    @VerifiesOuter
    inner class Verify {

        fun appLanguageChangedToPortuguese() {
            verifyScreenTitleMatchesText("Idioma do aplicativo")
        }

        fun appLanguageChangedToSpanish() {
            verifyScreenTitleMatchesText("Idioma de la aplicación")
        }

        fun brazilianPortugueseLanguageIsSelected() {
            verifyLanguageIsSelected(AppLanguage.PORTUGUESE_BRAZILIAN.langName)
        }

        fun spanishLanguageIsSelected() {
            verifyLanguageIsSelected(AppLanguage.SPANISH.langName)
        }

        fun defaultLanguageIsSelected() {
            verifyScreenTitleMatchesText(string.mail_settings_app_language)

            composeTestRule
                .onNodeWithText(string.mail_settings_system_default)
                .assertIsDisplayed()
                .assertIsSelected()

            val languages = listOf("English", "Deutsch", "Français", "Nederlands", "Español (España)")
            assertLanguagesAreShownButUnselected(languages)
        }

        private fun assertLanguagesAreShownButUnselected(languages: List<String>) {
            languages.forEach { language ->
                composeTestRule
                    .onNodeWithText(language)
                    .assertIsDisplayed()
                    .assertIsNotSelected()
            }
        }

        private fun verifyLanguageIsSelected(text: String) {
            composeTestRule
                .onNodeWithText(text)
                .assertIsSelected()
        }

        private fun verifyScreenTitleMatchesText(text: String) {
            composeTestRule
                .onNodeWithText(text)
                .assertIsDisplayed()
        }

        private fun verifyScreenTitleMatchesText(@StringRes text: Int) {
            composeTestRule
                .onNodeWithText(text)
                .assertIsDisplayed()
        }
    }
}
