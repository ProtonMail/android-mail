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

package ch.protonmail.android.uitest.screen.settings.appsettings

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.AccountInfo
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.SettingsScreenTestTags
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.util.HiltInstrumentedTest
import ch.protonmail.android.uitest.util.assertions.assertTextContains
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Before
import org.junit.Test

@SmokeTest
@HiltAndroidTest
internal class SettingsScreenTest : HiltInstrumentedTest() {

    private val settingsState = Data(
        AccountInfo("ProtonTest", "user-test@proton.ch"),
        AppSettings(
            hasAutoLock = false,
            hasAlternativeRouting = true,
            customAppLanguage = null,
            hasCombinedContacts = true
        ),
        AppInformation(appVersionName = "6.0.0-alpha-adf8373a", appVersionCode = 9026)
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ProtonTheme {
                MainSettingsScreen(
                    state = settingsState,
                    actions = MainSettingsScreen.Actions(
                        onAccountClick = {},
                        onThemeClick = {},
                        onPushNotificationsClick = {},
                        onAutoLockClick = {},
                        onAlternativeRoutingClick = {},
                        onAppLanguageClick = {},
                        onCombinedContactsClick = {},
                        onSwipeActionsClick = {},
                        onClearCacheClick = {},
                        onBackClick = {},
                        onExportLogsClick = {},
                        onCustomizeToolbarClick = {}
                    ),
                    LogsExportFeatureSetting(enabled = false, internalEnabled = false)
                )
            }
        }
    }

    @Test
    fun testSettingsScreenContainsAllExpectedSections() {
        composeTestRule.onNodeWithText(string.mail_settings_account_settings).assertIsDisplayed()
        composeTestRule.onNodeWithText(string.mail_settings_app_settings).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_app_information))
            .assertIsDisplayed()
    }

    @Test
    fun testSettingsScreenDisplayStateCorrectly() {
        composeTestRule
            .onNodeWithText("ProtonTest")
            .assertTextContains("user-test@proton.ch")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_theme)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_push_notifications)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_auto_lock)
            .assertTextContains(string.mail_settings_disabled)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_alternative_routing)
            .assertTextContains(string.mail_settings_allowed)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_app_language)
            .assertTextContains(string.mail_settings_auto_detect)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_combined_contacts)
            .assertTextContains(string.mail_settings_enabled)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_local_cache))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_customize_toolbar))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_swipe_actions))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_app_version))

        composeTestRule
            .onNodeWithText("6.0.0-alpha-adf8373a (9026)")
            .assertHasNoClickAction()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.SettingsList)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_app_version))
    }
}
