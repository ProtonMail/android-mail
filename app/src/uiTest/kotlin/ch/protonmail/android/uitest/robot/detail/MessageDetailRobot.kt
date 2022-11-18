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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.compose.TEST_TAG_AVATAR
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.ui.TEST_TAG_MESSAGE_HEADER

class MessageDetailRobot(private val composeTestRule: ComposeContentTestRule) {

    fun verify(block: Verify.() -> Unit): MessageDetailRobot {
        Verify(composeTestRule).apply(block)
        return this
    }

    class Verify(private val composeTestRule: ComposeContentTestRule) {

        fun subjectIsDisplayed(subject: String) {
            composeTestRule.onNodeWithText(subject)
                .assertIsDisplayed()
        }

        fun messageHeaderIsDisplayed() {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
                .assertIsDisplayed()
        }

        fun avatarIsDisplayed() {
            composeTestRule.onNodeWithTag(TEST_TAG_AVATAR, useUnmergedTree = true)
                .assertIsDisplayed()
        }

        fun senderNameIsDisplayed(senderName: String) {
            composeTestRule.onNodeWithText(senderName)
                .assertIsDisplayed()
        }

        fun senderAddressIsDisplayed(senderAddress: String) {
            composeTestRule.onNodeWithText(senderAddress)
                .assertIsDisplayed()
        }

        fun timeIsDisplayed(time: TextUiModel.Text) {
            composeTestRule.onNodeWithText(time.value)
                .assertIsDisplayed()
        }

        fun allRecipientsAreDisplayed(allRecipients: TextUiModel.Text) {
            composeTestRule.onNodeWithText(allRecipients.value)
                .assertIsDisplayed()
        }

        fun expandedRecipientsAreDisplayed(recipients: List<ParticipantUiModel>) {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
                .performClick()
            composeTestRule.onNodeWithText(recipients.first().participantAddress)
                .assertIsDisplayed()
        }

        fun extendedTimeIsDisplayed(extendedTime: TextUiModel.Text) {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
                .performClick()
            composeTestRule.onNodeWithText(extendedTime.value)
                .assertIsDisplayed()
        }

        fun locationNameIsDisplayed(locationName: String) {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
                .performClick()
            composeTestRule.onNodeWithText(locationName)
                .assertIsDisplayed()
        }

        fun sizeIsDisplayed(size: String) {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
                .performClick()
            composeTestRule.onNodeWithText(size)
                .assertIsDisplayed()
        }
    }
}

fun ComposeContentTestRule.MessageDetailRobot(content: @Composable () -> Unit): MessageDetailRobot {
    setContent(content)
    return MessageDetailRobot(this)
}
