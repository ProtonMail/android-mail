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

import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

internal data class MoveToBottomSheetFolderEntry(
    val index: Int,
    val name: String,
    val iconTint: Tint = Tint.NoColor,
    val isSelected: Boolean = false
) {

    object SystemFolders {

        val Inbox = MoveToBottomSheetFolderEntry(index = 0, name = getTestString(testR.string.label_title_inbox))
        val Archive = MoveToBottomSheetFolderEntry(index = 1, name = getTestString(testR.string.label_title_archive))
        val Spam = MoveToBottomSheetFolderEntry(index = 2, name = getTestString(testR.string.label_title_spam))
        val Trash = MoveToBottomSheetFolderEntry(index = 3, name = getTestString(testR.string.label_title_trash))
    }
}
