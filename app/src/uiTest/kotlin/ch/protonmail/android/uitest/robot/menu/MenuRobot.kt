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
package ch.protonmail.android.uitest.robot.menu

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import ch.protonmail.android.mailmailbox.presentation.sidebar.TEST_TAG_SIDEBAR_MENU
import ch.protonmail.android.uitest.models.folders.SidebarCustomItemEntry
import ch.protonmail.android.uitest.models.folders.SidebarItemCustomFolderEntryModel
import ch.protonmail.android.uitest.robot.mailbox.allmail.AllMailRobot
import ch.protonmail.android.uitest.robot.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.settings.SettingsRobot
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.presentation.compose.R.string
import ch.protonmail.android.maillabel.presentation.R.string as mailLabelStrings

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
internal class MenuRobot(private val composeTestRule: ComposeContentTestRule) {

    fun openInbox(): InboxRobot = InboxRobot(composeTestRule)

    fun openDrafts(): DraftsRobot {
        tapSidebarMenuItemWithText(mailLabelStrings.label_title_drafts)
        return DraftsRobot()
    }

    fun openSent(): DraftsRobot {
        tapSidebarMenuItemWithText(mailLabelStrings.label_title_sent)
        return DraftsRobot()
    }

    fun openAllMail(): AllMailRobot {
        tapSidebarMenuItemWithText(mailLabelStrings.label_title_all_mail)
        return AllMailRobot(composeTestRule)
    }

    fun openSettings(): SettingsRobot {
        tapSidebarMenuItemWithText(string.presentation_menu_item_title_settings)
        return SettingsRobot(composeTestRule)
    }

    fun openReportBugs() {
        tapSidebarMenuItemWithText(string.presentation_menu_item_title_report_a_bug)
    }

    internal inline fun verify(block: Verify.() -> Unit): MenuRobot = also { Verify().apply(block) }

    private fun tapSidebarMenuItemWithText(@StringRes menuItemName: Int) {
        composeTestRule
            .onNodeWithTag(TEST_TAG_SIDEBAR_MENU)
            .onChild()
            .performScrollToNode(hasText(menuItemName))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(menuItemName)
            .performClick()

        composeTestRule.waitForIdle()
    }

    fun swipeOpenSidebarMenu(): MenuRobot {
        composeTestRule
            .onRoot()
            .performTouchInput { swipeRight() }

        return this
    }

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    internal class Verify {

        fun customFoldersAreDisplayed(vararg folders: SidebarCustomItemEntry) {
            folders.forEach {
                val item = SidebarItemCustomFolderEntryModel(it.index)

                item.hasText(it.name)
                    .withIconTint(it.iconTint)
            }
        }
    }
}
