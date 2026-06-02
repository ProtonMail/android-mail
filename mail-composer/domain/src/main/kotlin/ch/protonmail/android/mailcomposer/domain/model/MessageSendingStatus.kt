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

package ch.protonmail.android.mailcomposer.domain.model

import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface MessageSendingStatus {
    val messageId: MessageId

    data class MessageSentUndoable(
        override val messageId: MessageId,
        val timeRemainingForUndo: Duration
    ) : MessageSendingStatus

    data class MessageSentFinal(
        override val messageId: MessageId
    ) : MessageSendingStatus

    data class MessageScheduledUndoable(
        override val messageId: MessageId,
        val deliveryTime: Instant
    ) : MessageSendingStatus

    data class SendMessageError(
        override val messageId: MessageId,
        val reason: SendErrorReason
    ) : MessageSendingStatus

    data class NoStatus(
        override val messageId: MessageId
    ) : MessageSendingStatus

    data class UndoSendError(
        override val messageId: MessageId,
        val badRequestMessage: String? = null
    ) : MessageSendingStatus

    data class CancellingScheduleSend(
        override val messageId: MessageId
    ) : MessageSendingStatus
}
