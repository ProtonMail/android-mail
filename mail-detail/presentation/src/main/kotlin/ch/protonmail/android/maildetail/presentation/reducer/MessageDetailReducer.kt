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

package ch.protonmail.android.maildetail.presentation.reducer

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.DefinitiveActionResult
import ch.protonmail.android.mailcommon.presentation.model.ActionResult.UndoableActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltipState
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import javax.inject.Inject

class MessageDetailReducer @Inject constructor(
    private val messageMetadataReducer: MessageDetailMetadataReducer,
    private val messageBannersReducer: MessageBannersReducer,
    private val messageBodyReducer: MessageBodyReducer,
    private val bottomBarReducer: BottomBarReducer,
    private val bottomSheetReducer: BottomSheetReducer,
    private val deleteDialogReducer: MessageDeleteDialogReducer,
    private val requestPhishingDialogReducer: MessageReportPhishingDialogReducer,
    private val mailLabelTextMapper: MailLabelTextMapper,
    private val customizeToolbarSpotlightReducer: MessageCustomizeToolbarSpotlightReducer
) {

    suspend fun newStateFrom(currentState: MessageDetailState, operation: MessageDetailOperation): MessageDetailState =
        currentState.copy(
            messageMetadataState = currentState.toNewMessageStateFrom(operation),
            messageBannersState = currentState.toNewBannersStateFrom(operation),
            messageBodyState = currentState.toNewMessageBodyStateFrom(operation),
            bottomBarState = currentState.toNewBottomBarStateFrom(operation),
            error = currentState.toNewErrorStateFrom(operation),
            bottomSheetState = currentState.toNewBottomSheetStateFrom(operation),
            exitScreenEffect = currentState.toNewExitStateFrom(operation),
            exitScreenWithMessageEffect = currentState.toNewExitWithMessageStateFrom(operation),
            openMessageBodyLinkEffect = currentState.toNewOpenMessageBodyLinkStateFrom(operation),
            openAttachmentEffect = currentState.toNewOpenAttachmentStateFrom(operation),
            openProtonCalendarIntent = currentState.toNewOpenProtonCalendarIntentFrom(operation),
            deleteDialogState = currentState.toNewDeleteDialogStateFrom(operation),
            requestPhishingLinkConfirmation = currentState.toNewPhishingLinkConfirmationState(operation),
            reportPhishingDialogState = currentState.toNewReportPhishingDialogStateFrom(operation),
            spotlightTooltip = currentState.toNewSpotlightState(operation)
        )

    private fun MessageDetailState.toNewErrorStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingErrorBar) {
            val textResource = when (operation) {
                is MessageDetailEvent.ErrorMarkingUnread -> R.string.error_mark_unread_failed
                is MessageDetailEvent.ErrorAddingStar -> R.string.error_star_operation_failed
                is MessageDetailEvent.ErrorRemovingStar -> R.string.error_unstar_operation_failed
                is MessageDetailEvent.ErrorMovingToTrash -> R.string.error_move_to_trash_failed
                is MessageDetailEvent.ErrorMovingMessage -> R.string.error_move_message_failed
                is MessageDetailEvent.ErrorLabelingMessage -> R.string.error_relabel_message_failed
                is MessageDetailEvent.ErrorGettingAttachment -> R.string.error_get_attachment_failed
                is MessageDetailEvent.ErrorGettingAttachmentNotEnoughSpace ->
                    R.string.error_get_attachment_not_enough_memory

                is MessageDetailEvent.ErrorAttachmentDownloadInProgress ->
                    R.string.error_attachment_download_in_progress

                is MessageDetailEvent.ErrorDeletingMessage -> R.string.error_delete_message_failed
                is MessageDetailEvent.ErrorDeletingNoApplicableFolder ->
                    R.string.error_delete_message_failed_wrong_folder

                is MessageDetailEvent.ErrorMovingToArchive -> R.string.error_move_to_archive_failed
                is MessageDetailEvent.ErrorMovingToSpam -> R.string.error_move_to_spam_failed
            }
            Effect.of(TextUiModel(textResource))
        } else {
            error
        }

    private fun MessageDetailState.toNewExitStateFrom(operation: MessageDetailOperation): Effect<Unit> =
        when (operation) {
            MessageDetailEvent.NoCachedMetadata,
            MessageViewAction.MarkUnread,
            MessageViewAction.ReportPhishingConfirmed -> Effect.of(Unit)

            else -> exitScreenEffect
        }

    private fun MessageDetailState.toNewExitWithMessageStateFrom(
        operation: MessageDetailOperation
    ): Effect<ActionResult> = when (operation) {
        MessageViewAction.Trash -> Effect.of(
            UndoableActionResult(TextUiModel(R.string.message_moved_to_trash))
        )

        is MessageViewAction.MoveToDestinationConfirmed -> Effect.of(
            UndoableActionResult(
                TextUiModel(
                    R.string.message_moved_to_selected_destination,
                    mailLabelTextMapper.mapToString(operation.mailLabelText)
                )
            )
        )

        is MessageViewAction.LabelAsConfirmed -> when (operation.archiveSelected) {
            true -> Effect.of(DefinitiveActionResult(TextUiModel(R.string.message_moved_to_archive)))
            else -> exitScreenWithMessageEffect
        }

        is MessageViewAction.DeleteConfirmed -> Effect.of(
            DefinitiveActionResult(TextUiModel(R.string.message_deleted))
        )

        is MessageViewAction.Archive -> Effect.of(UndoableActionResult(TextUiModel(R.string.message_moved_to_archive)))
        is MessageViewAction.Spam -> Effect.of(UndoableActionResult(TextUiModel(R.string.message_moved_to_spam)))

        else -> exitScreenWithMessageEffect
    }

    private suspend fun MessageDetailState.toNewMessageStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingMessage) {
            messageMetadataReducer.newStateFrom(messageMetadataState, operation)
        } else {
            messageMetadataState
        }

    private fun MessageDetailState.toNewBannersStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingMessageBanners) {
            messageBannersReducer.newStateFrom(operation)
        } else {
            messageBannersState
        }

    private fun MessageDetailState.toNewMessageBodyStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingMessageBody) {
            messageBodyReducer.newStateFrom(messageBodyState, operation)
        } else {
            messageBodyState
        }

    private fun MessageDetailState.toNewBottomBarStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailEvent.MessageBottomBarEvent) {
            bottomBarReducer.newStateFrom(bottomBarState, operation.bottomBarEvent)
        } else {
            bottomBarState
        }

    private fun MessageDetailState.toNewBottomSheetStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingBottomSheet) {
            val bottomSheetOperation = when (operation) {
                is MessageDetailEvent.MessageBottomSheetEvent -> operation.bottomSheetOperation
                is MessageViewAction.MoveToDestinationSelected -> MoveToDestinationSelected(operation.mailLabelId)
                is MessageViewAction.LabelAsToggleAction -> LabelToggled(operation.labelId)
                is MessageViewAction.RequestLabelAsBottomSheet,
                is MessageViewAction.RequestMoveToBottomSheet,
                is MessageViewAction.RequestContactActionsBottomSheet,
                is MessageViewAction.RequestMoreActionsBottomSheet -> BottomSheetOperation.Requested

                is MessageViewAction.LabelAsConfirmed,
                is MessageDetailEvent.ReportPhishingRequested,
                is MessageViewAction.DismissBottomSheet,
                is MessageViewAction.SwitchViewMode,
                is MessageViewAction.DeleteConfirmed,
                is MessageViewAction.PrintRequested -> BottomSheetOperation.Dismiss
            }
            bottomSheetReducer.newStateFrom(bottomSheetState, bottomSheetOperation)
        } else {
            bottomSheetState
        }

    private fun MessageDetailState.toNewOpenMessageBodyLinkStateFrom(operation: MessageDetailOperation): Effect<Uri> =
        when (operation) {
            is MessageViewAction.MessageBodyLinkClicked -> Effect.of(operation.uri)
            else -> openMessageBodyLinkEffect
        }

    private fun MessageDetailState.toNewOpenAttachmentStateFrom(
        operation: MessageDetailOperation
    ): Effect<OpenAttachmentIntentValues> = when (operation) {
        is MessageDetailEvent.OpenAttachmentEvent -> Effect.of(operation.values)
        else -> openAttachmentEffect
    }

    private fun MessageDetailState.toNewOpenProtonCalendarIntentFrom(
        operation: MessageDetailOperation
    ): Effect<OpenProtonCalendarIntentValues> = when (operation) {
        is MessageDetailEvent.HandleOpenProtonCalendarRequest -> Effect.of(operation.intent)
        else -> openProtonCalendarIntent
    }

    private fun MessageDetailState.toNewDeleteDialogStateFrom(operation: MessageDetailOperation): DeleteDialogState {
        return if (operation is MessageDetailOperation.AffectingDeleteDialog) {
            deleteDialogReducer.newStateFrom(operation)
        } else {
            deleteDialogState
        }
    }

    private fun MessageDetailState.toNewPhishingLinkConfirmationState(operation: MessageDetailOperation): Boolean =
        when (operation) {
            is MessageDetailEvent.MessageWithLabelsEvent -> operation.messageWithLabels.message.isPhishing()
            else -> requestPhishingLinkConfirmation
        }

    private fun MessageDetailState.toNewReportPhishingDialogStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingReportPhishingDialog) {
            requestPhishingDialogReducer.newStateFrom(operation)
        } else {
            reportPhishingDialogState
        }

    private fun MessageDetailState.toNewSpotlightState(operation: MessageDetailOperation): SpotlightTooltipState {
        return if (operation is MessageDetailOperation.AffectingSpotlight) {
            val canShow = bottomSheetState == null && bottomBarState is BottomBarState.Data
            if (canShow) {
                customizeToolbarSpotlightReducer.newStateFrom(operation)
            } else {
                spotlightTooltip
            }
        } else {
            spotlightTooltip
        }
    }
}
