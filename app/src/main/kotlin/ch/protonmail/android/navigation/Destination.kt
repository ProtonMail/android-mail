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

package ch.protonmail.android.navigation

import ch.protonmail.android.mailconversation.domain.ConversationId

sealed class Destination(val route: String) {
    object Launcher : Destination("launcher")

    object Mailbox : Destination("mailbox")

    object ConversationDetail : Destination("mailbox/conversation/{conversationId}") {

        const val CONVERSATION_ID_KEY = "conversationId"

        operator fun invoke(conversationId: ConversationId) =
            "mailbox/conversation/${conversationId.id}"
    }

    object Dialog {
        object SignOut : Destination("mailbox/signout")
    }
}
