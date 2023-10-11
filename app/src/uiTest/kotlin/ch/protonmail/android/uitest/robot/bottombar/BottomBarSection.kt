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

package ch.protonmail.android.uitest.robot.bottombar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBarTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.models.bottombar.BottomBarActionEntry
import ch.protonmail.android.uitest.models.bottombar.BottomBarActionEntryModel
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobot

@AttachTo(targets = [MailboxRobot::class])
internal class BottomBarSection : ComposeSectionRobot() {

    private val rootItem = composeTestRule.onNodeWithTag(BottomActionBarTestTags.RootItem)

    fun tapAction(entry: BottomBarActionEntry) = onActionEntryModel(entry.index) {
        click()
    }

    @VerifiesOuter
    inner class Verify {

        fun isShown() {
            rootItem.assertIsDisplayed()
        }

        fun isNotShown() {
            rootItem.assertDoesNotExist()
        }

        fun hasActions(vararg entries: BottomBarActionEntry) {
            entries.forEach {
                onActionEntryModel(it.index) {
                    hasDescription(it.description)
                }
            }
        }
    }

    private fun onActionEntryModel(position: Int, block: BottomBarActionEntryModel.() -> Unit) {
        block(BottomBarActionEntryModel(position, rootItem))
    }
}
