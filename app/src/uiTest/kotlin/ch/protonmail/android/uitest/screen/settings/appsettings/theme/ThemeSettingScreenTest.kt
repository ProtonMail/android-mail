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

package ch.protonmail.android.uitest.screen.settings.appsettings.theme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.model.Theme.DARK
import ch.protonmail.android.mailsettings.domain.model.Theme.LIGHT
import ch.protonmail.android.mailsettings.domain.model.Theme.SYSTEM_DEFAULT
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeUiModel
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RegressionTest
@HiltAndroidTest
internal class ThemeSettingScreenTest : HiltInstrumentedTest() {

    @Test
    fun testOnlySystemDefaultIsSelectedWhenThemeIsSystemDefault() {
        setupScreenWithSystemDefaultTheme()

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

    @Test
    fun testLightIsSelectedWhenThemeIsLight() {
        setupScreenWithLightTheme()

        composeTestRule
            .onNodeWithText(string.mail_settings_system_default)
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme_light)
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme_dark)
            .assertIsDisplayed()
            .assertIsNotSelected()
    }

    @Test
    fun testDarkIsSelectedWhenThemeIsDark() {
        setupScreenWithDarkTheme()

        composeTestRule
            .onNodeWithText(string.mail_settings_system_default)
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme_light)
            .assertIsDisplayed()
            .assertIsNotSelected()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme_dark)
            .assertIsDisplayed()
            .assertIsSelected()
    }

    @Test
    fun testCallbackIsInvokedWithThemeIdWhenAThemeIsSelected() {
        var selectedTheme: Theme? = null
        setupScreenWithSystemDefaultTheme {
            selectedTheme = it
        }
        assertNull(selectedTheme)

        composeTestRule
            .onNodeWithText(string.mail_settings_system_default)
            .assertIsDisplayed()
            .assertIsSelected()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme_dark)
            .performClick()

        assertEquals(DARK, selectedTheme)
    }

    private fun setupScreenWithLightTheme() {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = false, isLight = true, isDark = false)
            )
        )
    }

    private fun setupScreenWithDarkTheme() {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = false, isLight = false, isDark = true)
            )
        )
    }

    private fun setupScreenWithSystemDefaultTheme(onThemeSelected: (Theme) -> Unit = {}) {
        setupScreenWithState(
            Data(
                buildThemesList(isSystemDefault = true, isLight = false, isDark = false)
            ),
            onThemeSelected
        )
    }

    private fun buildThemesList(
        isSystemDefault: Boolean,
        isLight: Boolean,
        isDark: Boolean
    ) = listOf(
        ThemeUiModel(SYSTEM_DEFAULT, string.mail_settings_system_default, isSystemDefault),
        ThemeUiModel(LIGHT, string.mail_settings_theme_light, isLight),
        ThemeUiModel(DARK, string.mail_settings_theme_dark, isDark)
    )

    private fun setupScreenWithState(
        state: Data,
        onThemeSelected: (Theme) -> Unit = {}
    ) {
        composeTestRule.setContent {
            ProtonTheme {
                ThemeSettingsScreen(
                    onBackClick = { },
                    onThemeSelected = onThemeSelected,
                    state = state
                )
            }
        }
    }
}
