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

package ch.protonmail.android.uitest.models.bottombar

import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

// Every inheritor has a default index which reflects the current implementation in App.
internal sealed class BottomBarActionEntry(val index: Int, val description: String) {

    class MarkAsRead(index: Int = 0) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_mark_read_content_description))

    class MarkAsUnread(index: Int = 0) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_mark_unread_content_description))

    class Trash(index: Int = 1) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_trash_content_description))

    class Delete(index: Int = 1) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_delete_content_description))

    class MoveTo(index: Int = 2) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_move_content_description))

    class LabelAs(index: Int = 3) :
        BottomBarActionEntry(index, getTestString(testR.string.test_action_label_content_description))

    object More :
        BottomBarActionEntry(index = LastItemIndex, getTestString(testR.string.test_action_more_content_description))

    object Defaults {

        val actionsOnReadItem = arrayOf(MarkAsUnread(), Trash(), MoveTo(), LabelAs(), More)
        val actionsOnUnreadItem = arrayOf(MarkAsRead(), Trash(), MoveTo(), LabelAs(), More)
        val actionsOnTrashedReadItem = arrayOf(MarkAsUnread(), Delete(), MoveTo(), LabelAs(), More)
        val actionsOnTrashedUnreadItem = arrayOf(MarkAsRead(), Delete(), MoveTo(), LabelAs(), More)
        val actionsOnSpamReadItem = arrayOf(MarkAsUnread(), Delete(), MoveTo(), LabelAs(), More)
        val actionsOnSpamUnreadItem = arrayOf(MarkAsRead(), Delete(), MoveTo(), LabelAs(), More)
    }

    private companion object {

        private const val TotalItemsThreshold = 5
        private const val LastItemIndex = TotalItemsThreshold - 1
    }
}
