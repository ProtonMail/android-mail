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

import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.entity.Message
import javax.inject.Inject

class DetailAvatarUiModelMapper @Inject constructor() {

    operator fun invoke(message: Message, senderResolvedName: String): AvatarUiModel {

        return if (message.isDraft()) {
            AvatarUiModel.DraftIcon
        } else {
            AvatarUiModel.ParticipantInitial(senderResolvedName.getInitial())
        }
    }

    private fun Message.isDraft() = labelIds.any { it == SystemLabelId.AllDrafts.labelId }

    private fun String.getInitial(): String {
        val firstChar = this[0].uppercaseChar()
        val stringBuilder = StringBuilder().append(firstChar)
        if (firstChar.isHighSurrogate()) {
            stringBuilder.append(this[1])
        }
        return stringBuilder.toString()
    }
}
