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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import javax.inject.Inject

class MessageDetailReducer @Inject constructor(
    private val messageMetadataReducer: MessageDetailMetadataReducer,
    private val messageBodyReducer: MessageBodyReducer,
    private val bottomBarReducer: BottomBarReducer,
    private val bottomSheetReducer: BottomSheetReducer,
    private val deleteDialogReducer: MessageDeleteDialogReducer
) {

    fun newStateFrom(currentState: MessageDetailState, operation: MessageDetailOperation): MessageDetailState =
        currentState.copy(
            messageMetadataState = currentState.toNewMessageStateFrom(operation),
            messageBodyState = currentState.toNewMessageBodyStateFrom(operation),
            bottomBarState = currentState.toNewBottomBarStateFrom(operation),
            error = currentState.toNewErrorStateFrom(operation),
            bottomSheetState = currentState.toNewBottomSheetStateFrom(operation),
            exitScreenEffect = currentState.toNewExitStateFrom(operation),
            exitScreenWithMessageEffect = currentState.toNewExitWithMessageStateFrom(operation),
            openMessageBodyLinkEffect = currentState.toNewOpenMessageBodyLinkStateFrom(operation),
            openAttachmentEffect = currentState.toNewOpenAttachmentStateFrom(operation),
            deleteDialogState = currentState.toNewDeleteDialogStateFrom(operation)
        )

    private fun MessageDetailState.toNewErrorStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingErrorBar) {
            when (operation) {
                is MessageDetailEvent.ErrorMarkingUnread -> Effect.of(TextUiModel(R.string.error_mark_unread_failed))
                is MessageDetailEvent.ErrorAddingStar -> Effect.of(TextUiModel(R.string.error_star_operation_failed))
                is MessageDetailEvent.ErrorRemovingStar ->
                    Effect.of(TextUiModel(R.string.error_unstar_operation_failed))

                is MessageDetailEvent.ErrorMovingToTrash -> Effect.of(TextUiModel(R.string.error_move_to_trash_failed))
                is MessageDetailEvent.ErrorMovingMessage -> Effect.of(TextUiModel(R.string.error_move_message_failed))
                is MessageDetailEvent.ErrorLabelingMessage ->
                    Effect.of(TextUiModel(R.string.error_relabel_message_failed))

                is MessageDetailEvent.ErrorGettingAttachment ->
                    Effect.of(TextUiModel(R.string.error_get_attachment_failed))

                is MessageDetailEvent.ErrorGettingAttachmentNotEnoughSpace ->
                    Effect.of(TextUiModel(R.string.error_get_attachment_not_enough_memory))

                MessageDetailEvent.ErrorAttachmentDownloadInProgress ->
                    Effect.of(TextUiModel(R.string.error_attachment_download_in_progress))
            }
        } else {
            error
        }

    private fun MessageDetailState.toNewExitStateFrom(operation: MessageDetailOperation): Effect<Unit> =
        when (operation) {
            MessageViewAction.MarkUnread -> Effect.of(Unit)
            else -> exitScreenEffect
        }

    private fun MessageDetailState.toNewExitWithMessageStateFrom(
        operation: MessageDetailOperation
    ): Effect<TextUiModel> = when (operation) {
        MessageViewAction.Trash -> Effect.of(TextUiModel(R.string.message_moved_to_trash))
        is MessageViewAction.MoveToDestinationConfirmed -> Effect.of(
            TextUiModel(R.string.message_moved_to_selected_destination, operation.mailLabelText)
        )

        is MessageViewAction.LabelAsConfirmed -> when (operation.archiveSelected) {
            true -> Effect.of(TextUiModel(R.string.message_moved_to_archive))
            else -> exitScreenWithMessageEffect
        }

        else -> exitScreenWithMessageEffect
    }

    private fun MessageDetailState.toNewMessageStateFrom(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingMessage) {
            messageMetadataReducer.newStateFrom(messageMetadataState, operation)
        } else {
            messageMetadataState
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
                is MessageViewAction.RequestMoveToBottomSheet -> BottomSheetOperation.Requested

                is MessageViewAction.LabelAsConfirmed,
                is MessageViewAction.DismissBottomSheet -> BottomSheetOperation.Dismiss
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

    private fun MessageDetailState.toNewDeleteDialogStateFrom(operation: MessageDetailOperation): DeleteDialogState {
        return if (operation is MessageDetailOperation.AffectingDeleteDialog) {
            deleteDialogReducer.newStateFrom(operation)
        } else {
            deleteDialogState
        }
    }
}
