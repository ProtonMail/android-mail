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

import android.net.Uri
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation.AffectingBottomSheet
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation.AffectingErrorBar
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation.AffectingMessage
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation.AffectingMessageBody
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import me.proton.core.label.domain.entity.LabelId

sealed interface MessageDetailOperation {
    sealed interface AffectingMessage
    sealed interface AffectingMessageBody
    sealed interface AffectingErrorBar
    sealed interface AffectingBottomSheet
}

sealed interface MessageDetailEvent : MessageDetailOperation {

    data class MessageWithLabelsEvent(
        val messageDetailActionBar: MessageDetailActionBarUiModel,
        val messageDetailHeader: MessageDetailHeaderUiModel
    ) : MessageDetailEvent,
        AffectingMessage

    data class MessageBodyEvent(
        val messageBody: MessageBodyUiModel
    ) : MessageDetailEvent,
        AffectingMessageBody

    data class ErrorGettingMessageBody(
        val isNetworkError: Boolean
    ) : MessageDetailEvent, AffectingMessageBody

    data class ErrorDecryptingMessageBody(
        val messageBody: MessageBodyUiModel
    ) : MessageDetailEvent, AffectingMessageBody

    data class MessageBottomBarEvent(
        val bottomBarEvent: BottomBarEvent
    ) : MessageDetailEvent

    data class MessageBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : MessageDetailEvent, AffectingBottomSheet

    data class OpenAttachmentEvent(val values: OpenAttachmentIntentValues) : MessageDetailEvent

    data class AttachmentStatusChanged(
        val attachmentId: AttachmentId,
        val status: AttachmentWorkerStatus
    ) : MessageDetailEvent, AffectingMessageBody

    object NoCachedMetadata : MessageDetailEvent, AffectingMessage
    object ErrorAddingStar : MessageDetailEvent, AffectingMessage, AffectingErrorBar
    object ErrorRemovingStar : MessageDetailEvent, AffectingMessage, AffectingErrorBar

    object ErrorMarkingUnread : MessageDetailEvent, AffectingErrorBar
    object ErrorMovingToTrash : MessageDetailEvent, AffectingErrorBar
    object ErrorMovingMessage : MessageDetailEvent, AffectingErrorBar
    object ErrorLabelingMessage : MessageDetailEvent, AffectingErrorBar
    object ErrorGettingAttachment : MessageDetailEvent, AffectingErrorBar
    object ErrorGettingAttachmentNotEnoughSpace : MessageDetailEvent, AffectingErrorBar
    object ErrorAttachmentDownloadInProgress : MessageDetailEvent, AffectingErrorBar
}

sealed interface MessageViewAction : MessageDetailOperation {
    object Reload : MessageViewAction, AffectingMessageBody
    object Star : MessageViewAction, AffectingMessage
    object UnStar : MessageViewAction, AffectingMessage
    object MarkUnread : MessageViewAction
    object Trash : MessageViewAction
    object RequestMoveToBottomSheet : MessageViewAction, AffectingBottomSheet
    object RequestLabelAsBottomSheet : MessageViewAction, AffectingBottomSheet
    object DismissBottomSheet : MessageViewAction, AffectingBottomSheet
    data class MoveToDestinationSelected(val mailLabelId: MailLabelId) : MessageViewAction, AffectingBottomSheet
    data class MoveToDestinationConfirmed(val mailLabelText: String) : MessageViewAction
    data class LabelAsToggleAction(val labelId: LabelId) : MessageViewAction, AffectingBottomSheet
    data class LabelAsConfirmed(val archiveSelected: Boolean) : MessageViewAction, AffectingBottomSheet
    data class MessageBodyLinkClicked(val uri: Uri) : MessageViewAction
    object ShowAllAttachments : MessageViewAction
    data class OnAttachmentClicked(val attachmentId: AttachmentId) : MessageViewAction
}
