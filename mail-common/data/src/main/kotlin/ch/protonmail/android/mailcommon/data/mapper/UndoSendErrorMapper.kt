/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcommon.data.mapper

import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import uniffi.mail_uniffi.DraftCancelScheduleSendError
import uniffi.mail_uniffi.DraftCancelScheduleSendErrorReason
import uniffi.mail_uniffi.DraftUndoSendError
import uniffi.mail_uniffi.DraftUndoSendErrorReason

fun DraftUndoSendError.toUndoSendError(): UndoSendError = when (this) {
    is DraftUndoSendError.Other -> UndoSendError.Other(this.v1.toDataError())
    is DraftUndoSendError.Reason -> when (this.v1) {
        DraftUndoSendErrorReason.MESSAGE_DOES_NOT_EXIST,
        DraftUndoSendErrorReason.MESSAGE_CAN_NOT_BE_UNDO_SENT,
        DraftUndoSendErrorReason.SEND_CAN_NO_LONGER_BE_UNDONE -> UndoSendError.UndoSendFailed
    }
}

fun DraftCancelScheduleSendError.toUndoSendError(): UndoSendError = when (this) {
    is DraftCancelScheduleSendError.Other -> UndoSendError.Other(this.v1.toDataError())
    is DraftCancelScheduleSendError.Reason -> when (this.v1) {
        DraftCancelScheduleSendErrorReason.MESSAGE_DOES_NOT_EXIST,
        DraftCancelScheduleSendErrorReason.MESSAGE_NOT_SCHEDULED,
        DraftCancelScheduleSendErrorReason.MESSAGE_ALREADY_SENT -> UndoSendError.UndoSendFailed
    }
}
