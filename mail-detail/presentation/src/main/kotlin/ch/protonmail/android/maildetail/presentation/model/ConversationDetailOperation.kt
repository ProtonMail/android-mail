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

package ch.protonmail.android.maildetail.presentation.model

import android.content.Context
import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingDeleteDialog
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingErrorBar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingMessageBar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingMessages
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingReportPhishingDialog
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingTrashedMessagesBanner
import ch.protonmail.android.maildetail.presentation.ui.MessageBody
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetOperation
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import kotlinx.collections.immutable.ImmutableList
import me.proton.core.label.domain.entity.LabelId

sealed interface ConversationDetailOperation {

    sealed interface AffectingConversation : ConversationDetailOperation
    sealed interface AffectingMessages : ConversationDetailOperation
    sealed interface AffectingErrorBar
    sealed interface AffectingMessageBar
    sealed interface AffectingBottomSheet
    sealed interface AffectingDeleteDialog
    sealed interface AffectingReportPhishingDialog
    sealed interface AffectingSpotlight
    sealed interface AffectingTrashedMessagesBanner
}

sealed interface ConversationDetailEvent : ConversationDetailOperation {

    data class ConversationBottomBarEvent(val bottomBarEvent: BottomBarEvent) : ConversationDetailEvent

    data class ConversationData(
        val conversationUiModel: ConversationDetailMetadataUiModel
    ) : ConversationDetailEvent, AffectingConversation

    object ErrorLoadingContacts : ConversationDetailEvent, AffectingMessages
    object ErrorLoadingConversation : ConversationDetailEvent, AffectingConversation, AffectingMessages
    object ErrorLoadingMessages : ConversationDetailEvent, AffectingMessages
    object NoNetworkError : ConversationDetailEvent, AffectingMessages

    data class MessagesData(
        val messagesUiModels: ImmutableList<ConversationDetailMessageUiModel>,
        val messagesLabelIds: Map<MessageId, List<LabelId>>,
        val requestScrollToMessageId: MessageIdUiModel?,
        val filterByLocation: LabelId?,
        val shouldHideMessagesBasedOnTrashFilter: Boolean
    ) : ConversationDetailEvent, AffectingMessages, AffectingTrashedMessagesBanner

    data class ConversationBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : ConversationDetailEvent, AffectingBottomSheet

    data class MessageBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : ConversationDetailEvent, AffectingBottomSheet

    object ErrorAddStar : ConversationDetailEvent, AffectingErrorBar
    object ErrorRemoveStar : ConversationDetailEvent, AffectingErrorBar
    object ErrorMarkingAsUnread : ConversationDetailEvent, AffectingErrorBar
    object ErrorMovingToTrash : ConversationDetailEvent, AffectingErrorBar
    object ErrorMovingConversation : ConversationDetailEvent, AffectingBottomSheet, AffectingErrorBar
    object ErrorMovingMessage : ConversationDetailEvent, AffectingBottomSheet, AffectingErrorBar
    object ErrorLabelingConversation : ConversationDetailEvent, AffectingBottomSheet, AffectingErrorBar
    object ErrorGettingAttachment : ConversationDetailEvent, AffectingErrorBar
    object ErrorGettingAttachmentNotEnoughSpace : ConversationDetailEvent, AffectingErrorBar
    object ErrorAttachmentDownloadInProgress : ConversationDetailEvent, AffectingErrorBar
    object ErrorDeletingConversation : ConversationDetailEvent, AffectingErrorBar, AffectingDeleteDialog
    object ErrorDeletingNoApplicableFolder : ConversationDetailEvent, AffectingErrorBar, AffectingDeleteDialog

    data class ExpandDecryptedMessage(
        val messageId: MessageIdUiModel,
        val conversationDetailMessageUiModel: ConversationDetailMessageUiModel.Expanded
    ) : ConversationDetailEvent, AffectingMessages

    data class CollapseDecryptedMessage(
        val messageId: MessageIdUiModel,
        val conversationDetailMessageUiModel: ConversationDetailMessageUiModel.Collapsed
    ) : ConversationDetailEvent, AffectingMessages

    data class ShowAllAttachmentsForMessage(
        val messageId: MessageIdUiModel,
        val conversationDetailMessageUiModel: ConversationDetailMessageUiModel.Expanded
    ) : ConversationDetailEvent, AffectingMessages

    data class ErrorExpandingDecryptMessageError(val messageId: MessageIdUiModel) :
        ConversationDetailEvent, AffectingMessages, AffectingErrorBar

    data class ErrorExpandingRetrieveMessageError(val messageId: MessageIdUiModel) :
        ConversationDetailEvent, AffectingMessages, AffectingErrorBar

    data class ErrorExpandingRetrievingMessageOffline(val messageId: MessageIdUiModel) :
        ConversationDetailEvent, AffectingMessages, AffectingErrorBar

    data class ExpandingMessage(
        val messageId: MessageIdUiModel,
        val conversationDetailMessageUiModel: ConversationDetailMessageUiModel.Collapsed
    ) : ConversationDetailEvent, AffectingMessages

    data class AttachmentStatusChanged(
        val messageId: MessageIdUiModel,
        val attachmentId: AttachmentId,
        val status: AttachmentWorkerStatus
    ) : MessageDetailEvent, AffectingMessages

    data class OpenAttachmentEvent(val values: OpenAttachmentIntentValues) : ConversationDetailEvent
    data class ReportPhishingRequested(
        val messageId: MessageId,
        val isOffline: Boolean
    ) : ConversationDetailEvent, AffectingBottomSheet, AffectingReportPhishingDialog

    data object RequestCustomizeToolbarSpotlight :
        ConversationDetailEvent,
        ConversationDetailOperation.AffectingSpotlight

    data class HandleOpenProtonCalendarRequest(val intent: OpenProtonCalendarIntentValues) : ConversationDetailEvent

    data class MessageMoved(
        val mailLabelText: MailLabelText
    ) : ConversationDetailEvent, AffectingBottomSheet, AffectingMessageBar

    data object MessageLoadFailed : ConversationDetailEvent, AffectingBottomSheet, AffectingMessageBar

    data object MovedToSpam : ConversationDetailEvent

    data class LastMessageMoved(val mailLabelText: MailLabelText) : ConversationDetailEvent, AffectingBottomSheet

    data class ReplyToMessageRequested(
        val messageId: MessageIdUiModel
    ) : ConversationDetailEvent

    data class ReplyAllToMessageRequested(
        val messageId: MessageIdUiModel
    ) : ConversationDetailEvent

    data class ForwardMessageRequested(
        val messageId: MessageIdUiModel
    ) : ConversationDetailEvent
}

sealed interface ConversationDetailViewAction : ConversationDetailOperation {

    object Star : ConversationDetailViewAction, AffectingConversation
    object UnStar : ConversationDetailViewAction, AffectingConversation
    object MarkUnread : ConversationDetailViewAction
    object Trash : ConversationDetailViewAction
    data class ReplyToLastMessage(val replyToAll: Boolean) : ConversationDetailViewAction
    data object ForwardLastMessage : ConversationDetailViewAction
    data class PrintLastMessage(val context: Context) : ConversationDetailViewAction
    data object ReportPhishingLastMessage : ConversationDetailViewAction
    data object MoveToSpam : ConversationDetailViewAction
    data object Archive : ConversationDetailViewAction
    data class EffectConsumed(
        val messageId: MessageId,
        val effect: MessageBody.DoOnDisplayedEffect
    ) : ConversationDetailViewAction
    object DeleteRequested : ConversationDetailViewAction, AffectingDeleteDialog
    object DeleteDialogDismissed : ConversationDetailViewAction, AffectingDeleteDialog
    object DeleteConfirmed : ConversationDetailViewAction, AffectingDeleteDialog
    object RequestMoveToBottomSheet : ConversationDetailViewAction, AffectingBottomSheet
    object DismissBottomSheet : ConversationDetailViewAction, AffectingBottomSheet
    data class MoveToDestinationSelected(
        val mailLabelId: MailLabelId
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data class MoveToDestinationConfirmed(
        val mailLabelText: MailLabelText,
        val entryPoint: MoveToBottomSheetEntryPoint
    ) : ConversationDetailViewAction, AffectingBottomSheet

    object RequestConversationLabelAsBottomSheet : ConversationDetailViewAction, AffectingBottomSheet
    data class LabelAsToggleAction(val labelId: LabelId) : ConversationDetailViewAction, AffectingBottomSheet
    data class LabelAsConfirmed(
        val archiveSelected: Boolean,
        val entryPoint: LabelAsBottomSheetEntryPoint
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data class RequestMoreActionsBottomSheet(val messageId: MessageId) :
        ConversationDetailViewAction, AffectingBottomSheet

    data object RequestConversationMoreActionsBottomSheet :
        ConversationDetailViewAction, AffectingBottomSheet

    data class ExpandMessage(val messageId: MessageIdUiModel) : ConversationDetailViewAction
    data class CollapseMessage(val messageId: MessageIdUiModel) : ConversationDetailViewAction
    data class MessageBodyLinkClicked(val messageId: MessageIdUiModel, val uri: Uri) : ConversationDetailViewAction
    object DoNotAskLinkConfirmationAgain : ConversationDetailViewAction
    data class RequestScrollTo(val messageId: MessageIdUiModel) : ConversationDetailViewAction
    object ScrollRequestCompleted : ConversationDetailViewAction
    data class ShowAllAttachmentsForMessage(val messageId: MessageIdUiModel) : ConversationDetailViewAction
    data class OnAttachmentClicked(val messageId: MessageIdUiModel, val attachmentId: AttachmentId) :
        ConversationDetailViewAction

    data class ExpandOrCollapseMessageBody(val messageId: MessageIdUiModel) :
        ConversationDetailViewAction, AffectingMessages

    data class LoadRemoteContent(val messageId: MessageIdUiModel) : ConversationDetailViewAction, AffectingMessages
    data class ShowEmbeddedImages(val messageId: MessageIdUiModel) : ConversationDetailViewAction, AffectingMessages
    data class LoadRemoteAndEmbeddedContent(val messageId: MessageIdUiModel) :
        ConversationDetailViewAction, AffectingMessages

    data class ReportPhishing(val messageId: MessageId) : ConversationDetailViewAction
    object ReportPhishingDismissed : ConversationDetailViewAction, AffectingReportPhishingDialog
    data class ReportPhishingConfirmed(
        val messageId: MessageId
    ) : ConversationDetailViewAction, AffectingReportPhishingDialog

    data class OpenInProtonCalendar(val messageId: MessageId) : ConversationDetailViewAction
    data class SwitchViewMode(
        val messageId: MessageId,
        val viewModePreference: ViewModePreference
    ) : ConversationDetailViewAction, AffectingBottomSheet, AffectingMessages

    data class PrintRequested(
        val messageId: MessageId
    ) : ConversationDetailViewAction, AffectingBottomSheet, AffectingMessages

    data class Print(
        val context: Context,
        val messageId: MessageId
    ) : ConversationDetailViewAction

    data class MarkMessageUnread(
        val messageId: MessageId
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data class RequestContactActionsBottomSheet(
        val participant: ParticipantUiModel,
        val avatarUiModel: AvatarUiModel
    ) : ConversationDetailViewAction,
        AffectingBottomSheet

    data class RequestMessageLabelAsBottomSheet(
        val messageId: MessageId
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data class RequestMessageMoveToBottomSheet(
        val messageId: MessageId
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data object ChangeVisibilityOfMessages : ConversationDetailViewAction

    data object SpotlightDismissed : ConversationDetailViewAction, ConversationDetailOperation.AffectingSpotlight
    data object SpotlightDisplayed : ConversationDetailViewAction

    sealed class MoveMessage(
        val messageId: MessageId,
        val labelId: LabelId,
        val mailLabelText: MailLabelText
    ) : ConversationDetailViewAction, AffectingBottomSheet {

        class MoveToSpam(messageId: MessageId) :
            MoveMessage(messageId, SystemLabelId.Spam.labelId, MailLabelText(R.string.label_title_spam))

        class MoveToTrash(messageId: MessageId) :
            MoveMessage(messageId, SystemLabelId.Trash.labelId, MailLabelText(R.string.label_title_trash))

        class MoveToArchive(messageId: MessageId) :
            MoveMessage(messageId, SystemLabelId.Archive.labelId, MailLabelText(R.string.label_title_archive))

        class MoveTo(messageId: MessageId, labelId: LabelId, mailLabelText: MailLabelText) :
            MoveMessage(messageId, labelId, mailLabelText)
    }
}
