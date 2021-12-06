/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.uitests.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailmailbox.presentation.TEST_TAG_MAILBOX_SCREEN
import me.proton.core.test.android.robots.CoreRobot
import me.proton.core.test.android.robots.CoreVerify

class InboxRobot : CoreRobot() {

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
