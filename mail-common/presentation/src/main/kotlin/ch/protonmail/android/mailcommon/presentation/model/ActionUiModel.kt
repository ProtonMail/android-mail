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

package ch.protonmail.android.mailcommon.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.R

data class ActionUiModel(
    val action: Action,
    @DrawableRes val icon: Int = action.iconDrawable(),
    val description: TextUiModel = action.description(),
    val contentDescription: TextUiModel = action.contentDescription()
)

@DrawableRes
@SuppressWarnings("ComplexMethod")
fun Action.iconDrawable() = when (this) {
    Action.Reply -> R.drawable.ic_proton_reply
    Action.ReplyAll -> R.drawable.ic_proton_reply_all
    Action.Forward -> R.drawable.ic_proton_forward
    Action.MarkRead -> R.drawable.ic_proton_envelope
    Action.MarkUnread -> R.drawable.ic_proton_envelope_dot
    Action.Star -> R.drawable.ic_proton_star
    Action.Unstar -> R.drawable.ic_proton_star_slash
    Action.Label -> R.drawable.ic_proton_tag
    Action.Move -> R.drawable.ic_proton_folder_arrow_in
    Action.Trash -> R.drawable.ic_proton_trash
    Action.Delete -> R.drawable.ic_proton_trash_cross
    Action.Archive -> R.drawable.ic_proton_archive_box
    Action.Spam -> R.drawable.ic_proton_fire
    Action.ViewInLightMode -> R.drawable.ic_proton_sun
    Action.ViewInDarkMode -> R.drawable.ic_proton_moon
    Action.Print -> R.drawable.ic_proton_printer
    Action.ViewHeaders -> R.drawable.ic_proton_file_lines
    Action.ViewHtml -> R.drawable.ic_proton_code
    Action.ReportPhishing -> R.drawable.ic_proton_hook
    Action.Remind -> R.drawable.ic_proton_clock
    Action.SavePdf -> R.drawable.ic_proton_arrow_down_line
    Action.SenderEmails -> R.drawable.ic_proton_envelope
    Action.SaveAttachments -> R.drawable.ic_proton_arrow_down_to_square
    Action.More -> R.drawable.ic_proton_three_dots_horizontal
    Action.OpenCustomizeToolbar -> R.drawable.ic_proton_magic_proton_wand
}

@get:StringRes
val Action.contentDescriptionRes: Int
    get() = when (this) {
        Action.Reply -> R.string.action_reply_content_description
        Action.ReplyAll -> R.string.action_reply_all_content_description
        Action.Forward -> R.string.action_forward_content_description
        Action.MarkRead -> R.string.action_mark_read_content_description
        Action.MarkUnread -> R.string.action_mark_unread_content_description
        Action.Star -> R.string.action_star_content_description
        Action.Unstar -> R.string.action_unstar_content_description
        Action.Label -> R.string.action_label_content_description
        Action.Move -> R.string.action_move_content_description
        Action.Trash -> R.string.action_trash_content_description
        Action.Delete -> R.string.action_delete_content_description
        Action.Archive -> R.string.action_archive_content_description
        Action.Spam -> R.string.action_spam_content_description
        Action.ViewInLightMode -> R.string.action_view_in_light_mode_content_description
        Action.ViewInDarkMode -> R.string.action_view_in_dark_mode_content_description
        Action.Print -> R.string.action_print_content_description
        Action.ViewHeaders -> R.string.action_view_headers_content_description
        Action.ViewHtml -> R.string.action_view_html_content_description
        Action.ReportPhishing -> R.string.action_report_phishing_content_description
        Action.Remind -> R.string.action_remind_content_description
        Action.SavePdf -> R.string.action_save_pdf_content_description
        Action.SenderEmails -> R.string.action_sender_emails_content_description
        Action.SaveAttachments -> R.string.action_save_attachments_content_description
        Action.More -> R.string.action_more_content_description
        Action.OpenCustomizeToolbar -> R.string.action_open_customize_toolbar
    }

@SuppressWarnings("ComplexMethod")
fun Action.contentDescription() = TextUiModel(contentDescriptionRes)

@get:StringRes
val Action.descriptionRes: Int
    get() = when (this) {
        Action.Reply -> R.string.action_reply_description
        Action.ReplyAll -> R.string.action_reply_all_description
        Action.Forward -> R.string.action_forward_description
        Action.MarkRead -> R.string.action_mark_read_description
        Action.MarkUnread -> R.string.action_mark_unread_description
        Action.Star -> R.string.action_star_description
        Action.Unstar -> R.string.action_unstar_description
        Action.Label -> R.string.action_label_description
        Action.Move -> R.string.action_move_description
        Action.Trash -> R.string.action_trash_description
        Action.Delete -> R.string.action_delete_description
        Action.Archive -> R.string.action_archive_description
        Action.Spam -> R.string.action_spam_description
        Action.ViewInLightMode -> R.string.action_view_in_light_mode_description
        Action.ViewInDarkMode -> R.string.action_view_in_dark_mode_description
        Action.Print -> R.string.action_print_description
        Action.ViewHeaders -> R.string.action_view_headers_description
        Action.ViewHtml -> R.string.action_view_html_description
        Action.ReportPhishing -> R.string.action_report_phishing_description
        Action.Remind -> R.string.action_remind_description
        Action.SavePdf -> R.string.action_save_pdf_description
        Action.SenderEmails -> R.string.action_sender_emails_description
        Action.SaveAttachments -> R.string.action_save_attachments_description
        Action.More -> R.string.action_more_description
        Action.OpenCustomizeToolbar -> R.string.action_open_customize_toolbar_description
    }

fun Action.description() = TextUiModel.TextRes(descriptionRes)
