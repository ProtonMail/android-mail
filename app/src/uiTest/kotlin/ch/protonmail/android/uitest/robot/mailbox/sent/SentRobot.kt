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
package ch.protonmail.android.uitest.robot.mailbox.sent

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import ch.protonmail.android.uitest.robot.mailbox.ApplyLabelRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.SelectionStateRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot

/**
 * [SentRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Sent mailbox functionality.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "ExpressionBodySyntax")
class SentRobot : MailboxRobotInterface {

    override fun swipeLeftMessageAtPosition(position: Int): SentRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot(composeTestRule)
    }

    override fun deleteMessageWithSwipe(position: Int): SentRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    override fun refreshMessageList(): SentRobot {
        super.refreshMessageList()
        return SentRobot()
    }

    fun navigateUpToSent(): SentRobot {
        return SentRobot()
    }

    /**
     * Handles Mailbox selection state actions and verifications after user long click one of the messages.
     */
    class SelectionStateRobot(
        private val composeTestRule: ComposeContentTestRule
    ) : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot(composeTestRule)
        }

        override fun selectMessage(position: Int): SelectionStateRobot {
            super.selectMessage(position)
            return this
        }

        override fun addLabel(): ApplyLabelRobot {
            super.addLabel()
            return ApplyLabelRobot()
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot(composeTestRule)
        }

        fun moveToTrash(): SentRobot {
            return SentRobot()
        }
    }

    /**
     * Handles Move to folder dialog actions.
     */
    class MoveToFolderRobot(
        private val composeTestRule: ComposeContentTestRule
    ) : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot(composeTestRule)
        }
    }

    /**
     * Handles Move to folder dialog actions.
     */
    class ApplyLabelRobot : ApplyLabelRobotInterface {

        override fun selectLabelByName(name: String): ApplyLabelRobot {
            super.selectLabelByName(name)
            return ApplyLabelRobot()
        }

        override fun apply(): SentRobot {
            super.apply()
            return SentRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [SentRobot].
     */
    class Verify : MailboxRobotInterface.verify() {

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageStarred(subject: String) {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
