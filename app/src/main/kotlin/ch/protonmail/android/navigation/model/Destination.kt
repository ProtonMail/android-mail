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

package ch.protonmail.android.navigation.model

import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import me.proton.core.domain.entity.UserId

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object Conversation : Destination("mailbox/conversation/{$key}") {
            operator fun invoke(conversationId: ConversationId) =
                "mailbox/conversation/${conversationId.id}"

            fun getConversationId(key: String) = ConversationId(key)
        }

        object Message : Destination("mailbox/message/{$key}") {
            operator fun invoke(messageId: MessageId) =
                "mailbox/message/${messageId.id}"

            fun getMessageId(key: String) = MessageId(key)
        }

        object Settings : Destination("settings")
        object AccountSettings : Destination("settings/account")
        object ConversationModeSettings : Destination("settings/account/conversationMode")
        object ThemeSettings : Destination("settings/theme")
        object LanguageSettings : Destination("settings/appLanguage")
        object ReportBug : Destination("report")
        object SwipeActionsSettings : Destination("settings/swipeActions")
    }

    object Dialog {
        object RemoveAccount : Destination("remove/{key}") {

            operator fun invoke(userId: UserId?) =
                if (userId == null) "remove/null"
                else "remove/${userId.id}"

            fun getUserId(key: String) =
                if (key == "null") null
                else UserId(key)
        }
    }

    companion object {
        const val key = "key"
    }
}
