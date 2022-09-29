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

package ch.protonmail.android.uitest.robot.mailbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.action.ViewActions.swipeDown
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.uitest.util.awaitDisplayed

class MailboxRobot internal constructor(
    private val composeTestRule: ComposeContentTestRule
) {

    fun pullDownToRefresh(): MailboxRobot {
        composeTestRule.onList()
            .performTouchInput { swipeDown() }
        return this
    }

    fun scrollToItem(index: Int): MailboxRobot {
        composeTestRule.onList()
            .performScrollToIndex(index)
        return this
    }

    fun scrollToItem(subject: String): MailboxRobot {
        composeTestRule.onList()
            .performScrollToNode(hasText(subject))
        return this
    }

    fun verify(block: Verify.() -> Unit): MailboxRobot =
        this.also { Verify(composeTestRule).apply(block) }

    class Verify internal constructor(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun emptyMailboxIsDisplayed() {
            composeTestRule.onEmptyMailbox()
                .awaitDisplayed(composeTestRule)
                .assertIsDisplayed()
        }

        fun itemWithSubjectIsDisplayed(subject: String) {
            composeTestRule.onNodeWithText(subject)
                .awaitDisplayed(composeTestRule)
                .assertIsDisplayed()
        }

        fun listProgressIsDisplayed() {
            composeTestRule
                .onNodeWithTag(MailboxScreen.ListProgressTestTag)
                .assertIsDisplayed()
        }

        fun swipeRefreshProgressIsDisplayed() {
            composeTestRule
                // TODO
        }
    }
}

private fun ComposeContentTestRule.onList(): SemanticsNodeInteraction =
    onNode(hasScrollAction())

private fun ComposeContentTestRule.onEmptyMailbox(): SemanticsNodeInteraction =
    onNode(emptyMailboxMatcher())

private fun emptyMailboxMatcher(): SemanticsMatcher =
    hasTestTag(MailboxScreen.MailboxEmptyTestTag)

fun ComposeContentTestRule.MailboxRobot(content: @Composable () -> Unit) =
    MailboxRobot(this).also { setContent(content) }
