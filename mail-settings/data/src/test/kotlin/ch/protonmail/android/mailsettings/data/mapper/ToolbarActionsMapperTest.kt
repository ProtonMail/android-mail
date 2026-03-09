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

import ch.protonmail.android.mailcommon.domain.model.Action
import uniffi.mail_uniffi.MobileAction
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ToolbarActionsMapperTest {

    @Test
    fun `should map mobile actions to actions correctly`() {
        // Given
        val mobileActions = listOf(
            MobileAction.ARCHIVE,
            MobileAction.FORWARD,
            MobileAction.LABEL,
            MobileAction.MOVE,
            MobileAction.PRINT,
            MobileAction.REPLY,
            MobileAction.REPORT_PHISHING,
            MobileAction.SNOOZE,
            MobileAction.SPAM,
            MobileAction.TOGGLE_LIGHT,
            MobileAction.TOGGLE_READ,
            MobileAction.TOGGLE_STAR,
            MobileAction.TRASH,
            MobileAction.VIEW_HEADERS,
            MobileAction.VIEW_HTML
        )

        val expectedActions = listOf(
            Action.Archive,
            Action.Forward,
            Action.Label,
            Action.Move,
            Action.Print,
            Action.Reply,
            Action.ReportPhishing,
            Action.Snooze,
            Action.Spam,
            Action.ViewInLightMode,
            Action.MarkUnread,
            Action.Star,
            Action.Trash,
            Action.ViewHeaders,
            Action.ViewHtml,
            null
        )

        // When
        val mappings = mobileActions.zip(expectedActions)

        // Then
        mappings.forEach { (mobileAction, expectedAction) ->
            val actual = mobileAction.toAction()
            assertEquals(expectedAction, actual)
        }
    }

    @Test
    fun `should map actions to mobile actions correctly`() {
        // Given
        val actions = listOf(
            Action.Reply,
            Action.ReplyAll,
            Action.Forward,
            Action.MarkRead,
            Action.MarkUnread,
            Action.Star,
            Action.Unstar,
            Action.Label,
            Action.Move,
            Action.Trash,
            Action.Delete,
            Action.Archive,
            Action.Spam,
            Action.ViewInLightMode,
            Action.ViewInDarkMode,
            Action.Print,
            Action.ViewHeaders,
            Action.ViewHtml,
            Action.ReportPhishing,
            Action.Snooze,
            Action.CustomizeToolbar,
            Action.Inbox,
            Action.More,
            Action.SavePdf,
            Action.Remind,
            Action.SaveAttachments,
            Action.SenderEmails,
            Action.Pin,
            Action.Unpin
        )

        val expectedMobileActions = listOf(
            MobileAction.REPLY, // Action.Reply
            MobileAction.REPLY, // Action.ReplyAll
            MobileAction.FORWARD, // Action.Forward
            MobileAction.TOGGLE_READ, // Action.MarkRead
            MobileAction.TOGGLE_READ, // Action.MarkUnread
            MobileAction.TOGGLE_STAR, // Action.Star
            MobileAction.TOGGLE_STAR, // Action.Unstar
            MobileAction.LABEL, // Action.Label
            MobileAction.MOVE, // Action.Move
            MobileAction.TRASH, // Action.Trash
            MobileAction.TRASH, // Action.Delete
            MobileAction.ARCHIVE,
            MobileAction.SPAM,
            MobileAction.TOGGLE_LIGHT, // Action.ViewInLightMode
            MobileAction.TOGGLE_LIGHT, // Action.ViewInDarkMode
            MobileAction.PRINT,
            MobileAction.VIEW_HEADERS,
            MobileAction.VIEW_HTML,
            MobileAction.REPORT_PHISHING,
            MobileAction.SNOOZE,
            null, // Action.CustomizeToolbar
            null, // Action.Inbox
            null, // Action.More
            null, // Action.SavePdf
            null, // Action.Remind
            null, // Action.SaveAttachments
            null, // Action.SenderEmails
            null, // Action.Pin
            null // Action.Unpin
        )

        // When
        val mappings = actions.zip(expectedMobileActions)

        // Then
        mappings.forEach { (action, expectedMobileAction) ->
            val actual = action.toLocalMobileAction()
            assertEquals(expectedMobileAction, actual)
        }
    }
}
