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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM
import ch.protonmail.android.uitest.robot.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitest.robot.settings.swipeactions.SwipeActionsRobot
import ch.protonmail.android.uitest.util.awaitProgressIsHidden
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
internal class SettingsRobot(private val composeTestRule: ComposeTestRule) {

    fun openLanguageSettings(): LanguageRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_app_language)
            .performClick()
        composeTestRule.waitForIdle()

        return LanguageRobot(composeTestRule)
    }

    fun openSwipeActions(): SwipeActionsRobot {
        composeTestRule
            .onList()
            .performScrollToNode(hasText(string.mail_settings_swipe_actions))

        composeTestRule
            .onNodeWithText(string.mail_settings_swipe_actions)
            .performClick()

        composeTestRule.waitForIdle()

        return SwipeActionsRobot(composeTestRule)
    }

    fun openUserAccountSettings(): AccountSettingsRobot {
        composeTestRule
            .onNodeWithTag(TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM)
            .performClick()

        composeTestRule.awaitProgressIsHidden()

        return AccountSettingsRobot(composeTestRule)
    }

    fun openThemeSettings(): ThemeRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_theme)
            .performClick()
        composeTestRule.waitForIdle()

        return ThemeRobot(composeTestRule)
    }

    fun openCombinedContactsSettings(): CombinedContactsRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_combined_contacts)
            .performClick()
        composeTestRule.waitForIdle()

        return CombinedContactsRobot(composeTestRule)
    }

    fun openAlternativeRoutingSettings(): AlternativeRoutingRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_alternative_routing)
            .performClick()
        composeTestRule.waitForIdle()

        return AlternativeRoutingRobot(composeTestRule)
    }

    private fun ComposeTestRule.onList(): SemanticsNodeInteraction =
        onAllNodes(hasScrollAction()).onFirst() // second is drawer
}
