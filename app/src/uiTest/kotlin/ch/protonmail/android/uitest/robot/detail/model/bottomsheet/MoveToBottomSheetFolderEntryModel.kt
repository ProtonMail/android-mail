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

package ch.protonmail.android.uitest.robot.detail.model.bottomsheet

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetTestTags
import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.test.utils.ComposeTestRuleHolder
import ch.protonmail.android.uitest.util.assertions.assertTintColor
import ch.protonmail.android.uitest.util.child

internal class MoveToBottomSheetFolderEntryModel private constructor(
    matcher: SemanticsMatcher,
    index: Int,
    composeTestRule: ComposeTestRule = ComposeTestRuleHolder.rule
) {

    private val rootItem = composeTestRule.onAllNodes(matcher, useUnmergedTree = true)[index]

    private val icon = rootItem.child {
        hasTestTag(MoveToBottomSheetTestTags.FolderIcon)
    }

    private val text = rootItem.child {
        hasTestTag(MoveToBottomSheetTestTags.FolderNameText)
    }

    private val selectionIcon = rootItem.child {
        hasTestTag(MoveToBottomSheetTestTags.FolderSelectionIcon)
    }

    // region actions
    fun click() {
        rootItem.performClick()
    }
    // endregion

    // region verification
    fun hasIcon(tint: Tint): MoveToBottomSheetFolderEntryModel = apply {
        icon.assertExists()
        icon.assertTintColor(tint)
    }

    fun hasText(value: String): MoveToBottomSheetFolderEntryModel = apply {
        text.assertTextEquals(value)
    }

    fun hasSelectionIcon(): MoveToBottomSheetFolderEntryModel = apply {
        selectionIcon.assertExists()
    }

    fun hasNoSelectionIcon(): MoveToBottomSheetFolderEntryModel = apply {
        selectionIcon.assertDoesNotExist()
    }
    // endregion

    companion object {

        operator fun invoke(index: Int): MoveToBottomSheetFolderEntryModel {
            val matcher = hasTestTag(MoveToBottomSheetTestTags.FolderItem)
            return MoveToBottomSheetFolderEntryModel(matcher, index)
        }

        operator fun invoke(folderName: String): MoveToBottomSheetFolderEntryModel {
            val matcher = hasText(folderName) and hasParent(hasTestTag(MoveToBottomSheetTestTags.FolderItem))
            return MoveToBottomSheetFolderEntryModel(matcher, index = 0) // Always 0, no duplicates in names.
        }
    }
}
