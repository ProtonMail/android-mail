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

package ch.protonmail.android.uitest.robot.detail.model.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeaderTestTags
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.util.child

@Suppress("TooManyFunctions")
internal data class MessageCollapsedItemEntryModel(
    private val index: Int,
    private val composeTestRule: ComposeTestRule
) {

    private val rootItem = composeTestRule.onAllNodesWithTag(
        testTag = ConversationDetailCollapsedMessageHeaderTestTags.RootItem,
        useUnmergedTree = true
    )[index]

    private val avatar = rootItem.child {
        hasTestTag(AvatarTestTags.Avatar)
    }

    private val avatarDraft = rootItem.child {
        hasTestTag(AvatarTestTags.AvatarDraft)
    }

    private val attachmentIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.AttachmentIcon)
    }

    private val repliedIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.RepliedIcon)
    }

    private val repliedAllIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.RepliedAllIcon)
    }

    private val forwardedIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.ForwardedIcon)
    }

    private val starIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.StarIcon)
    }

    private val sender = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.Sender)
    }

    private val expirationIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.ExpirationIcon)
    }

    private val expirationText = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.ExpirationText)
    }

    private val locationIcon = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.Location)
    }

    private val time = rootItem.child {
        hasTestTag(ConversationDetailCollapsedMessageHeaderTestTags.Time)
    }

    // region verification
    fun isNotDisplayed() = apply {
        rootItem.assertDoesNotExist()
    }

    fun hasAvatar(initial: AvatarInitial) = apply {
        when (initial) {
            is AvatarInitial.WithText -> avatar.assertTextEquals(initial.text)
            is AvatarInitial.Draft -> avatarDraft.assertIsDisplayed()
        }
    }

    fun hasAttachmentIcon() = apply {
        attachmentIcon.assertIsDisplayed()
    }

    fun hasRepliedIcon() = apply {
        repliedIcon.assertIsDisplayed()
    }

    fun hasRepliedAllIcon() = apply {
        repliedAllIcon.assertIsDisplayed()
    }

    fun hasForwardedIcon() = apply {
        forwardedIcon.assertIsDisplayed()
    }

    fun hasStarIcon() = apply {
        starIcon.assertIsDisplayed()
    }

    fun hasSender(value: String) = apply {
        sender.assertTextEquals(value)
    }

    fun hasExpiration(value: String) = apply {
        expirationIcon.assertIsDisplayed()
        expirationText.assertTextEquals(value)
    }

    fun hasLocationIcon() = apply {
        locationIcon.assertIsDisplayed()
    }

    fun hasTime(value: String) = apply {
        time.assertTextEquals(value)
    }
    // endregion
}
