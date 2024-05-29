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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarSystemLabelTestTags.BaseTag
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxTopAppBarTestTags
import ch.protonmail.android.mailsidebar.presentation.SidebarMenuTestTags
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.folders.SidebarCustomItemEntry
import ch.protonmail.android.uitest.models.folders.SidebarItemCustomFolderEntryModel
import ch.protonmail.android.uitest.robot.ComposeRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.robot.settings.SettingsRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString
import me.proton.core.label.domain.entity.LabelId
import ch.protonmail.android.test.R as testR

@AsDsl
internal class MenuRobot : ComposeRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(SidebarMenuTestTags.Root)
    private val hamburgerMenuButton = composeTestRule.onNodeWithTag(MailboxTopAppBarTestTags.NavigationButton)

    fun openSidebarMenu(): MenuRobot = apply {
        hamburgerMenuButton.awaitDisplayed().performClick()
        rootItem.awaitDisplayed()
    }

    fun openInbox() = openMailbox(SystemLabelId.Inbox)

    fun openDrafts() = openMailbox(SystemLabelId.Drafts)

    fun openArchive() = openMailbox(SystemLabelId.Archive)

    fun openSent() = openMailbox(SystemLabelId.Sent)

    fun openSpam() = openMailbox(SystemLabelId.Spam)

    fun openTrash() = openMailbox(SystemLabelId.Trash)

    fun openAllMail() = openMailbox(SystemLabelId.AllMail)

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

    fun openSubscription() {
        tapSidebarMenuItemWithText(getTestString(testR.string.test_subscription))
    }

    fun tapSignOut() {
        tapSidebarMenuItemWithText(getTestString(testR.string.test_signout))
    }

    private fun tapSidebarMenuItemWithText(value: String) {
        rootItem.onChild()
            .apply { performScrollToNode(hasText(value)) }
            .child { hasText(value) }
            .performClick()

        composeTestRule.waitForIdle()
    }

    private fun openMailbox(id: SystemLabelId): MailboxRobot {
        composeTestRule
            .onNodeWithTag(id.labelId.testTag)
            .performScrollTo()
            .performClick()

        rootItem.awaitHidden()
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

private val LabelId.testTag: String
    get() = "$BaseTag#${this.id}"
