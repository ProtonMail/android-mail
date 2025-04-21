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
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ConversationBottomBarEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ConversationBottomSheetEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorAddStar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorAttachmentDownloadInProgress
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorDeletingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorDeletingNoApplicableFolder
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorExpandingDecryptMessageError
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorExpandingRetrieveMessageError
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorGettingAttachment
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorGettingAttachmentNotEnoughSpace
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorLabelingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorMarkingAsUnread
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorMovingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorMovingMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorMovingToTrash
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.ErrorRemoveStar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.MessagesData
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.OpenAttachmentEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingDeleteDialog
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.MessageBodyLink
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maildetail.presentation.model.TrashedMessagesBannerState
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState.LabelAsBottomSheetAction.LabelToggled
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState.MoveToBottomSheetAction.MoveToDestinationSelected
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import javax.inject.Inject

class ConversationDetailReducer @Inject constructor(
    private val bottomBarReducer: BottomBarReducer,
    private val metadataReducer: ConversationDetailMetadataReducer,
    private val messagesReducer: ConversationDetailMessagesReducer,
    private val bottomSheetReducer: BottomSheetReducer,
    private val deleteDialogReducer: ConversationDeleteDialogReducer,
    private val reportPhishingDialogReducer: ConversationReportPhishingDialogReducer,
    private val trashedMessagesBannerReducer: TrashedMessagesBannerReducer,
    private val mailLabelTextMapper: MailLabelTextMapper,
    private val customizeToolbarSpotlightReducer: ConversationCustomizeToolbarSpotlightReducer
) {

    fun newStateFrom(
        currentState: ConversationDetailState,
        operation: ConversationDetailOperation
    ): ConversationDetailState {
        return currentState.copy(
            conversationState = currentState.toNewConversationState(operation),
            messagesState = currentState.toNewMessageState(operation),
            bottomBarState = currentState.toNewBottomBarState(operation),
            bottomSheetState = currentState.toNewBottomSheetStateFrom(operation),
            error = currentState.toErrorState(operation),
            message = currentState.toMessageState(operation),
            exitScreenEffect = currentState.toExitState(operation),
            exitScreenWithMessageEffect = currentState.toExitWithMessageState(operation),
            openMessageBodyLinkEffect = currentState.toOpenMessageBodyLinkState(operation),
            openAttachmentEffect = currentState.toNewOpenAttachmentStateFrom(operation),
            openProtonCalendarIntent = currentState.toNewOpenProtonCalendarIntentFrom(operation),
            openReply = currentState.toOpenReplyFrom(operation),
            openReplyAll = currentState.toOpenReplyAllFrom(operation),
            openForward = currentState.toOpenForwardFrom(operation),
            scrollToMessage = currentState.toScrollToMessageState(operation),
            deleteDialogState = currentState.toNewDeleteDialogState(operation),
            reportPhishingDialogState = currentState.toNewReportPhishingDialogState(operation),
            trashedMessagesBannerState = currentState.toNewTrashedMessagesBannerState(operation),
            spotlightTooltip = currentState.toNewSpotlightState(operation)
        )
    }

    private fun ConversationDetailState.toNewConversationState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingConversation) {
            metadataReducer.newStateFrom(conversationState, operation)
        } else {
            conversationState
        }

    private fun ConversationDetailState.toNewMessageState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingMessages) {
            messagesReducer.newStateFrom(messagesState, operation)
        } else {
            messagesState
        }

    private fun ConversationDetailState.toNewBottomBarState(operation: ConversationDetailOperation) =
        if (operation is ConversationBottomBarEvent) {
            bottomBarReducer.newStateFrom(bottomBarState, operation.bottomBarEvent)
        } else {
            bottomBarState
        }

    private fun ConversationDetailState.toNewBottomSheetStateFrom(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingBottomSheet) {
            val bottomSheetOperation = when (operation) {
                is ConversationBottomSheetEvent -> operation.bottomSheetOperation
                is ConversationDetailEvent.MessageBottomSheetEvent -> operation.bottomSheetOperation
                is ConversationDetailViewAction.MoveToDestinationSelected ->
                    MoveToDestinationSelected(operation.mailLabelId)

                is ConversationDetailViewAction.LabelAsToggleAction -> LabelToggled(operation.labelId)
                is ConversationDetailViewAction.RequestConversationLabelAsBottomSheet,
                is ConversationDetailViewAction.RequestMoreActionsBottomSheet,
                is ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet,
                is ConversationDetailViewAction.RequestContactActionsBottomSheet,
                is ConversationDetailViewAction.RequestMoveToBottomSheet,
                is ConversationDetailViewAction.RequestMessageLabelAsBottomSheet,
                is ConversationDetailViewAction.RequestMessageMoveToBottomSheet -> BottomSheetOperation.Requested

                is ErrorMovingMessage,
                is ErrorMovingConversation,
                is ErrorLabelingConversation,
                is ConversationDetailViewAction.LabelAsConfirmed,
                is ConversationDetailEvent.ReportPhishingRequested,
                is ConversationDetailViewAction.DismissBottomSheet,
                is ConversationDetailViewAction.SwitchViewMode,
                is ConversationDetailViewAction.PrintRequested,
                is ConversationDetailViewAction.MarkMessageUnread,
                is ConversationDetailViewAction.MoveMessage,
                is ConversationDetailViewAction.MoveToDestinationConfirmed,
                is ConversationDetailEvent.MessageMoved,
                is ConversationDetailEvent.MessageLoadFailed,
                is ConversationDetailEvent.LastMessageMoved -> BottomSheetOperation.Dismiss
            }
            bottomSheetReducer.newStateFrom(bottomSheetState, bottomSheetOperation)
        } else {
            bottomSheetState
        }

    @Suppress("ComplexMethod")
    private fun ConversationDetailState.toErrorState(operation: ConversationDetailOperation): Effect<TextUiModel> {
        return if (operation is ConversationDetailOperation.AffectingErrorBar) {
            val textResource = when (operation) {
                is ErrorAddStar -> R.string.error_star_operation_failed
                is ErrorRemoveStar -> R.string.error_unstar_operation_failed
                is ErrorMarkingAsUnread -> R.string.error_mark_as_unread_failed
                is ErrorMovingToTrash -> R.string.error_move_to_trash_failed
                is ErrorMovingConversation -> R.string.error_move_conversation_failed
                is ErrorMovingMessage -> R.string.error_move_message_failed
                is ErrorLabelingConversation -> R.string.error_relabel_message_failed
                is ErrorExpandingDecryptMessageError -> R.string.decryption_error
                is ErrorExpandingRetrieveMessageError -> R.string.detail_error_retrieving_message_body
                is ErrorExpandingRetrievingMessageOffline -> R.string.error_offline_loading_message
                is ErrorGettingAttachment -> R.string.error_get_attachment_failed
                is ErrorGettingAttachmentNotEnoughSpace -> R.string.error_get_attachment_not_enough_memory
                is ErrorAttachmentDownloadInProgress -> R.string.error_attachment_download_in_progress
                is ErrorDeletingConversation -> R.string.error_delete_conversation_failed
                is ErrorDeletingNoApplicableFolder -> R.string.error_delete_conversation_failed_wrong_folder
            }
            Effect.of(TextUiModel(textResource))
        } else {
            error
        }
    }

    private fun ConversationDetailState.toMessageState(operation: ConversationDetailOperation): Effect<TextUiModel> {
        return if (operation is ConversationDetailOperation.AffectingMessageBar) {
            val textUiModel = when (operation) {
                is ConversationDetailEvent.MessageMoved -> {
                    TextUiModel.TextResWithArgs(
                        R.string.message_moved_to,
                        listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
                    )
                }

                ConversationDetailEvent.MessageLoadFailed -> {
                    TextUiModel.TextRes(
                        R.string.error_offline_loading_message
                    )
                }
            }

            Effect.of(textUiModel)
        } else {
            message
        }
    }

    private fun ConversationDetailState.toExitState(operation: ConversationDetailOperation): Effect<Unit> =
        when (operation) {
            is ConversationDetailViewAction.MarkUnread -> Effect.of(Unit)
            else -> exitScreenEffect
        }

    private fun ConversationDetailState.toExitWithMessageState(
        operation: ConversationDetailOperation
    ): Effect<ActionResult> = when (operation) {
        is ConversationDetailViewAction.Trash -> Effect.of(
            UndoableActionResult(TextUiModel(R.string.conversation_moved_to_trash))
        )

        is ConversationDetailViewAction.MoveToDestinationConfirmed ->
            when (operation.entryPoint == MoveToBottomSheetEntryPoint.Conversation) {
                true -> Effect.of(
                    UndoableActionResult(
                        TextUiModel.TextResWithArgs(
                            R.string.conversation_moved_to_selected_destination,
                            listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
                        )
                    )
                )

                false -> exitScreenWithMessageEffect
            }

        is ConversationDetailViewAction.LabelAsConfirmed ->
            when (
                operation.archiveSelected &&
                    operation.entryPoint == LabelAsBottomSheetEntryPoint.Conversation
            ) {
                true -> Effect.of(DefinitiveActionResult(TextUiModel(R.string.conversation_moved_to_archive)))
                false -> exitScreenWithMessageEffect
            }

        is ConversationDetailViewAction.DeleteConfirmed -> Effect.of(
            DefinitiveActionResult(TextUiModel(R.string.conversation_deleted))
        )

        is ConversationDetailEvent.MovedToSpam -> Effect.of(
            DefinitiveActionResult(TextUiModel(R.string.conversation_moved_to_spam))
        )

        is ConversationDetailEvent.LastMessageMoved -> {
            val textUiModel = TextUiModel.TextResWithArgs(
                R.string.message_moved_to,
                listOf(mailLabelTextMapper.mapToString(operation.mailLabelText))
            )
            Effect.of(DefinitiveActionResult(textUiModel))
        }

        else -> exitScreenWithMessageEffect
    }

    private fun ConversationDetailState.toOpenMessageBodyLinkState(
        operation: ConversationDetailOperation
    ): Effect<MessageBodyLink> = when (operation) {
        is ConversationDetailViewAction.MessageBodyLinkClicked -> Effect.of(
            MessageBodyLink(operation.messageId, operation.uri)
        )

        else -> openMessageBodyLinkEffect
    }

    private fun ConversationDetailState.toScrollToMessageState(
        operation: ConversationDetailOperation
    ): MessageIdUiModel? = when (operation) {
        // Scroll to message requested
        is ConversationDetailViewAction.RequestScrollTo -> operation.messageId

        // Scroll to message completed, so we need to clear the state
        is ConversationDetailViewAction.ScrollRequestCompleted -> null

        // ConversationDetailEvent.MessagesData update should not clear the scroll state. It will be cleared when
        // the scroll is completed.
        is MessagesData -> {
            operation.requestScrollToMessageId ?: scrollToMessage
        }

        else -> scrollToMessage
    }

    private fun ConversationDetailState.toNewOpenAttachmentStateFrom(
        operation: ConversationDetailOperation
    ): Effect<OpenAttachmentIntentValues> = when (operation) {
        is OpenAttachmentEvent -> Effect.of(operation.values)
        else -> openAttachmentEffect
    }

    private fun ConversationDetailState.toNewOpenProtonCalendarIntentFrom(
        operation: ConversationDetailOperation
    ): Effect<OpenProtonCalendarIntentValues> = when (operation) {
        is ConversationDetailEvent.HandleOpenProtonCalendarRequest -> Effect.of(operation.intent)
        else -> openProtonCalendarIntent
    }

    private fun ConversationDetailState.toOpenReplyFrom(
        operation: ConversationDetailOperation
    ): Effect<MessageIdUiModel> = when (operation) {
        is ConversationDetailEvent.ReplyToMessageRequested -> Effect.of(operation.messageId)
        else -> openReply
    }

    private fun ConversationDetailState.toOpenReplyAllFrom(
        operation: ConversationDetailOperation
    ): Effect<MessageIdUiModel> = when (operation) {
        is ConversationDetailEvent.ReplyAllToMessageRequested -> Effect.of(operation.messageId)
        else -> openReplyAll
    }

    private fun ConversationDetailState.toOpenForwardFrom(
        operation: ConversationDetailOperation
    ): Effect<MessageIdUiModel> = when (operation) {
        is ConversationDetailEvent.ForwardMessageRequested -> Effect.of(operation.messageId)
        else -> openForward
    }

    private fun ConversationDetailState.toNewDeleteDialogState(
        operation: ConversationDetailOperation
    ): DeleteDialogState {
        return if (operation is AffectingDeleteDialog) {
            deleteDialogReducer.newStateFrom(operation)
        } else {
            deleteDialogState
        }
    }

    private fun ConversationDetailState.toNewReportPhishingDialogState(
        operation: ConversationDetailOperation
    ): ReportPhishingDialogState {
        return if (operation is ConversationDetailOperation.AffectingReportPhishingDialog) {
            reportPhishingDialogReducer.newStateFrom(operation)
        } else {
            reportPhishingDialogState
        }
    }

    private fun ConversationDetailState.toNewTrashedMessagesBannerState(
        operation: ConversationDetailOperation
    ): TrashedMessagesBannerState {
        return if (operation is ConversationDetailOperation.AffectingTrashedMessagesBanner) {
            trashedMessagesBannerReducer.newStateFrom(operation)
        } else {
            trashedMessagesBannerState
        }
    }

    private fun ConversationDetailState.toNewSpotlightState(
        operation: ConversationDetailOperation
    ): SpotlightTooltipState {
        return if (operation is ConversationDetailOperation.AffectingSpotlight) {
            val hasBottomBar = bottomBarState is BottomBarState.Data
            val canReduce = when (operation) {
                ConversationDetailEvent.RequestCustomizeToolbarSpotlight -> hasBottomBar && bottomSheetState == null
                ConversationDetailViewAction.SpotlightDismissed -> hasBottomBar
            }
            if (canReduce) {
                customizeToolbarSpotlightReducer.newStateFrom(operation)
            } else {
                spotlightTooltip
            }
        } else {
            spotlightTooltip
        }
    }
}
