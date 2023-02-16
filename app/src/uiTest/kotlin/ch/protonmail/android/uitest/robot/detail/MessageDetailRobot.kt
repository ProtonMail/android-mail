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

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import ch.protonmail.android.mailcommon.presentation.compose.TEST_TAG_AVATAR
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.ui.TEST_TAG_MESSAGE_BODY_WEB_VIEW
import ch.protonmail.android.maildetail.presentation.ui.TEST_TAG_MESSAGE_HEADER
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onNodeWithContentDescription
import ch.protonmail.android.uitest.util.onNodeWithText

class MessageDetailRobot(private val composeTestRule: ComposeContentTestRule) {

    fun expandHeader(): MessageDetailRobot {
        composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_HEADER)
            .performTouchInput { click(Offset.Zero) }
        return this
    }

    fun markAsUnread(): MessageDetailRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_mark_unread_content_description)
            .performClick()
        return this
    }

    fun moveToTrash(): MailboxRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_trash_content_description)
            .performClick()

        return MailboxRobot(composeTestRule)
    }

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
            composeTestRule.onNodeWithText(recipients.first().participantAddress)
                .assertIsDisplayed()
        }

        fun extendedTimeIsDisplayed(extendedTime: TextUiModel.Text) {
            composeTestRule.onNodeWithText(extendedTime.value)
                .assertIsDisplayed()
        }

        fun locationNameIsDisplayed(locationName: String) {
            composeTestRule.onNodeWithText(locationName)
                .assertIsDisplayed()
        }

        fun sizeIsDisplayed(size: String) {
            composeTestRule.onNodeWithText(size)
                .assertIsDisplayed()
        }

        fun labelIsDisplayed(name: String) {
            composeTestRule.onNodeWithText(name)
                .assertIsDisplayed()
        }

        fun messageBodyWebViewIsDisplayed() {
            composeTestRule.onNodeWithTag(TEST_TAG_MESSAGE_BODY_WEB_VIEW)
                .awaitDisplayed(composeTestRule)
                .assertIsDisplayed()
        }

        fun messageBodyLoadingErrorMessageIsDisplayed(@StringRes errorMessage: Int) {
            composeTestRule.onNodeWithText(errorMessage)
                .assertIsDisplayed()
        }

        fun messageBodyReloadButtonIsDisplayed() {
            composeTestRule.onNodeWithText(R.string.reload)
                .assertIsDisplayed()
        }

        fun messageBodyDecryptionErrorMessageIsDisplayed() {
            composeTestRule.onNodeWithText(R.string.decryption_error)
                .assertIsDisplayed()
        }
    }
}

fun ComposeContentTestRule.MessageDetailRobot(content: @Composable () -> Unit): MessageDetailRobot {
    setContent(content)
    return MessageDetailRobot(this)
}
