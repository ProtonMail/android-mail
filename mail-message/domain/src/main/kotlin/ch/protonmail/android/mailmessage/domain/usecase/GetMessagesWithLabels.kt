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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

class GetMessagesWithLabels @Inject constructor(
    private val observeMessages: ObserveMessages,
    private val observeLabels: ObserveLabels
) {

    suspend operator fun invoke(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError, List<MessageWithLabels>> {
        return combine(
            observeMessages(userId, messageIds),
            observeLabels(userId, LabelType.MessageLabel),
            observeLabels(userId, LabelType.MessageFolder)
        ) { messagesEither, labelsEither, foldersEither ->
            either {
                val messages = messagesEither.bind()
                val allLabelAndFolders = (labelsEither.bind() + foldersEither.bind()).sortedBy { it.order }
                messages.map { message ->
                    val messageLabels = allLabelAndFolders.filter { label ->
                        label.labelId in message.labelIds
                    }
                    MessageWithLabels(message, messageLabels)
                }
            }
        }.first()
    }
}
