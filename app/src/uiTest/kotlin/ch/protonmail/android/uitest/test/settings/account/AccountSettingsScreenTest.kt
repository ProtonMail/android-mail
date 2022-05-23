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

package ch.protonmail.android.uitest.test.settings.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingScreen
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_LIST
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.util.assertTextContains
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.compose.theme.ProtonTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

class AccountSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsState = Data(
        recoveryEmail = "recovery-email@protonmail.com",
        mailboxSize = 20_000,
        mailboxUsedSpace = 15_000,
        defaultEmail = "contact@protonmail.ch",
        isConversationMode = true
    )

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ProtonTheme {
                AccountSettingScreen(
                    onBackClick = {},
                    onPasswordManagementClick = {},
                    onRecoveryEmailClick = {},
                    onConversationModeClick = {},
                    onDefaultEmailAddressClick = {},
                    onDisplayNameClick = {},
                    onPrivacyClick = {},
                    onSearchMessageContentClick = {},
                    onLabelsFoldersClick = {},
                    onLocalStorageClick = {},
                    onSnoozeNotificationsClick = {},
                    state = settingsState
                )
            }
        }
    }

    @Test
    @Category(SmokeTest::class)
    fun testAccountSettingsScreenContainsAllExpectedSections() {
        composeTestRule.onNodeWithText(string.mail_settings_account).assertIsDisplayed()
        composeTestRule.onNodeWithText(string.mail_settings_addresses).assertIsDisplayed()
        composeTestRule.onNodeWithText(string.mail_settings_mailbox).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_snooze))
            .assertIsDisplayed()
    }

    @Test
    @Category(SmokeTest::class)
    fun testAccountSettingsScreenDisplayStateCorrectly() {
        composeTestRule
            .onNodeWithText(string.mail_settings_recovery_email)
            .assertTextContains("recovery-email@protonmail.com")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_password_management)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_recovery_email)
            .assertTextContains("recovery-email@protonmail.com")
            .assertIsDisplayed()

        // Assert values individually as android's `Formatter.formatShortFileSize` method
        // adds many non-printable BiDi chars when executing on some virtual devices
        // so checking for "1 kB / 2 kB" would not find a match
        composeTestRule
            .onNodeWithText(string.mail_settings_mailbox_size)
            .assertTextContains(value = "15", substring = true)
            .assertTextContains(value = "20", substring = true)
            .assertTextContains(value = "kB", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_conversation_mode)
            .assertTextContains(string.mail_settings_enabled)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_default_email_address)
            .assertTextContains("contact@protonmail.ch")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string.mail_settings_display_name_and_signature)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_privacy))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_search_message_content))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_labels_and_folders))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_local_storage))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(string.mail_settings_snooze_notifications))
            .assertIsDisplayed()
    }

}
