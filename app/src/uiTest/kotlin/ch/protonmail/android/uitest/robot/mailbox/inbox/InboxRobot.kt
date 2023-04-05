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

package ch.protonmail.android.uitest.robot.mailbox.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.maillabel.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenTestTags
import ch.protonmail.android.mailmailbox.presentation.mailbox.UnreadItemsFilterTestTags
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntry
import ch.protonmail.android.uitest.models.mailbox.MailboxListItemEntryModel
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.SelectionStateRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitest.util.onAllNodesWithText
import me.proton.core.test.android.robots.CoreRobot
import me.proton.core.test.android.robots.CoreVerify

@Suppress("unused", "MemberVisibilityCanBePrivate", "ExpressionBodySyntax")
class InboxRobot(
    override val composeTestRule: ComposeContentTestRule
) : CoreRobot(), MailboxRobotInterface {

    override fun clickMessageByPosition(position: Int): MessageRobot {
        val model = MailboxListItemEntryModel(position)

        model.click()

        return super.clickMessageByPosition(position)
    }

    override fun swipeLeftMessageAtPosition(position: Int): InboxRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot(composeTestRule)
    }

    override fun deleteMessageWithSwipe(position: Int): InboxRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    override fun refreshMessageList(): InboxRobot {
        super.refreshMessageList()
        return this
    }

    fun filterUnreadMessages(): InboxRobot {
        composeTestRule
            .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)
            .performClick()

        return this
    }

    class SelectionStateRobot(
        private val composeRule: ComposeContentTestRule
    ) : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot(composeRule)
        }

        override fun selectMessage(position: Int): SelectionStateRobot {
            super.selectMessage(position)
            return this
        }

        override fun addLabel(): InboxRobot {
            super.addLabel()
            return InboxRobot(composeRule)
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot(composeRule)
        }

        fun moveToTrash(): InboxRobot {
            return InboxRobot(composeRule)
        }
    }

    class MoveToFolderRobot(
        private val composeRule: ComposeContentTestRule
    ) : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot(composeRule)
        }
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    inner class Verify(private val composeRule: ComposeContentTestRule) : CoreVerify() {

        fun mailboxScreenDisplayed() {
            composeRule.waitUntil(timeoutMillis = 60_000) {
                composeRule.onAllNodesWithText(R.string.label_title_inbox)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }

            composeRule.onNodeWithTag(MailboxScreenTestTags.Root).assertIsDisplayed()
        }

        fun unreadFilterIsDisplayed() {
            composeRule
                .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)
                .assertIsDisplayed()
                .assertIsNotSelected()
        }

        fun unreadFilterIsSelected() {
            composeRule
                .onNodeWithTag(UnreadItemsFilterTestTags.UnreadFilterChip)
                .assertIsDisplayed()
                .assertIsSelected()
        }

        fun listItemsAreShown(vararg inboxEntries: MailboxListItemEntry) {
            for (entry in inboxEntries) {
                val model = MailboxListItemEntryModel(entry.index)

                model.hasAvatarText(entry.avatarText)
                    .hasParticipants(entry.participants)
                    .hasSubject(entry.subject)
                    .hasDate(entry.date)

                entry.count?.let { model.hasCount(it) } ?: model.hasNoCount()
            }
        }

        fun unreadItemAtPosition(position: Int) {
            val model = MailboxListItemEntryModel(position)

            model.assertUnread()
        }

        fun readItemAtPosition(position: Int) {
            val model = MailboxListItemEntryModel(position)

            model.assertRead()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify(composeTestRule).apply(block)
}
