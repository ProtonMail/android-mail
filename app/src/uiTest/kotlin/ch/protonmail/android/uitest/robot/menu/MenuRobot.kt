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
package ch.protonmail.android.uitest.robot.menu

import androidx.annotation.IdRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import ch.protonmail.android.sidebar.TEST_TAG_SIDEBAR_MENU
import ch.protonmail.android.uitest.robot.contacts.ContactsRobot
import ch.protonmail.android.uitest.robot.mailbox.archive.ArchiveRobot
import ch.protonmail.android.uitest.robot.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.labelfolder.LabelFolderRobot
import ch.protonmail.android.uitest.robot.mailbox.sent.SentRobot
import ch.protonmail.android.uitest.robot.mailbox.trash.TrashRobot
import ch.protonmail.android.uitest.robot.manageaccounts.AccountPanelRobot
import ch.protonmail.android.uitest.robot.reportbugs.ReportBugsRobot
import ch.protonmail.android.uitest.robot.settings.SettingsRobot

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class MenuRobot(
    private val composeTestRule: ComposeContentTestRule? = null
) {

    fun archive(): ArchiveRobot {
        return ArchiveRobot()
    }

    fun settings(): SettingsRobot {
        composeTestRule!!
            .onRoot()
            .performTouchInput { swipeRight() }

        composeTestRule
            .onNodeWithTag(TEST_TAG_SIDEBAR_MENU)
            .onChild()
            .performScrollToNode(hasText("Settings"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Settings")
            .performClick()

        composeTestRule.waitForIdle()

        return SettingsRobot(composeTestRule)
    }

    fun drafts(): DraftsRobot {
        return DraftsRobot()
    }

    fun inbox(): InboxRobot {
        return InboxRobot()
    }

    fun sent(): SentRobot {
        return SentRobot()
    }

    fun contacts(): ContactsRobot {
        return ContactsRobot()
    }

    fun reportBugs(): ReportBugsRobot {
        return ReportBugsRobot()
    }

    fun logout(): MenuRobot {
        return this
    }

    fun trash(): TrashRobot {
        return TrashRobot()
    }

    fun closeMenuWithSwipe(): MenuRobot {
        return this
    }

    fun labelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }

    fun accountsList(): AccountPanelRobot {
        return AccountPanelRobot()
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectMenuItem(@IdRes menuItemName: String) {}

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) {}

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun menuOpened() {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun menuClosed() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
