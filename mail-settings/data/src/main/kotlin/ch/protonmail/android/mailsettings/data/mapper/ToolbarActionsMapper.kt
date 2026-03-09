/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalMobileAction
import ch.protonmail.android.mailcommon.domain.model.Action
import uniffi.mail_uniffi.MobileAction

fun LocalMobileAction.toAction() = when (this) {
    MobileAction.ARCHIVE -> Action.Archive
    MobileAction.FORWARD -> Action.Forward
    MobileAction.LABEL -> Action.Label
    MobileAction.MOVE -> Action.Move
    MobileAction.PRINT -> Action.Print
    MobileAction.REPLY -> Action.Reply
    MobileAction.REPORT_PHISHING -> Action.ReportPhishing
    MobileAction.SNOOZE -> Action.Snooze
    MobileAction.SPAM -> Action.Spam
    MobileAction.TOGGLE_LIGHT -> Action.ViewInLightMode
    MobileAction.TOGGLE_READ -> Action.MarkUnread
    MobileAction.TOGGLE_STAR -> Action.Star
    MobileAction.TRASH -> Action.Trash
    MobileAction.VIEW_HEADERS -> Action.ViewHeaders
    MobileAction.VIEW_HTML -> Action.ViewHtml
}

fun Action.toLocalMobileAction(): MobileAction? = when (this) {
    Action.Reply,
    Action.ReplyAll -> MobileAction.REPLY

    Action.Forward -> MobileAction.FORWARD
    Action.MarkRead,
    Action.MarkUnread -> MobileAction.TOGGLE_READ

    Action.Star,
    Action.Unstar -> MobileAction.TOGGLE_STAR

    Action.Label -> MobileAction.LABEL
    Action.Move -> MobileAction.MOVE
    Action.Trash,
    Action.Delete -> MobileAction.TRASH

    Action.Archive -> MobileAction.ARCHIVE

    Action.Spam -> MobileAction.SPAM

    Action.ViewInLightMode,
    Action.ViewInDarkMode -> MobileAction.TOGGLE_LIGHT

    Action.Print -> MobileAction.PRINT
    Action.ViewHeaders -> MobileAction.VIEW_HEADERS
    Action.ViewHtml -> MobileAction.VIEW_HTML
    Action.ReportPhishing -> MobileAction.REPORT_PHISHING
    Action.Snooze -> MobileAction.SNOOZE

    Action.CustomizeToolbar,
    Action.Inbox,
    Action.More,
    Action.SavePdf,
    Action.Remind,
    Action.SaveAttachments,
    Action.SenderEmails,
    Action.Pin,
    Action.Unpin -> null // Not handled
}
