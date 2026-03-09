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

package ch.protonmail.android.mailconversation.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import uniffi.mail_uniffi.ActionError
import uniffi.mail_uniffi.ActionErrorReason

fun ActionError.toConversationError() = when (this) {
    is ActionError.Other -> ConversationError.Other(this.v1.toDataError())
    is ActionError.Reason -> when (v1) {
        ActionErrorReason.UNKNOWN_LABEL -> ConversationError.UnknownLabel
        ActionErrorReason.UNKNOWN_MESSAGE -> ConversationError.UnknownMessage
        ActionErrorReason.UNKNOWN_CONTENT_ID -> ConversationError.UnknownContentId
    }
}

