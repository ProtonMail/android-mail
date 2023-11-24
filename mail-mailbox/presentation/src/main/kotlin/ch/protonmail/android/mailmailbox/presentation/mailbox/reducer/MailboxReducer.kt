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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import javax.inject.Inject

class MailboxReducer @Inject constructor(
    private val mailboxListReducer: MailboxListReducer,
    private val topAppBarReducer: MailboxTopAppBarReducer,
    private val unreadFilterReducer: MailboxUnreadFilterReducer,
    private val bottomAppBarReducer: BottomBarReducer,
    private val onboardingReducer: OnboardingReducer,
    private val actionMessageReducer: MailboxActionMessageReducer,
    private val deleteDialogReducer: MailboxDeleteDialogReducer,
    private val bottomSheetReducer: BottomSheetReducer
) {

    internal fun newStateFrom(currentState: MailboxState, operation: MailboxOperation): MailboxState =
        currentState.copy(
            mailboxListState = currentState.toNewMailboxListStateFrom(operation),
            topAppBarState = currentState.toNewTopAppBarStateFrom(operation),
            unreadFilterState = currentState.toNewUnreadFilterStateFrom(operation),
            bottomAppBarState = currentState.toNewBottomAppBarStateFrom(operation),
            onboardingState = currentState.toNewOnboardingStateFrom(operation),
            deleteDialogState = currentState.toNewDeleteActionStateFrom(operation),
            bottomSheetState = currentState.toNewBottomSheetState(operation),
            actionMessage = currentState.toNewActionMessageStateFrom(operation),
            error = currentState.toNewErrorBarState(operation)
        )

    private fun MailboxState.toNewMailboxListStateFrom(operation: MailboxOperation): MailboxListState {
        return if (operation is MailboxOperation.AffectingMailboxList) {
            mailboxListReducer.newStateFrom(mailboxListState, operation)
        } else {
            mailboxListState
        }
    }

    private fun MailboxState.toNewTopAppBarStateFrom(operation: MailboxOperation): MailboxTopAppBarState {
        return if (operation is MailboxOperation.AffectingTopAppBar) {
            topAppBarReducer.newStateFrom(topAppBarState, operation)
        } else {
            topAppBarState
        }
    }

    private fun MailboxState.toNewUnreadFilterStateFrom(operation: MailboxOperation): UnreadFilterState {
        return if (operation is MailboxOperation.AffectingUnreadFilter) {
            unreadFilterReducer.newStateFrom(unreadFilterState, operation)
        } else {
            unreadFilterState
        }
    }

    private fun MailboxState.toNewBottomAppBarStateFrom(operation: MailboxOperation): BottomBarState {
        return if (operation is MailboxOperation.AffectingBottomAppBar) {
            val bottomBarOperation = when (operation) {
                is MailboxEvent.EnterSelectionMode -> BottomBarEvent.ShowBottomSheet
                is MailboxViewAction.ExitSelectionMode -> BottomBarEvent.HideBottomSheet
                is MailboxEvent.MessageBottomBarEvent -> operation.bottomBarEvent
                is MailboxEvent.DeleteConfirmed,
                is MailboxEvent.Trash -> BottomBarEvent.HideBottomSheet
            }
            bottomAppBarReducer.newStateFrom(bottomAppBarState, bottomBarOperation)
        } else {
            bottomAppBarState
        }
    }

    private fun MailboxState.toNewOnboardingStateFrom(operation: MailboxOperation): OnboardingState {
        return if (operation is MailboxOperation.AffectingOnboarding) {
            onboardingReducer.newStateFrom(operation)
        } else {
            onboardingState
        }
    }

    private fun MailboxState.toNewActionMessageStateFrom(operation: MailboxOperation): Effect<TextUiModel> {
        return if (operation is MailboxOperation.AffectingActionMessage) {
            actionMessageReducer.newStateFrom(operation)
        } else {
            actionMessage
        }
    }

    private fun MailboxState.toNewDeleteActionStateFrom(operation: MailboxOperation): DeleteDialogState {
        return if (operation is MailboxOperation.AffectingDeleteDialog) {
            deleteDialogReducer.newStateFrom(operation)
        } else {
            deleteDialogState
        }
    }

    private fun MailboxState.toNewBottomSheetState(operation: MailboxOperation): BottomSheetState? {
        return if (operation is MailboxOperation.AffectingBottomSheet) {
            val bottomSheetOperation = when (operation) {
                is MailboxEvent.MailboxBottomSheetEvent -> operation.bottomSheetOperation
                is MailboxViewAction.RequestMoveToBottomSheet,
                is MailboxViewAction.RequestLabelAsBottomSheet -> BottomSheetOperation.Requested

                is MailboxViewAction.LabelAsToggleAction -> LabelToggled(operation.label)
                is MailboxEvent.ErrorRetrievingCustomMailLabels,
                is MailboxEvent.ErrorRetrievingFolderColorSettings,
                is MailboxViewAction.LabelAsConfirmed -> BottomSheetOperation.Dismiss
                is MailboxViewAction.MoveToDestinationSelected -> MoveToDestinationSelected(operation.mailLabelId)
                is MailboxViewAction.MoveToConfirmed -> BottomSheetOperation.Dismiss
            }
            bottomSheetReducer.newStateFrom(bottomSheetState, bottomSheetOperation)
        } else {
            bottomSheetState
        }
    }

    private fun MailboxState.toNewErrorBarState(operation: MailboxOperation): Effect<TextUiModel> {
        return if (operation is MailboxOperation.AffectingErrorBar) {
            when (operation) {
                is MailboxEvent.ErrorLabeling ->
                    Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed))

                MailboxEvent.ErrorRetrievingCustomMailLabels ->
                    Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed_retrieving_labels))
                MailboxEvent.ErrorRetrievingFolderColorSettings ->
                    Effect.of(TextUiModel(R.string.mailbox_action_label_messages_failed))

                MailboxEvent.ErrorMoving ->
                    Effect.of(TextUiModel(R.string.mailbox_action_move_messages_failed))
            }
        } else {
            error
        }
    }
}
