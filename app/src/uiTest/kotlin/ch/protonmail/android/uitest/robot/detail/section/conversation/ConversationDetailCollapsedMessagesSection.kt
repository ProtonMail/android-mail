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

import androidx.compose.ui.test.junit4.ComposeTestRule
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.robot.detail.model.conversation.MessageCollapsedItemEntryModel

internal class ConversationDetailCollapsedMessagesSection(
    private val composeTestRule: ComposeTestRule
) {

    internal fun verify(func: Verify.() -> Unit) = Verify().apply(func)

    @Suppress("TooManyFunctions")
    internal inner class Verify {

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

    private fun withEntryModel(
        index: Int,
        block: MessageCollapsedItemEntryModel.() -> MessageCollapsedItemEntryModel
    ): MessageCollapsedItemEntryModel {
        val entryModel = MessageCollapsedItemEntryModel(index, composeTestRule)
        return block(entryModel)
    }
}
