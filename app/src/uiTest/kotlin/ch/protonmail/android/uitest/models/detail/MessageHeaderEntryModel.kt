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
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailHeaderTestTags
import ch.protonmail.android.uitest.models.labels.LabelEntry
import ch.protonmail.android.uitest.models.labels.LabelEntryModel
import ch.protonmail.android.uitest.util.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.child

@Suppress("TooManyFunctions")
internal class MessageHeaderEntryModel(
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val rootItem = composeTestRule.onNodeWithTag(
        testTag = MessageDetailHeaderTestTags.RootItem,
        useUnmergedTree = true
    )

    private val avatar = rootItem.child {
        hasTestTag(AvatarTestTags.Avatar)
    }

    private val avatarDraft = rootItem.child {
        hasTestTag(AvatarTestTags.AvatarDraft)
    }

    private val senderName = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.SenderName)
    }

    private val senderAddress = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.SenderAddress)
    }

    private val icons = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.Icons)
    }

    private val time = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.Time)
    }

    private val moreButton = rootItem.child {
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
    // endregion

    // region verification
    fun isDisplayed() = apply {
        rootItem.assertExists()
    }

    fun hasAvatarDraft() = apply {
        avatarDraft.assertIsDisplayed()
    }

    fun hasAvatarText(text: String) = apply {
        avatar.assertTextEquals(text)
    }

    fun hasSenderName(name: String) = apply {
        senderName.assertTextEquals(name)
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
    // endregion
}
