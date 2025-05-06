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

package ch.protonmail.android.mailcomposer.domain.sample

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import me.proton.core.domain.entity.UserId

object DraftStateSample {

    val NewDraftState = build()

    /**
     * Represents a draft that was created locally and never synced to API yet
     */
    val LocalDraftNeverSynced = build(
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = null,
        state = DraftSyncState.Local
    )
    /**
     * Represents a draft that was created locally and then synced to remote
     */
    val LocalDraftThatWasSyncedOnce = build(
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.Synchronized
    )

    /**
     * Represents a draft that was created from another platform and being edited for the first time
     * Draft state being created will have a messageId in the "remote" format but have no `apiMessageId` yet)
     */
    val RemoteWithoutApiMessageId = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = null,
        state = DraftSyncState.Synchronized
    )

    val RemoteDraftState = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.Synchronized
    )

    /**
     * Represents a remote draft that is scheduled for sending.
     */
    val RemoteDraftInSendingState = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.Sending
    )

    /**
     * Represents a remote draft that failed to be sent.
     */
    val RemoteDraftInErrorSendingState = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.ErrorSending,
        sendingError = SendingError.ExternalAddressSendDisabled("Api message for disabled")
    )

    /**
     * Represents a remote draft that failed to upload attachments.
     */
    val RemoteDraftInErrorAttachmentUploadState = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.ErrorUploadAttachments
    )

    /**
     * Represents a remote draft that has been successfully sent.
     */
    val RemoteDraftInSentState = build(
        messageId = MessageIdSample.RemoteDraft,
        apiMessageId = MessageIdSample.RemoteDraft,
        state = DraftSyncState.Sent
    )

    /**
     * Represents a local draft that's forwarding a parent message
     */
    val LocalDraftWithForwardAction = build(
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = MessageIdSample.MessageWithAttachments,
        state = DraftSyncState.Local,
        action = DraftAction.Forward(MessageIdSample.Invoice)
    )

    fun build(
        userId: UserId = UserIdSample.Primary,
        messageId: MessageId = MessageIdSample.EmptyDraft,
        apiMessageId: MessageId? = null,
        state: DraftSyncState = DraftSyncState.Local,
        action: DraftAction = DraftAction.Compose,
        sendingError: SendingError? = null
    ) = DraftState(
        userId = userId,
        messageId = messageId,
        apiMessageId = apiMessageId,
        state = state,
        action = action,
        sendingError = sendingError,
        sendingStatusConfirmed = false
    )
}
