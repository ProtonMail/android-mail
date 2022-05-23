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

package ch.protonmail.android.uitest.test.settings.appsettings

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.AccountInfo
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.TEST_TAG_SETTINGS_LIST
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.util.assertTextContains
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsState = Data(
        AccountInfo("ProtonTest", "user-test@proton.ch"),
        AppSettings(
            hasAutoLock = false,
            hasAlternativeRouting = true,
            customAppLanguage = null,
            hasCombinedContacts = true
        ),
        AppInformation(appVersionName = "6.0.0-alpha-adf8373a")
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ProtonTheme {
                MainSettingsScreen(
                    state = settingsState,
                    onAccountClick = {},
                    onThemeClick = {},
                    onPushNotificationsClick = {},
                    onAutoLockClick = {},
                    onAlternativeRoutingClick = {},
                    onAppLanguageClick = {},
                    onCombinedContactsClick = {},
                    onSwipeActionsClick = {},
                    onBackClick = {}
                )
            }
        }
    }

    @Test
    @Category(SmokeTest::class)
    fun testSettingsScreenContainsAllExpectedSections() {
        composeTestRule.onNodeWithText(string.mail_settings_account_settings).assertIsDisplayed()
        composeTestRule.onNodeWithText(string.mail_settings_app_settings).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_app_information))
            .assertIsDisplayed()
    }

    @Test
    @Category(SmokeTest::class)
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
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_swipe_actions))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_app_version))

        composeTestRule
            .onNodeWithText("6.0.0-alpha-adf8373a")
            .assertHasNoClickAction()
            .assertIsDisplayed()
    }

}
