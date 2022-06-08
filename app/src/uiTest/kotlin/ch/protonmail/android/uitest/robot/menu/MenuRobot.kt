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

import androidx.annotation.IdRes
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
import ch.protonmail.android.mailmailbox.presentation.TEST_TAG_SIDEBAR_MENU
import ch.protonmail.android.uitest.robot.contacts.ContactsRobot
import ch.protonmail.android.uitest.robot.mailbox.allmail.AllMailRobot
import ch.protonmail.android.uitest.robot.mailbox.archive.ArchiveRobot
import ch.protonmail.android.uitest.robot.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.labelfolder.LabelFolderRobot
import ch.protonmail.android.uitest.robot.mailbox.sent.SentRobot
import ch.protonmail.android.uitest.robot.mailbox.trash.TrashRobot
import ch.protonmail.android.uitest.robot.manageaccounts.AccountPanelRobot
import ch.protonmail.android.uitest.robot.reportbugs.ReportBugsRobot
import ch.protonmail.android.uitest.robot.settings.SettingsRobot
import ch.protonmail.android.uitest.util.hasText
import ch.protonmail.android.uitest.util.onNodeWithText
import me.proton.core.presentation.compose.R.string
import ch.protonmail.android.maillabel.presentation.R.string as mailLabelStrings

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
class MenuRobot(private val composeTestRule: ComposeContentTestRule) {

    @Suppress("unused", "ExpressionBodySyntax")
    fun closeMenuWithSwipe(): MenuRobot {
        return this
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openAccountsList(): AccountPanelRobot {
        return AccountPanelRobot(composeTestRule)
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openArchive(): ArchiveRobot {
        return ArchiveRobot()
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openContacts(): ContactsRobot {
        return ContactsRobot(composeTestRule)
    }

    fun openDrafts(): DraftsRobot {
        openSidebarItemWithText(mailLabelStrings.label_title_drafts)
        return DraftsRobot()
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openInbox(): InboxRobot {
        return InboxRobot(composeTestRule)
    }

    fun openAllMail(): AllMailRobot {
        openSidebarItemWithText(mailLabelStrings.label_title_all_mail)
        return AllMailRobot(composeTestRule)
    }

    @Suppress("unused")
    fun openLabelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openReportBugs(): ReportBugsRobot {
        return ReportBugsRobot()
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openSent(): SentRobot {
        return SentRobot()
    }

    fun openSettings(): SettingsRobot {
        openSidebarItemWithText(string.presentation_menu_item_title_settings)
        return SettingsRobot(composeTestRule)
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun openTrash(): TrashRobot {
        return TrashRobot()
    }

    @Suppress("unused", "ExpressionBodySyntax")
    fun selectLogout(): MenuRobot {
        return this
    }

    inline fun verify(block: Verify.() -> Unit): MenuRobot =
        also { Verify().apply(block) }

    @Suppress("unused", "EmptyFunctionBlock")
    private fun selectMenuItem(@IdRes menuItemName: String) {}

    @Suppress("unused", "EmptyFunctionBlock")
    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) {}

    private fun openSidebarItemWithText(@StringRes menuItemName: Int) {
        swipeOpenSidebarMenu()

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

    private fun swipeOpenSidebarMenu() {
        composeTestRule
            .onRoot()
            .performTouchInput { swipeRight() }
    }

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify {

        @Suppress("unused", "EmptyFunctionBlock")
        fun menuOpened() {}

        @Suppress("unused", "EmptyFunctionBlock")
        fun menuClosed() {}
    }
}
