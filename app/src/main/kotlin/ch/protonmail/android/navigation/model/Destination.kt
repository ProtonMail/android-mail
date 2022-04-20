/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation.model

import ch.protonmail.android.mailconversation.domain.ConversationId
import me.proton.core.domain.entity.UserId

sealed class Destination(val route: String) {

    object Screen {
        object Mailbox : Destination("mailbox")

        object Conversation : Destination("mailbox/conversation/{key}") {
            operator fun invoke(conversationId: ConversationId) =
                "mailbox/conversation/${conversationId.id}"

            fun getConversationId(key: String) = ConversationId(key)
        }

        object Settings : Destination("settings")
        object AccountSettings : Destination("settings/account")
        object ConversationModeSettings : Destination("settings/account/conversationMode")
        object ThemeSettings : Destination("settings/theme")
        object LanguageSettings : Destination("settings/appLanguage")
        object ReportBug : Destination("report")
    }

    object Dialog {
        object RemoveAccount : Destination("remove/{key}") {
            operator fun invoke(userId: UserId?) = when (userId) {
                null -> "remove/null"
                else -> "remove/${userId.id}"
            }

            fun getUserId(key: String) = when (key) {
                "null" -> null
                else -> UserId(key)
            }
        }
    }

    companion object {
        const val key = "key"
    }
}
