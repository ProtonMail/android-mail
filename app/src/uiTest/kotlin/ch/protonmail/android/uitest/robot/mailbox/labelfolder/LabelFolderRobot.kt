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
package ch.protonmail.android.uitest.robot.mailbox.labelfolder

import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface

/**
 * [LabelFolderRobot] class implements [MailboxRobotInterface],
 * contains actions and verifications for Labels or Folders mailbox functionality.
 */
@Suppress("unused")
class LabelFolderRobot : MailboxRobotInterface {

    override fun refreshMessageList(): LabelFolderRobot {
        super.refreshMessageList()
        return this
    }

    /**
     * Contains all the validations that can be performed by [LabelFolderRobot].
     */
    open class Verify : MailboxRobotInterface.verify() {
        @SuppressWarnings("EmptyFunctionBlock")
        fun withMessageSubjectAndLocationExists(subject: String, location: String) {}
    }

    inline fun verify(block: Verify.() -> Unit): LabelFolderRobot {
        Verify().apply(block)
        return LabelFolderRobot()
    }
}
