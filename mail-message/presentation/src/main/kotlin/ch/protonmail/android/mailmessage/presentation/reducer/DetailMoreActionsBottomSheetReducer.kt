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

package ch.protonmail.android.mailmessage.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.presentation.mapper.DetailMoreActionsBottomSheetUiMapper
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded
import javax.inject.Inject

class DetailMoreActionsBottomSheetReducer @Inject constructor(
    private val mapper: DetailMoreActionsBottomSheetUiMapper
) {

    fun newStateFrom(
        currentState: BottomSheetState?,
        operation: DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetOperation
    ): BottomSheetState {
        return when (operation) {
            is DataLoaded -> operation.toNewBottomSheetState(currentState)
        }
    }

    private fun DataLoaded.toNewBottomSheetState(currentState: BottomSheetState?): BottomSheetState {
        val headerUiModel = mapper.toHeaderUiModel(messageSender, messageSubject, messageId)
        val actionsUiModel = mapper.mapMoreActionUiModels(actions)

        return BottomSheetState(
            contentState = DetailMoreActionsBottomSheetState.Data(
                isAffectingConversation = affectingConversation,
                messageDataUiModel = headerUiModel,
                replyActionsUiModel = actionsUiModel
            ),
            bottomSheetVisibilityEffect = currentState?.bottomSheetVisibilityEffect ?: Effect.empty()
        )
    }
}
