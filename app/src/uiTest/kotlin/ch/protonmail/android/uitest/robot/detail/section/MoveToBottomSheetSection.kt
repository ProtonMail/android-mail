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

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntry
import ch.protonmail.android.uitest.robot.detail.model.bottomsheet.MoveToBottomSheetFolderEntryModel
import ch.protonmail.android.uitest.util.UiDeviceHolder.uiDevice
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitEnabled
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.child
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AttachTo(targets = [ConversationDetailRobot::class, MessageDetailRobot::class])
internal class MoveToBottomSheetSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(
        testTag = MoveToBottomSheetTestTags.RootItem,
        useUnmergedTree = true
    )

    private val headerText = rootItem.child {
        hasTestTag(MoveToBottomSheetTestTags.MoveToText)
    }

    private val doneButton = rootItem.child {
        hasTestTag(MoveToBottomSheetTestTags.DoneButton)
    }

    fun tapDoneButton() {
        doneButton.awaitEnabled().performClick()
    }

    fun selectFolderAtPosition(index: Int) {
        val model = MoveToBottomSheetFolderEntryModel(index)

        model.click()
    }

    fun selectFolderWithName(name: String) {
        val model = MoveToBottomSheetFolderEntryModel(folderName = name)

        model.click()
    }

    fun dismiss() {
        uiDevice.pressBack()
    }

    @VerifiesOuter
    inner class Verify {

        fun isShown() {
            rootItem
                .awaitDisplayed()
                .assertExists()
        }

        fun isHidden() {
            rootItem
                .awaitHidden()
                .isNotDisplayed()
        }

        fun headerTextIsShown() {
            headerText.assertTextEquals(getTestString(testR.string.test_bottom_sheet_move_to_title))
        }

        fun doneButtonIsShown() {
            doneButton.assertTextEquals(getTestString(testR.string.test_bottom_sheet_done_action))
        }

        fun hasFolders(vararg entries: MoveToBottomSheetFolderEntry) {
            entries.forEach {
                val entryModel = MoveToBottomSheetFolderEntryModel(it.index)

                entryModel
                    .hasIcon(it.iconTint)
                    .hasText(it.name)
                    .also { model -> if (it.isSelected) model.hasSelectionIcon() else model.hasNoSelectionIcon() }
            }
        }
    }
}
