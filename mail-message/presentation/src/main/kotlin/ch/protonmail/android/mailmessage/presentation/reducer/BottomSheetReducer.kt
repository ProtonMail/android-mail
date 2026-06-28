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
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetOperation
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ManageAccountSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.SnoozeSheetState
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoBottomSheetEvent
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoSheetState
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedElementsSheetState
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedTrackersBottomSheetEvent
import javax.inject.Inject

class BottomSheetReducer @Inject constructor(
    private val mailboxMoreActionsBottomSheetReducer: MailboxMoreActionsBottomSheetReducer,
    private val detailMoreActionsBottomSheetReducer: DetailMoreActionsBottomSheetReducer,
    private val contactActionsBottomSheetReducer: ContactActionsBottomSheetReducer
) {

    fun newStateFrom(currentState: BottomSheetState?, operation: BottomSheetOperation): BottomSheetState? {
        return when (operation) {
            is ContactActionsBottomSheetState.ContactActionsBottomSheetOperation ->
                contactActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is MoveToBottomSheetState.MoveToBottomSheetEvent.Ready ->
                BottomSheetState(
                    contentState = MoveToBottomSheetState.Requested(
                        operation.userId,
                        operation.currentLabel,
                        operation.itemIds,
                        operation.entryPoint,
                        operation.activeCategoryId
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )

            is LabelAsBottomSheetState.LabelAsBottomSheetEvent.Ready ->
                BottomSheetState(
                    contentState = LabelAsBottomSheetState.Requested(
                        operation.userId,
                        operation.currentLabel,
                        operation.itemIds,
                        operation.entryPoint
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )

            is MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetOperation ->
                mailboxMoreActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is DetailMoreActionsBottomSheetState.DetailMoreActionsBottomSheetEvent.DataLoaded ->
                detailMoreActionsBottomSheetReducer.newStateFrom(currentState, operation)

            is ManageAccountSheetState.ManageAccountsBottomSheetEvent ->
                BottomSheetState(
                    contentState = ManageAccountSheetState.Requested,
                    bottomSheetVisibilityEffect = currentState?.bottomSheetVisibilityEffect ?: Effect.empty()
                )

            is SnoozeSheetState.SnoozeOptionsBottomSheetEvent.Ready ->
                BottomSheetState(
                    contentState = SnoozeSheetState.Requested(
                        operation.userId,
                        operation.labelId,
                        operation.itemIds
                    ),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )

            is BottomSheetOperation.Dismiss -> BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Hide))
            is BottomSheetOperation.Requested -> BottomSheetState(null, Effect.of(BottomSheetVisibilityEffect.Show))

            is BlockedTrackersBottomSheetEvent.Ready ->
                BottomSheetState(
                    contentState = BlockedElementsSheetState.Requested(operation.elements),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )

            is EncryptionInfoBottomSheetEvent.Ready ->
                BottomSheetState(
                    contentState = EncryptionInfoSheetState.Requested(operation.uiModel),
                    bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
                )

            else -> null
        }
    }
}
