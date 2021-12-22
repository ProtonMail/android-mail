/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitest.robot.mailbox.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailmailbox.presentation.TEST_TAG_MAILBOX_SCREEN
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.SelectionStateRobotInterface
import me.proton.core.test.android.robots.CoreRobot
import me.proton.core.test.android.robots.CoreVerify

@Suppress("unused", "MemberVisibilityCanBePrivate", "ExpressionBodySyntax")
class InboxRobot : CoreRobot(), MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(position: Int): InboxRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot()
    }

    override fun deleteMessageWithSwipe(position: Int): InboxRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    override fun refreshMessageList(): InboxRobot {
        super.refreshMessageList()
        return InboxRobot()
    }

    class SelectionStateRobot : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot()
        }

        override fun selectMessage(position: Int): SelectionStateRobot {
            super.selectMessage(position)
            return this
        }

        override fun addLabel(): InboxRobot {
            super.addLabel()
            return InboxRobot()
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot()
        }

        fun moveToTrash(): InboxRobot {
            return InboxRobot()
        }
    }

    class MoveToFolderRobot : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    inner class Verify : CoreVerify() {

        fun mailboxScreenDisplayed(
            composeRule: ComposeContentTestRule
        ) {
            composeRule.waitUntil(timeoutMillis = 60_000) {
                composeRule.onAllNodesWithTag(TEST_TAG_MAILBOX_SCREEN)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }

            composeRule.onNodeWithTag(TEST_TAG_MAILBOX_SCREEN).assertIsDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
