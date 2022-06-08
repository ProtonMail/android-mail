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

package ch.protonmail.android.uitest.robot.mailbox.allmail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onFirst
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitest.util.onAllNodesWithText

class AllMailRobot(
    override val composeTestRule: ComposeContentTestRule
) : MailboxRobotInterface {

    inner class Verify : MailboxRobotInterface.verify() {

        fun allMailScreenDisplayed(composeRule: ComposeContentTestRule) {
            composeRule
                .onAllNodesWithText(R.string.label_title_all_mail)
                .onFirst() // Both "TopBar" and "sidebar" are found as match of "All Mail". Only TopBar is displayed.
                .assertIsDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
