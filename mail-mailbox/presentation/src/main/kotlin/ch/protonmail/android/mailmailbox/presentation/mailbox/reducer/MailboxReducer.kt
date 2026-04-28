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

import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetOperation
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState.Enabled
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState.Disabled
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import javax.inject.Inject

class MailboxReducer @Inject constructor(
    private val mailboxListReducer: MailboxListReducer,
    private val topAppBarReducer: MailboxTopAppBarReducer,
    private val unreadFilterReducer: MailboxUnreadFilterReducer,
    private val categoryViewReducer: MailboxCategoryViewReducer,
    private val showSpamTrashFilterReducer: MailboxShowSpamTrashFilterReducer,
    private val bottomAppBarReducer: BottomBarReducer,
    private val actionMessageReducer: MailboxActionMessageReducer,
    private val deleteDialogReducer: MailboxDeleteDialogReducer,
    private val clearAllDialogReducer: MailboxClearAllDialogReducer,
    private val bottomSheetReducer: BottomSheetReducer
) {

    internal fun newStateFrom(currentState: MailboxState, operation: MailboxOperation): MailboxState =
        currentState.copy(
            mailboxListState = currentState.toNewMailboxListStateFrom(operation),
            topAppBarState = currentState.toNewTopAppBarStateFrom(operation),
            unreadFilterState = currentState.toNewUnreadFilterStateFrom(operation),
            categoryViewState = currentState.toNewCategoryViewStateFrom(operation),
            showSpamTrashIncludeFilterState = currentState.toNewShowSpamTrashFilterStateFrom(operation),
            bottomAppBarState = currentState.toNewBottomAppBarStateFrom(operation),
            deleteDialogState = currentState.toNewDeleteActionStateFrom(operation),
            clearAllDialogState = currentState.toNewClearAllDialogStateFrom(operation),
            bottomSheetState = currentState.toNewBottomSheetState(operation),
            actionResult = currentState.toNewActionMessageStateFrom(operation),
            composerNavigationState = currentState.toComposerNavigationState(operation),
            error = currentState.toNewErrorBarState(operation),
            showRatingBooster = currentState.toNewShowRatingBoosterState(operation)
        )

    private fun MailboxState.toNewCategoryViewStateFrom(operation: MailboxOperation): CategoryViewState {

        return if (operation is MailboxOperation.AffectingCategoryView) {
            categoryViewReducer.newStateFrom(operation)
        } else {
            categoryViewState
        }
    }

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

    private fun MailboxState.toNewShowSpamTrashFilterStateFrom(
        operation: MailboxOperation
    ): ShowSpamTrashIncludeFilterState {
        return if (operation is MailboxOperation.AffectingShowSpamTrashFilter) {
            showSpamTrashFilterReducer.newStateFrom(showSpamTrashIncludeFilterState, operation)
        } else {
            showSpamTrashIncludeFilterState
        }
    }

    private fun MailboxState.toNewBottomAppBarStateFrom(operation: MailboxOperation): BottomBarState {
        return if (operation is MailboxOperation.AffectingBottomAppBar) {
            val bottomBarOperation = when (operation) {
                is MailboxEvent.EnterSelectionMode -> BottomBarEvent.ShowBottomSheet
                is MailboxEvent.MessageBottomBarEvent -> operation.bottomBarEvent
                is MailboxEvent.DeleteConfirmed,
                is MailboxViewAction.ExitSearchMode,
                is MailboxEvent.MoveToConfirmed,
                is MailboxEvent.LabelAsConfirmed,
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam,
                is MailboxViewAction.MoveToInbox,
                MailboxViewAction.SnoozeDismissed,
                is MailboxViewAction.ExitSelectionMode -> BottomBarEvent.HideBottomSheet
            }
            bottomAppBarReducer.newStateFrom(bottomAppBarState, bottomBarOperation)
        } else {
            bottomAppBarState
        }
    }

    private fun MailboxState.toNewActionMessageStateFrom(operation: MailboxOperation): Effect<ActionResult> {
        return if (operation is MailboxOperation.AffectingActionMessage) {
            actionMessageReducer.newStateFrom(operation)
        } else {
            actionResult
        }
    }

    private fun MailboxState.toNewDeleteActionStateFrom(operation: MailboxOperation): DeleteDialogState {
        return if (operation is MailboxOperation.AffectingDeleteDialog) {
            deleteDialogReducer.newStateFrom(operation)
        } else {
            deleteDialogState
        }
    }

    private fun MailboxState.toNewClearAllDialogStateFrom(operation: MailboxOperation): DeleteDialogState {
        return if (operation is MailboxOperation.AffectingClearAllDialog) {
            clearAllDialogReducer.newStateFrom(operation)
        } else {
            clearAllDialogState
        }
    }

    private fun MailboxState.toNewBottomSheetState(operation: MailboxOperation): BottomSheetState? {
        return if (operation is MailboxOperation.AffectingBottomSheet) {
            val bottomSheetOperation = when (operation) {
                is MailboxEvent.MailboxBottomSheetEvent -> operation.bottomSheetOperation
                is MailboxViewAction.RequestMoreActionsBottomSheet,
                is MailboxViewAction.RequestMoveToBottomSheet,
                is MailboxViewAction.RequestLabelAsBottomSheet,
                is MailboxViewAction.SwipeLabelAsAction,
                is MailboxViewAction.SwipeMoveToAction,
                is MailboxViewAction.RequestSnoozeBottomSheet,
                is MailboxViewAction.RequestManageAccountsBottomSheet -> BottomSheetOperation.Requested

                is MailboxEvent.ErrorRetrievingCustomMailLabels,
                is MailboxEvent.ErrorRetrievingFolderColorSettings,
                is MailboxEvent.ErrorRetrievingDestinationMailFolders,
                is MailboxEvent.DeleteConfirmed,
                is MailboxEvent.ErrorDeleting,
                is MailboxEvent.MoveToConfirmed,
                is MailboxEvent.LabelAsConfirmed,
                is MailboxViewAction.Star,
                is MailboxViewAction.UnStar,
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam,
                is MailboxViewAction.MoveToInbox,
                is MailboxViewAction.MarkAsRead,
                is MailboxViewAction.MarkAsUnread,
                is MailboxViewAction.SnoozeDismissed,
                is MailboxViewAction.DismissBottomSheet -> BottomSheetOperation.Dismiss
            }
            bottomSheetReducer.newStateFrom(bottomSheetState, bottomSheetOperation)
        } else {
            bottomSheetState
        }
    }

    private fun MailboxState.toNewErrorBarState(operation: MailboxOperation): Effect<TextUiModel> {
        return if (operation is MailboxOperation.AffectingErrorBar) {
            val textResource = when (operation) {
                is MailboxEvent.ErrorLabeling -> R.string.mailbox_action_label_messages_failed
                is MailboxEvent.ErrorRetrievingCustomMailLabels ->
                    R.string.mailbox_action_label_messages_failed_retrieving_labels

                is MailboxEvent.ErrorMoving -> R.string.mailbox_action_move_messages_failed
                is MailboxEvent.ErrorRetrievingDestinationMailFolders -> R.string.mailbox_action_move_messages_failed
                is MailboxEvent.ErrorRetrievingFolderColorSettings ->
                    R.string.mailbox_action_failed_retrieving_colors

                is MailboxEvent.ErrorDeleting -> R.string.mailbox_action_delete_failed
                is MailboxEvent.ErrorComposing -> R.string.mailbox_action_no_sender_addresses
            }
            Effect.of(TextUiModel(textResource))
        } else {
            error
        }
    }

    private fun MailboxState.toComposerNavigationState(operation: MailboxOperation): MailboxComposerNavigationState {
        val affectingOperation = operation as? MailboxOperation.AffectingComposer
            ?: return this.composerNavigationState

        return when (affectingOperation) {
            is MailboxEvent.SenderHasValidAddressUpdated -> {
                if (affectingOperation.isValid) {
                    Enabled()
                } else {
                    Disabled
                }
            }

            is MailboxEvent.NavigateToComposer -> {
                (this.composerNavigationState as? Enabled)
                    ?.copy(navigateToCompose = Effect.of(Unit))
                    ?: this.composerNavigationState
            }
        }
    }

    private fun MailboxState.toNewShowRatingBoosterState(operation: MailboxOperation): Effect<Unit> {
        return if (operation is MailboxEvent.ShowRatingBooster) {
            Effect.of(Unit)
        } else {
            showRatingBooster
        }
    }
}
