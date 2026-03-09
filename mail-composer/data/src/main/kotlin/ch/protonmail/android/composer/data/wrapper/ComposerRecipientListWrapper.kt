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

package ch.protonmail.android.composer.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toSaveDraftError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import uniffi.mail_uniffi.AddSingleRecipientError
import uniffi.mail_uniffi.ComposerRecipient
import uniffi.mail_uniffi.ComposerRecipientList
import uniffi.mail_uniffi.ComposerRecipientValidationCallback
import uniffi.mail_uniffi.RemoveRecipientError
import uniffi.mail_uniffi.SingleRecipientEntry

class ComposerRecipientListWrapper(private val rustRecipients: ComposerRecipientList) {

    fun recipients(): List<ComposerRecipient> = rustRecipients.recipients()

    fun registerCallback(callback: ComposerRecipientValidationCallback) = rustRecipients.setCallback(callback)

    fun addSingleRecipient(recipient: SingleRecipientEntry): Either<SaveDraftError, Unit> =
        when (val error = rustRecipients.addSingleRecipient(recipient)) {
            is AddSingleRecipientError.Ok -> Unit.right()
            is AddSingleRecipientError.Duplicate -> SaveDraftError.DuplicateRecipient.left()
            is AddSingleRecipientError.Other -> SaveDraftError.Other(DataError.Local.Unknown).left()
            is AddSingleRecipientError.SaveFailed -> error.v1.toSaveDraftError().left()
        }

    fun removeSingleRecipient(recipient: SingleRecipientEntry): Either<SaveDraftError, Unit> =
        when (val error = rustRecipients.removeSingleRecipient(recipient.email)) {
            is RemoveRecipientError.Ok -> Unit.right()
            is RemoveRecipientError.EmptyGroupName -> SaveDraftError.EmptyRecipientGroupName.left()
            is RemoveRecipientError.Other -> SaveDraftError.Other(DataError.Local.Unknown).left()
            is RemoveRecipientError.SaveFailed -> error.v1.toSaveDraftError().left()
        }
}
