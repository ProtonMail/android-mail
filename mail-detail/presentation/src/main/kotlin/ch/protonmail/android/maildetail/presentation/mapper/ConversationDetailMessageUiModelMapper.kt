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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import me.proton.core.contact.domain.entity.Contact
import javax.inject.Inject

class ConversationDetailMessageUiModelMapper @Inject constructor(
    private val avatarUiModelMapper: DetailAvatarUiModelMapper,
    private val resolveParticipantName: ResolveParticipantName
) {

    fun toUiModel(message: Message, contacts: List<Contact>): ConversationDetailMessageUiModel {
        val senderResolvedName = resolveParticipantName(message.sender, contacts)
        return ConversationDetailMessageUiModel.Collapsed(
            avatar = avatarUiModelMapper(
                message,
                senderResolvedName = senderResolvedName
            ),
            isUnread = message.unread,
            sender = senderResolvedName
        )
    }
}

