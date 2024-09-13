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
import ch.protonmail.android.mailsettings.presentation.settings.SettingsScreenTestTags
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.robot.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitest.robot.settings.swipeactions.SwipeActionsRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitProgressIsHidden
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText

internal class SettingsRobot : ComposeRobot() {
    private val rootItem = composeTestRule.onNodeWithTag(SettingsScreenTestTags.RootItem)

    init {
        rootItem.awaitDisplayed()
    }

    fun openLanguageSettings(): LanguageRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_app_language)
            .performClick()
        composeTestRule.waitForIdle()

        return LanguageRobot()
    }

    fun openSwipeActions(): SwipeActionsRobot {
        composeTestRule
            .onList()
            .performScrollToNode(hasText(string.mail_settings_swipe_actions))

        composeTestRule
            .onNodeWithText(string.mail_settings_swipe_actions)
            .performClick()

        composeTestRule.waitForIdle()

        return SwipeActionsRobot()
    }

    fun openUserAccountSettings(): AccountSettingsRobot {
        composeTestRule
            .onNodeWithTag(SettingsScreenTestTags.AccountSettingsItem)
            .performClick()

        composeTestRule.awaitProgressIsHidden()

        return AccountSettingsRobot()
    }

    fun openThemeSettings(): ThemeRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_theme)
            .performClick()
        composeTestRule.waitForIdle()

        return ThemeRobot()
    }

    fun openCombinedContactsSettings(): CombinedContactsRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_combined_contacts)
            .performClick()
        composeTestRule.waitForIdle()

        return CombinedContactsRobot()
    }

    fun openAlternativeRoutingSettings(): AlternativeRoutingRobot {
        composeTestRule
            .onNodeWithText(string.mail_settings_alternative_routing)
            .performClick()
        composeTestRule.waitForIdle()

        return AlternativeRoutingRobot()
    }

    private fun ComposeTestRule.onList(): SemanticsNodeInteraction =
        onAllNodes(hasScrollAction()).onFirst() // second is drawer
}
