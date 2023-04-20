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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeader
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreenTestTags
import ch.protonmail.android.maildetail.presentation.ui.LabelAsBottomSheetTestTags
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailHeaderTestTags
import ch.protonmail.android.maildetail.presentation.ui.MoveToBottomSheetTestTags
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.onAllNodesWithText
import ch.protonmail.android.uitest.util.onNodeWithContentDescription
import ch.protonmail.android.uitest.util.onNodeWithText
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ConversationDetailRobot(private val composeTestRule: ComposeContentTestRule) {

    fun expandHeader(): ConversationDetailRobot {
        composeTestRule.onNodeWithTag(MessageDetailHeaderTestTags.RootItem)
            .performTouchInput { click(Offset.Zero) }
        return this
    }

    fun markAsUnread(): ConversationDetailRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_mark_unread_content_description)
            .performClick()
        return this
    }

    fun moveToTrash(): MailboxRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_trash_content_description)
            .performClick()

        return MailboxRobot(composeTestRule)
    }

    fun openMoveToBottomSheet(): ConversationDetailRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_move_content_description)
            .performClick()
        return this
    }

    fun openLabelAsBottomSheet(): ConversationDetailRobot {
        composeTestRule.onNodeWithContentDescription(R.string.action_label_content_description)
            .performClick()
        return this
    }

    fun waitUntilMessageIsShown(timeout: Duration = 30.seconds): ConversationDetailRobot {
        composeTestRule.waitForIdle()

        // Wait for the WebView to appear.
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebView).awaitDisplayed(composeTestRule, timeout)

        return this
    }

    fun verify(block: Verify.() -> Unit): ConversationDetailRobot {
        Verify(composeTestRule).apply(block)
        return this
    }

    @Suppress("TooManyFunctions")
    class Verify(private val composeTestRule: ComposeContentTestRule) {

        fun conversationDetailScreenIsShown() {
            composeTestRule.onNodeWithTag(ConversationDetailScreenTestTags.RootItem)
                .awaitDisplayed(composeTestRule)
                .assertExists()
        }

        fun attachmentIconIsDisplayed() {
            composeTestRule.onAllNodesWithTag(ConversationDetailCollapsedMessageHeader.AttachmentIconTestTag)
                .onFirst()
                .assertIsDisplayed()
        }

        fun draftIconAvatarIsDisplayed(useUnmergedTree: Boolean = false) {
            composeTestRule
                .onNodeWithTag(
                    testTag = AvatarTestTags.AvatarDraft,
                    useUnmergedTree = useUnmergedTree
                )
                .assertIsDisplayed()
        }

        fun errorMessageIsDisplayed(message: TextUiModel) {
            composeTestRule.onNodeWithText(message)
                .assertIsDisplayed()
        }

        fun expirationIsDisplayed(expiration: String) {
            composeTestRule.onNodeWithText(expiration)
                .assertIsDisplayed()
        }

        fun forwardedIconIsDisplayed(useUnmergedTree: Boolean = false) {
            composeTestRule
                .onNodeWithTag(
                    testTag = ConversationDetailCollapsedMessageHeader.ForwardedIconTestTag,
                    useUnmergedTree = useUnmergedTree
                )
                .assertIsDisplayed()
        }

        fun repliedAllIconIsDisplayed(useUnmergedTree: Boolean = false) {
            composeTestRule
                .onNodeWithTag(
                    testTag = ConversationDetailCollapsedMessageHeader.RepliedAllIconTestTag,
                    useUnmergedTree = useUnmergedTree
                )
                .assertIsDisplayed()
        }

        fun repliedIconIsDisplayed(useUnmergedTree: Boolean = false) {
            composeTestRule
                .onNodeWithTag(
                    testTag = ConversationDetailCollapsedMessageHeader.RepliedIconTestTag,
                    useUnmergedTree = useUnmergedTree
                )
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

        fun starIconIsDisplayed(useUnmergedTree: Boolean = false) {
            composeTestRule
                .onAllNodesWithTag(
                    testTag = ConversationDetailCollapsedMessageHeader.StarIconTestTag,
                    useUnmergedTree = useUnmergedTree
                )
                .onFirst()
                .assertIsDisplayed()
        }

        fun timeIsDisplayed(time: TextUiModel) {
            composeTestRule.onAllNodesWithText(time)
                .onFirst()
                .assertIsDisplayed()
        }

        fun messageBodyIsDisplayedInWebView(messageBody: String, tagName: String = "html") {
            Web.onWebView(ViewMatchers.withClassName(Matchers.equalTo("android.webkit.WebView")))
                .forceJavascriptEnabled()
                .withElement(DriverAtoms.findElement(Locator.TAG_NAME, tagName))
                .check(WebViewAssertions.webMatches(DriverAtoms.getText(), CoreMatchers.containsString(messageBody)))
        }

        fun messageHeaderIsDisplayed() {
            composeTestRule.onNodeWithTag(MessageDetailHeaderTestTags.RootItem)
                .assertIsDisplayed()
        }

        fun collapsedHeaderDoesNotExist() {
            composeTestRule.onNodeWithTag(ConversationDetailCollapsedMessageHeader.CollapsedHeaderTestTag)
                .assertDoesNotExist()
        }

        fun moveToBottomSheetExists() {
            composeTestRule.onNodeWithTag(MoveToBottomSheetTestTags.RootItem, useUnmergedTree = true)
                .awaitDisplayed(composeTestRule, timeout = 5.seconds)
                .assertExists()
        }

        fun labelAsBottomSheetExists() {
            composeTestRule.onNodeWithTag(LabelAsBottomSheetTestTags.RootItem, useUnmergedTree = true)
                .awaitDisplayed(composeTestRule, timeout = 5.seconds)
                .assertExists()
        }

        fun moveToBottomSheetIsDismissed() {
            composeTestRule.onNodeWithTag(MoveToBottomSheetTestTags.RootItem, useUnmergedTree = true)
                .awaitHidden(composeTestRule, timeout = 5.seconds)
                .assertDoesNotExist()
        }

        fun labelAsBottomSheetIsDismissed() {
            composeTestRule.onNodeWithTag(LabelAsBottomSheetTestTags.RootItem, useUnmergedTree = true)
                .awaitHidden(composeTestRule, timeout = 5.seconds)
                .assertDoesNotExist()
        }
    }
}

fun ComposeContentTestRule.ConversationDetailRobot(content: @Composable () -> Unit): ConversationDetailRobot {
    setContent(content)
    return ConversationDetailRobot(this)
}
