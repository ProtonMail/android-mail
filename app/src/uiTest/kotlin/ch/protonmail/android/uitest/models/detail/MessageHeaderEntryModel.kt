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

package ch.protonmail.android.uitest.models.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadgeTestTags
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailItemTestTags
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeaderTestTags
import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.models.avatar.AvatarInitial
import ch.protonmail.android.uitest.models.labels.LabelEntry
import ch.protonmail.android.uitest.models.labels.LabelEntryModel
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString

@Suppress("TooManyFunctions")
internal class MessageHeaderEntryModel(
    composeTestRule: ComposeTestRule
) {

    private val collapseAnchor = composeTestRule.onNodeWithTag(
        testTag = ConversationDetailItemTestTags.CollapseAnchor
    )

    private val rootItem = composeTestRule.onNodeWithTag(
        testTag = MessageDetailHeaderTestTags.RootItem,
        useUnmergedTree = true
    )

    private val quickActionsItem = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ActionsRootItem)
    }

    private val avatarRootItem = rootItem.child {
        hasTestTag(AvatarTestTags.AvatarRootItem)
    }

    private val avatar = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarText)
    }

    private val avatarDraft = avatarRootItem.child {
        hasTestTag(AvatarTestTags.AvatarDraft)
    }

    private val senderName = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.SenderName)
    }

    private val senderAddress = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.SenderAddress)
    }

    private val authenticityBadge = rootItem.child {
        hasTestTag(OfficialBadgeTestTags.Item)
    }

    private val icons = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.Icons)
    }

    private val time = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.Time)
    }

    private val replyButton = quickActionsItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ReplyButton)
    }

    private val replyAllButton = quickActionsItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ReplyAllButton)
    }

    private val moreButton = quickActionsItem.child {
        hasTestTag(MessageDetailHeaderTestTags.MoreButton)
    }

    private val recipientsText = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.AllRecipientsText)
    }

    private val recipientsValue = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.AllRecipientsValue)
    }

    private val labelsList = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.LabelsList)
    }

    // region actions
    fun click() = apply {
        rootItem.performClick()
    }

    fun tapReplyButton() = apply {
        replyButton.performClick()
    }

    fun collapseMessage() = apply {
        collapseAnchor.performClick()
    }
    // endregion

    // region verification
    fun isDisplayed() = apply {
        rootItem.assertExists()
    }

    fun hasAvatar(initial: AvatarInitial) = apply {
        when (initial) {
            is AvatarInitial.WithText -> avatar.assertTextEquals(initial.text)
            is AvatarInitial.Draft -> avatarDraft.assertIsDisplayed()
        }
    }

    fun hasSenderName(name: String) = apply {
        senderName.assertTextEquals(name)
    }

    fun hasAuthenticityBadge(expectedValue: Boolean) {
        if (expectedValue) {
            authenticityBadge.assertIsDisplayed()
            authenticityBadge.assertTextEquals(getTestString(R.string.test_auth_badge_official))
        } else {
            authenticityBadge.assertDoesNotExist()
        }
    }

    fun hasSenderAddress(address: String) = apply {
        senderAddress.assertTextEquals(address)
    }

    fun hasMoreButton() = apply {
        moreButton.assertIsDisplayed()
    }

    fun hasIcons() = apply {
        icons.assertIsDisplayed()
    }

    fun hasNoIcons() = apply {
        icons.assertIsNotDisplayed()
    }

    fun hasDate(date: String) = apply {
        time.assertTextEquals(date)
    }

    fun hasRecipient(recipient: String) = apply {
        recipientsText.assertIsDisplayed()
        recipientsValue.assertTextEquals(recipient)
    }

    fun hasLabels(vararg entries: LabelEntry) = apply {
        entries.forEach {
            val model = LabelEntryModel(labelsList, it.index)
            model.hasText(it.text)
        }
    }

    fun hasReplyButton() = apply {
        replyButton.assertIsDisplayed()
    }

    fun hasReplyAllButton() = apply {
        replyAllButton.assertIsDisplayed()
    }
    // endregion
}
