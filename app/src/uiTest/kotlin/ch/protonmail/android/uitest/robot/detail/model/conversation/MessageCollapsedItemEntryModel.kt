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
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadgeTestTags
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailCollapsedMessageHeaderTestTags
import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString

@Suppress("TooManyFunctions")
internal data class MessageCollapsedItemEntryModel(
    private val index: Int,
    private val composeTestRule: ComposeTestRule
) {

    private val rootItem = composeTestRule.onAllNodesWithTag(
        testTag = ConversationDetailCollapsedMessageHeaderTestTags.RootItem,
        useUnmergedTree = true
    )[index]

    private val avatarRootItem = rootItem.child {
        hasTestTag(AvatarTestTags.AvatarRootItem)
    }

    private val avatar = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarText)
    }

    private val avatarDraft = avatarRootItem.child {
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

    private val authenticityBadge = rootItem.child {
        hasTestTag(OfficialBadgeTestTags.Item)
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

    // region actions
    fun click(): MessageCollapsedItemEntryModel = apply {
        rootItem.performClick()
    }
    // endregion

    // region verification
    fun isNotDisplayed(): MessageCollapsedItemEntryModel = apply {
        rootItem.assertDoesNotExist()
    }

    fun hasAvatar(initial: AvatarInitial): MessageCollapsedItemEntryModel = apply {
        when (initial) {
            is AvatarInitial.WithText -> avatar.assertTextEquals(initial.text)
            is AvatarInitial.Draft -> avatarDraft.assertIsDisplayed()
        }
    }

    fun hasAttachmentIcon(): MessageCollapsedItemEntryModel = apply {
        attachmentIcon.assertIsDisplayed()
    }

    fun hasRepliedIcon(): MessageCollapsedItemEntryModel = apply {
        repliedIcon.assertIsDisplayed()
    }

    fun hasRepliedAllIcon(): MessageCollapsedItemEntryModel = apply {
        repliedAllIcon.assertIsDisplayed()
    }

    fun hasForwardedIcon(): MessageCollapsedItemEntryModel = apply {
        forwardedIcon.assertIsDisplayed()
    }

    fun hasStarIcon(): MessageCollapsedItemEntryModel = apply {
        starIcon.assertIsDisplayed()
    }

    fun hasSender(value: String): MessageCollapsedItemEntryModel = apply {
        sender.assertTextEquals(value)
    }

    fun hasAuthenticityBadge(expectedValue: Boolean): MessageCollapsedItemEntryModel = apply {
        if (expectedValue) {
            authenticityBadge.assertIsDisplayed()
            authenticityBadge.assertTextEquals(getTestString(R.string.test_auth_badge_official))
        } else {
            authenticityBadge.assertDoesNotExist()
        }
    }

    fun hasExpiration(value: String): MessageCollapsedItemEntryModel = apply {
        expirationIcon.assertIsDisplayed()
        expirationText.assertTextEquals(value)
    }

    fun hasLocationIcon(): MessageCollapsedItemEntryModel = apply {
        locationIcon.assertIsDisplayed()
    }

    fun hasTime(value: String): MessageCollapsedItemEntryModel = apply {
        time.assertTextEquals(value)
    }
    // endregion
}
