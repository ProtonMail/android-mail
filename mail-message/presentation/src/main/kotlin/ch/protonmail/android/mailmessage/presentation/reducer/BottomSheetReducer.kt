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
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.UpsellingBottomSheetState
import javax.inject.Inject

class BottomSheetReducer @Inject constructor(
    private val moveToBottomSheetReducer: MoveToBottomSheetReducer,
    private val labelAsBottomSheetReducer: LabelAsBottomSheetReducer,
    private val mailboxMoreActionsBottomSheetReducer: MailboxMoreActionsBottomSheetReducer,
    private val detailMoreActionsBottomSheetReducer: DetailMoreActionsBottomSheetReducer,
    private val contactActionsBottomSheetReducer: ContactActionsBottomSheetReducer,
    private val upsellingBottomSheetReducer: UpsellingBottomSheetReducer
) {

    fun newStateFrom(currentState: BottomSheetState?, operation: BottomSheetOperation): BottomSheetState? {
        return when (operation) {
            is ContactActionsBottomSheetState.ContactActionsBottomSheetOperation ->
                contactActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is MoveToBottomSheetState.MoveToBottomSheetOperation ->
                moveToBottomSheetReducer.newStateFrom(currentState, operation)

            is LabelAsBottomSheetState.LabelAsBottomSheetOperation ->
                labelAsBottomSheetReducer.newStateFrom(currentState, operation)

            is MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetOperation ->
                mailboxMoreActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded ->
                detailMoreActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is UpsellingBottomSheetState.UpsellingBottomSheetEvent ->
                upsellingBottomSheetReducer.newStateFrom(currentState, operation)

            is BottomSheetOperation.Dismiss -> BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Hide))
            is BottomSheetOperation.Requested -> BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Show))
        }
    }
}
