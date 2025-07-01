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
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.DialogState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import javax.inject.Inject

class MailboxReducer @Inject constructor(
    private val mailboxListReducer: MailboxListReducer,
    private val topAppBarReducer: MailboxTopAppBarReducer,
    private val unreadFilterReducer: MailboxUnreadFilterReducer,
    private val bottomAppBarReducer: BottomBarReducer,
    private val storageLimitReducer: StorageLimitReducer,
    private val upgradeStorageReducer: UpgradeStorageReducer,
    private val actionMessageReducer: MailboxActionMessageReducer,
    private val deleteDialogReducer: MailboxDeleteDialogReducer,
    private val bottomSheetReducer: BottomSheetReducer
) {

    internal fun newStateFrom(currentState: MailboxState, operation: MailboxOperation): MailboxState =
        currentState.copy(
            mailboxListState = currentState.toNewMailboxListStateFrom(operation),
            topAppBarState = currentState.toNewTopAppBarStateFrom(operation),
            upgradeStorageState = currentState.toNewStorageSplitStateFrom(operation),
            unreadFilterState = currentState.toNewUnreadFilterStateFrom(operation),
            bottomAppBarState = currentState.toNewBottomAppBarStateFrom(operation),
            storageLimitState = currentState.toNewStorageLimitStateFrom(operation),
            deleteDialogState = currentState.toNewDeleteActionStateFrom(operation),
            deleteAllDialogState = currentState.toNewDeleteAllActionStateFrom(operation),
            bottomSheetState = currentState.toNewBottomSheetState(operation),
            actionResult = currentState.toNewActionMessageStateFrom(operation),
            error = currentState.toNewErrorBarState(operation),
            showRatingBooster = currentState.toNewShowRatingBoosterState(operation),
            showNPSFeedback = currentState.toNewShowNPSFeedbackState(operation),
            autoDeleteSettingState = currentState.toNewAutoDeleteSettingState(operation)
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
                is MailboxEvent.MessageBottomBarEvent -> operation.bottomBarEvent
                is MailboxEvent.DeleteConfirmed,
                is MailboxEvent.Trash,
                is MailboxViewAction.ExitSearchMode,
                is MailboxViewAction.MoveToConfirmed,
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam,
                is MailboxViewAction.ExitSelectionMode -> BottomBarEvent.HideBottomSheet
            }
            bottomAppBarReducer.newStateFrom(bottomAppBarState, bottomBarOperation)
        } else {
            bottomAppBarState
        }
    }

    private fun MailboxState.toNewStorageLimitStateFrom(operation: MailboxOperation): StorageLimitState {
        return if (operation is MailboxOperation.AffectingStorageLimit) {
            storageLimitReducer.newStateFrom(this.storageLimitState, operation)
        } else {
            storageLimitState
        }
    }

    private fun MailboxState.toNewStorageSplitStateFrom(operation: MailboxOperation): UpgradeStorageState {
        return if (operation is MailboxOperation.AffectingUpgradeStorage) {
            upgradeStorageReducer.newStateFrom(operation)
        } else {
            upgradeStorageState
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

    private fun MailboxState.toNewDeleteAllActionStateFrom(operation: MailboxOperation): DeleteDialogState {
        return if (operation is MailboxOperation.AffectingClearDialog) {
            when (operation) {
                is MailboxEvent.DeleteAll ->
                    when (operation.location) {
                        SystemLabelId.Trash.labelId -> DeleteDialogState.Shown(
                            title = TextUiModel(R.string.mailbox_action_clear_trash_dialog_title),
                            message = TextUiModel(R.string.mailbox_action_clear_trash_dialog_body_message)
                        )

                        SystemLabelId.Spam.labelId -> DeleteDialogState.Shown(
                            title = TextUiModel(R.string.mailbox_action_clear_spam_dialog_title),
                            message = TextUiModel(R.string.mailbox_action_clear_spam_dialog_body_message)
                        )

                        else -> DeleteDialogState.Hidden
                    }

                is MailboxEvent.DeleteAllConfirmed,
                MailboxViewAction.DeleteAllDialogDismissed -> DeleteDialogState.Hidden
            }
        } else {
            deleteAllDialogState
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
                is MailboxViewAction.RequestUpsellingBottomSheet -> BottomSheetOperation.Requested

                is MailboxViewAction.LabelAsToggleAction -> LabelToggled(operation.label)
                is MailboxEvent.ErrorRetrievingCustomMailLabels,
                is MailboxEvent.ErrorRetrievingFolderColorSettings,
                is MailboxEvent.ErrorRetrievingDestinationMailFolders,
                is MailboxEvent.Trash,
                is MailboxViewAction.MoveToConfirmed,
                is MailboxViewAction.LabelAsConfirmed,
                is MailboxViewAction.Star,
                is MailboxViewAction.UnStar,
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam,
                is MailboxViewAction.MarkAsRead,
                is MailboxViewAction.MarkAsUnread,
                is MailboxEvent.DeleteConfirmed,
                is MailboxViewAction.DismissBottomSheet -> BottomSheetOperation.Dismiss

                is MailboxViewAction.MoveToDestinationSelected -> MoveToDestinationSelected(operation.mailLabelId)
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
            }
            Effect.of(TextUiModel(textResource))
        } else {
            error
        }
    }

    private fun MailboxState.toNewShowRatingBoosterState(operation: MailboxOperation): Effect<Unit> {
        return if (operation is MailboxOperation.AffectingRatingBooster) {
            Effect.of(Unit)
        } else {
            showRatingBooster
        }
    }

    private fun MailboxState.toNewShowNPSFeedbackState(operation: MailboxOperation): Effect<Unit> {
        return if (operation is MailboxOperation.AffectingNPSFeedback) {
            Effect.of(Unit)
        } else {
            showNPSFeedback
        }
    }

    private fun MailboxState.toNewAutoDeleteSettingState(operation: MailboxOperation): AutoDeleteSettingState {
        return if (operation is MailboxOperation.AffectingAutoDelete) {
            when (operation) {
                is MailboxViewAction.AutoDeleteDialogActionSubmitted -> AutoDeleteSettingState.Data(
                    enablingDialogState = DialogState.Hidden
                )

                MailboxViewAction.DismissAutoDelete -> AutoDeleteSettingState.Data(
                    enablingDialogState = DialogState.Hidden
                )

                MailboxViewAction.ShowAutoDeleteDialog -> AutoDeleteSettingState.Data(
                    enablingDialogState = DialogState.Shown(
                        title = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_title),
                        message = TextUiModel(R.string.mail_settings_auto_delete_dialog_enabling_text),
                        dismissButtonText = TextUiModel(R.string.mail_settings_auto_delete_dialog_button_cancel),
                        confirmButtonText = TextUiModel(
                            R.string.mail_settings_auto_delete_dialog_enabling_button_confirm
                        )
                    )
                )

                is MailboxEvent.AutoDeleteStateChanged -> AutoDeleteSettingState.Data(
                    isEnabled = operation.isFeatureFlagEnabled &&
                        operation.autoDeleteSetting is AutoDeleteSetting.Enabled
                )
            }
        } else {
            autoDeleteSettingState
        }
    }
}
