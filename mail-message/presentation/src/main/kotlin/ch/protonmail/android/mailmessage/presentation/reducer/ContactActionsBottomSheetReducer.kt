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
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import javax.inject.Inject

class ContactActionsBottomSheetReducer @Inject constructor() {

    fun newStateFrom(
        currentState: BottomSheetState?,
        operation: ContactActionsBottomSheetState.ContactActionsBottomSheetOperation
    ): BottomSheetState? {
        return when (operation) {
            is ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData ->
                operation.toNewBottomSheetState(
                    currentState
                )
            else -> currentState
        }
    }

    private fun ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData.toNewBottomSheetState(
        currentState: BottomSheetState?
    ): BottomSheetState {
        return BottomSheetState(
            contentState = ContactActionsBottomSheetState.Data(
                participant = participant, avatarUiModel = avatarUiModel, contactId = contactId
            ),
            bottomSheetVisibilityEffect = currentState?.bottomSheetVisibilityEffect ?: Effect.empty()
        )
    }
}
