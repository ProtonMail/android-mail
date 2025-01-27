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

import arrow.core.getOrElse
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import timber.log.Timber
import javax.inject.Inject

class LoadDataForMessageLabelAsBottomSheet @Inject constructor(
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val observeMessageWithLabels: ObserveMessageWithLabels
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId
    ): LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData {
        val labels = observeCustomMailLabels(userId).first()
        val color = observeFolderColorSettings(userId).first()
        val message = observeMessageWithLabels(userId, messageId).first()

        val mappedLabels = labels.onLeft {
            Timber.e("Error while observing custom labels")
        }.getOrElse { emptyList() }

        val selectedLabels = message.fold(
            ifLeft = { emptyList() },
            ifRight = { messageWithLabels ->
                messageWithLabels.labels
                    .filter { it.type == LabelType.MessageLabel && !it.labelId.isReservedSystemLabelId() }
                    .map { it.labelId }
            }
        )

        return LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = mappedLabels.map { it.toCustomUiModel(color, emptyMap(), null) }
                .toImmutableList(),
            selectedLabels = selectedLabels.toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(messageId)
        )
    }
}
