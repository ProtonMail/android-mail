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

package ch.protonmail.android.uitest.robot.detail.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreenTestTags
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onNodeWithText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class ConversationDetailRobot(val composeTestRule: ComposeContentTestRule) {

    fun waitUntilMessageIsShown(timeout: Duration = 30.seconds): ConversationDetailRobot {
        composeTestRule.waitForIdle()

        // Wait for the WebView to appear.
        composeTestRule.onNodeWithTag(MessageBodyTestTags.WebView).awaitDisplayed(composeTestRule, timeout)

        return this
    }

    fun verify(block: Verify.() -> Unit) = Verify(composeTestRule).apply(block)

    internal class Verify(private val composeTestRule: ComposeContentTestRule) {

        fun conversationDetailScreenIsShown() {
            composeTestRule.onNodeWithTag(ConversationDetailScreenTestTags.RootItem)
                .awaitDisplayed(composeTestRule)
                .assertExists()
        }

        fun errorMessageIsDisplayed(message: TextUiModel) {
            composeTestRule.onNodeWithText(message)
                .assertIsDisplayed()
        }
    }
}

internal fun ComposeContentTestRule.ConversationDetailRobot(content: @Composable () -> Unit): ConversationDetailRobot {
    setContent(content)
    return ConversationDetailRobot(this)
}
