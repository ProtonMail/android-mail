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

package ch.protonmail.android.uitest.robot.composer.section

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import ch.protonmail.android.mailcomposer.presentation.ui.ChangeSenderBottomSheetTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.model.sender.ChangeSenderEntry
import ch.protonmail.android.uitest.robot.composer.model.sender.ChangeSenderEntryModel
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden

@AttachTo(targets = [ComposerRobot::class], identifier = "changeSenderBottomSheet")
internal class ChangeSenderBottomSheetSection : ComposeSectionRobot() {

    private val parent = composeTestRule.onNodeWithTag(ChangeSenderBottomSheetTestTags.Root)

    fun tapEntryAt(position: Int) {
        val model = ChangeSenderEntryModel(position)
        model.selectSender()
    }

    fun dismiss() {
        parent.performTouchInput { swipeDown() }
    }

    @VerifiesOuter
    inner class Verify {

        fun isShown() {
            parent.awaitDisplayed().assertIsDisplayed()
        }

        fun isHidden() {
            parent.awaitHidden().assertIsNotDisplayed()
        }

        fun hasEntries(vararg entries: ChangeSenderEntry) {
            for (entry in entries) {
                val model = ChangeSenderEntryModel(entry.index)

                if (entry.isEnabled) {
                    model.hasText(entry.address)
                } else {
                    model.doesNotExist()
                }
            }
        }
    }
}
