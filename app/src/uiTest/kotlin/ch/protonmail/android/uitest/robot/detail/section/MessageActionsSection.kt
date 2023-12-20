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

package ch.protonmail.android.uitest.robot.detail.section

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ch.protonmail.android.maildetail.presentation.ui.MessageBodyTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.util.child

@AttachTo(targets = [ConversationDetailRobot::class, MessageDetailRobot::class], identifier = "actionsSection")
internal class MessageActionsSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(MessageBodyTestTags.MessageActionsRootItem)

    private val replyButton = rootItem.child {
        hasTestTag(MessageBodyTestTags.MessageReplyButton)
    }

    private val replyAllButton = rootItem.child {
        hasTestTag(MessageBodyTestTags.MessageReplyAllButton)
    }

    private val forwardButton = rootItem.child {
        hasTestTag(MessageBodyTestTags.MessageForwardButton)
    }

    fun tapReplyButton() {
        replyButton.performScrollTo().performClick()
    }
}
