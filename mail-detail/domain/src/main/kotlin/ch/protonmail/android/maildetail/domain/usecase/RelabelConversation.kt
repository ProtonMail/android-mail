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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class RelabelConversation @Inject constructor(
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationId: ConversationId,
        currentLabelIds: List<LabelId>,
        currentPartialSelectedLabelIds: List<LabelId>,
        updatedLabelIds: List<LabelId>,
        updatedPartialSelectedLabelIds: List<LabelId>
    ): Either<DataError, Conversation> {
        val removedLabels = currentLabelIds - updatedLabelIds
        val addedLabels = updatedLabelIds - currentLabelIds

        val removedPartialSelectedLabels = currentPartialSelectedLabelIds - updatedPartialSelectedLabelIds

        return conversationRepository.relabel(
            userId = userId,
            conversationId = conversationId,
            labelsToBeRemoved = removedLabels + removedPartialSelectedLabels,
            labelsToBeAdded = addedLabels
        )
    }
}
