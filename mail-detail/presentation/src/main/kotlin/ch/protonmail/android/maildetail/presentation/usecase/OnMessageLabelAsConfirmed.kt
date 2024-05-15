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

package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelMessage
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.LabelUiModelWithSelectedState
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import timber.log.Timber
import javax.inject.Inject

class OnMessageLabelAsConfirmed @Inject constructor(
    private val moveMessage: MoveMessage,
    private val observeMessageWithLabels: ObserveMessageWithLabels,
    private val relabelMessage: RelabelMessage
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        labelUiModelsWithSelectedState: List<LabelUiModelWithSelectedState>,
        archiveSelected: Boolean
    ): Either<DataError.Local, Message> {
        val messageWithLabels = checkNotNull(
            observeMessageWithLabels(userId, messageId).first().getOrNull()
        ) { "Message not found" }

        val previousSelectedLabels = messageWithLabels.labels
            .filter { it.type == LabelType.MessageLabel }
            .map { it.labelId }

        val newSelectedLabels = labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.Selected }
            .map { it.labelUiModel.id.labelId }

        if (archiveSelected) {
            moveMessage(
                userId,
                messageId,
                SystemLabelId.Archive.labelId
            ).onLeft { Timber.e("Move message failed: $it") }
        }

        return relabelMessage(
            userId = userId,
            messageId = messageId,
            currentLabelIds = previousSelectedLabels,
            updatedLabelIds = newSelectedLabels
        )
    }
}
