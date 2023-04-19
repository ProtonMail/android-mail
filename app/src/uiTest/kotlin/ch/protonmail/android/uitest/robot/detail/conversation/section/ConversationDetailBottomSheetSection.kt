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

package ch.protonmail.android.uitest.robot.detail.conversation.section

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.ui.LabelAsBottomSheetTestTags
import ch.protonmail.android.maildetail.presentation.ui.MoveToBottomSheetTestTags
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.onNodeWithContentDescription

internal class ConversationDetailBottomSheetSection(private val composeTestRule: ComposeContentTestRule) {

    fun markAsUnread() = apply {
        composeTestRule.onNodeWithContentDescription(R.string.action_mark_unread_content_description).performClick()
    }

    fun moveToTrash() = apply {
        composeTestRule.onNodeWithContentDescription(R.string.action_trash_content_description).performClick()
    }

    fun openMoveToBottomSheet() = apply {
        composeTestRule.onNodeWithContentDescription(R.string.action_move_content_description).performClick()
    }

    fun openLabelAsBottomSheet() = apply {
        composeTestRule.onNodeWithContentDescription(R.string.action_label_content_description).performClick()
    }

    internal fun verify(func: ConversationDetailBottomSheetSection.Verify.() -> Unit) = Verify().apply(func)

    internal inner class Verify {

        fun moveToBottomSheetExists() {
            composeTestRule.onNodeWithTag(MoveToBottomSheetTestTags.RootItem)
                .assertExists()
        }

        fun labelAsBottomSheetExists() {
            composeTestRule.onNodeWithTag(LabelAsBottomSheetTestTags.RootItem)
                .assertExists()
        }

        fun moveToBottomSheetIsDismissed() {
            composeTestRule.onNodeWithTag(MoveToBottomSheetTestTags.RootItem)
                .awaitHidden(composeTestRule)
                .assertDoesNotExist()
        }

        fun labelAsBottomSheetIsDismissed() {
            composeTestRule.onNodeWithTag(LabelAsBottomSheetTestTags.RootItem)
                .awaitHidden(composeTestRule)
                .assertDoesNotExist()
        }
    }
}
