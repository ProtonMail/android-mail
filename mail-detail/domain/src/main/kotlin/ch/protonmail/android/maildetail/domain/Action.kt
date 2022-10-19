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

package ch.protonmail.android.maildetail.domain

import androidx.annotation.DrawableRes

enum class Action {
    Reply,
    ReplyAll,
    Forward,
    MarkRead,
    MarkUnread,
    Star,
    Unstar,
    Label,
    Move,
    Trash,
    Archive,
    Spam,
    ViewInLightMode,
    ViewInDarkMode,
    Print,
    ViewHeaders,
    ViewHtml,
    ReportPhishing,
    Remind,
    SavePdf,
    SenderEmails,
    SaveAttachments
}

@DrawableRes
fun Action.iconDrawable(): Int {
    return when (this) {
        Action.Reply -> R.drawable.ic_proton_arrow_up_and_left
        Action.ReplyAll -> R.drawable.ic_proton_arrows_up_and_left
        Action.Forward -> R.drawable.ic_arrow_forward
        Action.MarkRead -> R.drawable.ic_proton_envelope
        Action.MarkUnread -> R.drawable.ic_proton_envelope_dot
        Action.Star -> R.drawable.ic_proton_star
        Action.Unstar -> R.drawable.ic_proton_star_filled
        Action.Label -> R.drawable.ic_proton_tag
        Action.Move -> R.drawable.ic_proton_folder_arrow_in
        Action.Trash -> R.drawable.ic_proton_trash
        Action.Archive -> R.drawable.ic_proton_archive_box
        Action.Spam -> R.drawable.ic_proton_fire
        Action.ViewInLightMode -> R.drawable.ic_proton_circle
        Action.ViewInDarkMode -> R.drawable.ic_proton_circle_filled
        Action.Print -> R.drawable.ic_proton_printer
        Action.ViewHeaders -> R.drawable.ic_proton_file_lines
        Action.ViewHtml -> R.drawable.ic_proton_code
        Action.ReportPhishing -> R.drawable.ic_proton_hook
        Action.Remind -> R.drawable.ic_proton_clock
        Action.SavePdf -> R.drawable.ic_proton_arrow_down_line
        Action.SenderEmails -> R.drawable.ic_proton_envelope
        Action.SaveAttachments -> R.drawable.ic_proton_arrow_down_to_square
    }
}
