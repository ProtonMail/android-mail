/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitest.robot.settings.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_LIST
import ch.protonmail.android.mailsettings.presentation.accountsettings.TEST_TAG_ACCOUNT_SETTINGS_SCREEN
import ch.protonmail.android.uitest.robot.settings.SettingsRobot
import ch.protonmail.android.uitest.robot.settings.account.labelsandfolders.LabelsAndFoldersRobot
import ch.protonmail.android.uitest.robot.settings.account.privacy.PrivacySettingsRobot
import ch.protonmail.android.uitest.robot.settings.account.swipinggestures.SwipingGesturesSettingsRobot

/**
 * [AccountSettingsRobot] class contains actions and verifications for
 * Account settings functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
class AccountSettingsRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    fun subscription(): SubscriptionRobot {
        return SubscriptionRobot()
    }

    fun privacy(): PrivacySettingsRobot {
        return PrivacySettingsRobot()
    }

    fun defaultEmailAddress(): DefaultEmailAddressRobot {
        return DefaultEmailAddressRobot()
    }

    fun displayNameAndSignature(): DisplayNameAndSignatureRobot {
        return DisplayNameAndSignatureRobot()
    }

    fun foldersAndLabels(): LabelsAndFoldersRobot {
        return LabelsAndFoldersRobot()
    }

    fun navigateUpToSettings(): SettingsRobot {
        return SettingsRobot()
    }

    fun swipingGestures(): SwipingGesturesSettingsRobot {
        return SwipingGesturesSettingsRobot()
    }

    fun conversationMode(): ConversationModeRobot {
        composeTestRule!!
            .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
            .onChild()
            .performScrollToNode(hasText("Conversation mode"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Conversation mode")
            .performClick()
        composeTestRule.waitForIdle()
        return ConversationModeRobot(composeTestRule)
    }

    /**
     * Contains all the validations that can be performed by [AccountSettingsRobot].
     */
    class Verify {

        fun accountSettingsOpened(composeRule: ComposeContentTestRule) {
            composeRule.waitUntil(timeoutMillis = 5000) {
                composeRule
                    .onAllNodesWithTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }
            composeRule
                .onNodeWithTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN)
                .assertIsDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
