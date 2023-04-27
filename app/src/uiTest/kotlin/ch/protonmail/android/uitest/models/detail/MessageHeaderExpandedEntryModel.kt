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

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailHeaderTestTags
import ch.protonmail.android.uitest.models.labels.LabelEntry
import ch.protonmail.android.uitest.models.labels.LabelEntryModel
import ch.protonmail.android.uitest.util.child

internal class MessageHeaderExpandedEntryModel(composeTestRule: ComposeTestRule) {

    private val rootItem = composeTestRule.onNodeWithTag(
        testTag = MessageDetailHeaderTestTags.RootItem,
        useUnmergedTree = true
    )

    // The structure of the Labels component is a bit different than the others.
    private val labels = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedLabelRow)
    }

    private val time = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedTimeRow)
    }.asExtendedHeaderRowEntryModel()

    private val location = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedFolderRow)
    }.asExtendedHeaderRowEntryModel()

    private val size = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedSizeRow)
    }.asExtendedHeaderRowEntryModel()

    private val hideDetailsButton = rootItem.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedHideDetails)
    }

    // region verification
    fun hasRecipients(vararg recipients: ExtendedHeaderRecipientEntry) = apply {
        recipients.forEach {
            val model = it.asEntryModel()
            model.hasName(it.name)
                .hasAddress(it.address)
        }
    }

    fun hasLabels(vararg entries: LabelEntry) = apply {
        labels.child { hasTestTag(MessageDetailHeaderTestTags.LabelIcon) }.assertExists()

        entries.forEach {
            val model = LabelEntryModel(labels, it.index)
            model.hasText(it.text)
        }
    }

    fun hasTime(value: String) = apply {
        time.hasIcon()
            .hasText(value)
    }

    fun hasLocation(value: String) = apply {
        location.hasIcon()
            .hasText(value)
    }

    fun hasSize(value: String) = apply {
        size.hasIcon()
            .hasText(value)
    }

    fun hasHideDetailsButton() = apply {
        hideDetailsButton.assertIsDisplayed()
    }
    // endregion

    private fun ExtendedHeaderRecipientEntry.asEntryModel(): ExtendedHeaderRecipientEntryModel {
        val testTag = when (kind) {
            RecipientKind.To -> MessageDetailHeaderTestTags.ToRecipientsList
            RecipientKind.Cc -> MessageDetailHeaderTestTags.CcRecipientsList
            RecipientKind.Bcc -> MessageDetailHeaderTestTags.BccRecipientsList
        }

        val parent = rootItem.child { hasTestTag(testTag) }
        return ExtendedHeaderRecipientEntryModel(parent, index)
    }

    private fun SemanticsNodeInteraction.asExtendedHeaderRowEntryModel() = ExtendedHeaderRowEntryModel(this)
}
