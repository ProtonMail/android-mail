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
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class RelabelConversation @Inject constructor(
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationId: ConversationId,
        currentSelections: LabelSelectionList,
        updatedSelections: LabelSelectionList
    ): Either<DataError, Conversation> {
        val currentFullAndPartialSelections = currentSelections.let { it.selectedLabels + it.partiallySelectionLabels }
        val updatedFullAndPartialSelections = updatedSelections.let { it.selectedLabels + it.partiallySelectionLabels }

        val labelsToBeRemoved = currentFullAndPartialSelections - updatedFullAndPartialSelections.toSet()
        val labelsToBeAdded = updatedSelections.selectedLabels - currentSelections.selectedLabels.toSet()

        return conversationRepository.relabel(
            userId = userId,
            conversationId = conversationId,
            labelsToBeRemoved = labelsToBeRemoved,
            labelsToBeAdded = labelsToBeAdded
        )
    }
}
