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
package ch.protonmail.android.uitest.robot.settings.account

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_LIST
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_SCREEN
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.test.android.robots.settings.PasswordManagementRobot

@AsDsl
internal class AccountSettingsRobot : ComposeRobot() {

    fun openConversationMode(): ConversationModeRobot {
        clickOnAccountListItemWithText(string.mail_settings_conversation_mode)
        return ConversationModeRobot()
    }

    fun openPasswordManagement(): PasswordManagementRobot {
        clickOnAccountListItemWithText(string.mail_settings_password_management)
        return PasswordManagementRobot()
    }

    private fun clickOnAccountListItemWithText(@StringRes itemNameRes: Int) {
        composeTestRule
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText(itemNameRes))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(itemNameRes)
            .performClick()
        composeTestRule.waitForIdle()
    }

    @VerifiesOuter
    inner class Verify {

        fun accountSettingsScreenIsDisplayed() {
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule
                    .onAllNodesWithTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }
            composeTestRule
                .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN)
                .assertIsDisplayed()
        }
    }
}
