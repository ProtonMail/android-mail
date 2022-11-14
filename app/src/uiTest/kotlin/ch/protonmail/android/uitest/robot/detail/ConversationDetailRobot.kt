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

package ch.protonmail.android.uitest.robot.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen

class ConversationDetailRobot(private val composeTestRule: ComposeContentTestRule) {

    fun verify(block: Verify.() -> Unit): ConversationDetailRobot {
        Verify(composeTestRule).apply(block)
        return this
    }

    class Verify(private val composeTestRule: ComposeContentTestRule) {

        fun draftIconAvatarIsDisplayed() {
            composeTestRule.onNodeWithTag(Avatar.DraftTestTag)
                .assertIsDisplayed()
        }

        fun expirationIsDisplayed(expiration: String) {
            composeTestRule.onNodeWithText(expiration)
                .assertIsDisplayed()
        }

        fun forwardedIconIsDisplayed() {
            composeTestRule
                .onNodeWithTag(ConversationDetailScreen.ForwardedMessageTestTag)
                .assertIsDisplayed()
        }

        fun repliedAllIconIsDisplayed() {
            composeTestRule
                .onNodeWithTag(ConversationDetailScreen.RepliedAllMessageTestTag)
                .assertIsDisplayed()
        }

        fun repliedIconIsDisplayed() {
            composeTestRule
                .onNodeWithTag(ConversationDetailScreen.RepliedMessageTestTag)
                .assertIsDisplayed()
        }

        fun senderInitialIsDisplayed(initial: String) {
            composeTestRule.onAllNodesWithText(initial, substring = false)
                .onFirst()
                .assertIsDisplayed()
        }

        fun senderIsDisplayed(sender: String) {
            composeTestRule.onAllNodesWithText(sender)
                .onFirst()
                .assertIsDisplayed()
        }

        fun subjectIsDisplayed(subject: String) {
            composeTestRule.onNodeWithText(subject)
                .assertIsDisplayed()
        }
    }
}

fun ComposeContentTestRule.ConversationDetailRobot(content: @Composable () -> Unit): ConversationDetailRobot {
    setContent(content)
    return ConversationDetailRobot(this)
}
