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

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import ch.protonmail.android.mailmailbox.presentation.sidebar.SidebarMenuTestTags
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.folders.SidebarCustomItemEntry
import ch.protonmail.android.uitest.models.folders.SidebarItemCustomFolderEntryModel
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.robot.settings.SettingsRobot
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AsDsl
internal class MenuRobot : ComposeRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(SidebarMenuTestTags.Root)

    fun swipeOpenSidebarMenu(): MenuRobot = apply {
        composeTestRule
            .onRoot()
            .performTouchInput { swipeRight() }
    }

    fun openInbox() = openMailbox(getTestString(testR.string.test_label_title_inbox))

    fun openDrafts() = openMailbox(getTestString(testR.string.test_label_title_drafts))

    fun openArchive() = openMailbox(getTestString(testR.string.test_label_title_archive))

    fun openSent() = openMailbox(getTestString(testR.string.test_label_title_sent))

    fun openSpam() = openMailbox(getTestString(testR.string.test_label_title_spam))

    fun openTrash() = openMailbox(getTestString(testR.string.test_label_title_trash))

    fun openAllMail() = openMailbox(getTestString(testR.string.test_label_title_all_mail))

    fun openSettings(): SettingsRobot {
        tapSidebarMenuItemWithText(getTestString(testR.string.test_mail_settings_settings))
        return SettingsRobot()
    }

    fun openFolderWithName(folderName: String) {
        tapSidebarMenuItemWithText(folderName)
    }

    fun openReportBugs() {
        tapSidebarMenuItemWithText(getTestString(testR.string.test_report_a_problem))
    }

    private fun tapSidebarMenuItemWithText(value: String) {
        rootItem.onChild()
            .apply { performScrollToNode(hasText(value)) }
            .child { hasText(value) }
            .performClick()

        composeTestRule.waitForIdle()
    }

    private fun openMailbox(sidebarTextMenu: String): MailboxRobot {
        tapSidebarMenuItemWithText(sidebarTextMenu)
        return MailboxRobot()
    }

    @VerifiesOuter
    inner class Verify {

        fun customFoldersAreDisplayed(vararg folders: SidebarCustomItemEntry) {
            folders.forEach {
                val item = SidebarItemCustomFolderEntryModel(it.index)

                item.hasText(it.name)
                    .withIconTint(it.iconTint)
            }
        }
    }
}
