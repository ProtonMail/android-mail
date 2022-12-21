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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MoveConversation @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val observeExclusiveMailLabels: ObserveExclusiveMailLabels
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> {
        return conversationRepository.observeConversation(userId, conversationId).first().fold(
            ifLeft = { DataError.Local.NoDataCached.left() },
            ifRight = { conversation ->
                val exclusiveLabelIds = observeExclusiveMailLabels(userId).first().allById.mapKeys { it.key.labelId }
                val fromLabelId = conversation.labels
                    .map { it.labelId }
                    .firstOrNull { labelId -> labelId in exclusiveLabelIds }
                conversationRepository.move(userId, conversationId, fromLabelId, toLabelId = labelId)
            }
        )
    }

}
