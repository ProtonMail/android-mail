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

package ch.protonmail.android.uitest.robot.detail.section.conversation

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.conversation.MessageCollapsedItemEntryModel

@AttachTo(targets = [ConversationDetailRobot::class], identifier = "messagesCollapsedSection")
internal class ConversationDetailCollapsedMessagesSection : ComposeSectionRobot() {

    private val messagesList = composeTestRule.onNodeWithTag(ConversationDetailScreenTestTags.MessagesList)

    fun scrollToTop() {
        messagesList.performTouchInput { swipeUp() }
    }

    fun openMessageAtIndex(index: Int) = withEntryModel(index) {
        click()
    }

    @VerifiesOuter
    inner class Verify {

        fun collapsedHeaderIsNotDisplayed() = withEntryModel(index = 0) {
            isNotDisplayed()
        }

        fun avatarInitialIsDisplayed(index: Int, text: String) = withEntryModel(index) {
            hasAvatar(AvatarInitial.WithText(text))
        }

        fun avatarDraftIsDisplayed(index: Int) = withEntryModel(index) {
            hasAvatar(AvatarInitial.Draft)
        }

        fun attachmentIconIsDisplayed(index: Int) = withEntryModel(index) {
            hasAttachmentIcon()
        }

        fun forwardedIconIsDisplayed(index: Int) = withEntryModel(index) {
            hasForwardedIcon()
        }

        fun repliedAllIconIsDisplayed(index: Int) = withEntryModel(index) {
            hasRepliedAllIcon()
        }

        fun repliedIconIsDisplayed(index: Int) = withEntryModel(index) {
            hasRepliedIcon()
        }

        fun senderNameIsDisplayed(index: Int, value: String) = withEntryModel(index) {
            hasSender(value)
        }

        fun authenticityBadgeIsDisplayed(index: Int, value: Boolean) = withEntryModel(index) {
            hasAuthenticityBadge(value)
        }

        fun expirationIsDisplayed(index: Int, value: String) = withEntryModel(index) {
            hasExpiration(value)
        }

        fun starIconIsDisplayed(index: Int) = withEntryModel(index) {
            hasStarIcon()
        }

        fun timeIsDisplayed(index: Int, value: String) = withEntryModel(index) {
            hasTime(value)
        }
    }

    private fun withEntryModel(index: Int, block: MessageCollapsedItemEntryModel.() -> MessageCollapsedItemEntryModel) {
        val entryModel = MessageCollapsedItemEntryModel(index, composeTestRule)
        block(entryModel)
    }
}
