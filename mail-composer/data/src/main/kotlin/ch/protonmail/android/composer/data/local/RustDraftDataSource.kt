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

package ch.protonmail.android.composer.data.local

import arrow.core.Either
import ch.protonmail.android.composer.data.wrapper.AttachmentsWrapper
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.ChangeSenderError
import ch.protonmail.android.mailcomposer.domain.model.DiscardDraftError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.model.MessagePasswordError
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendDraftError
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.model.ValidatedRecipients
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.DraftAddressValidationResult
import uniffi.mail_uniffi.DraftExpirationTime
import uniffi.mail_uniffi.DraftRecipientExpirationFeatureReport
import uniffi.mail_uniffi.DraftScheduleSendOptions

@Suppress("ComplexInterface", "TooManyFunctions")
interface RustDraftDataSource {

    suspend fun getMessageId(): Either<DataError, MessageId>
    suspend fun open(userId: UserId, messageId: MessageId): Either<OpenDraftError, LocalDraftWithSyncStatus>
    suspend fun create(userId: UserId, action: DraftAction): Either<OpenDraftError, LocalDraft>
    suspend fun discard(userId: UserId, messageId: MessageId): Either<DiscardDraftError, Unit>
    suspend fun saveSubject(subject: Subject): Either<SaveDraftError, Unit>
    suspend fun saveBody(body: DraftBody): Either<SaveDraftError, Unit>
    suspend fun send(): Either<SendDraftError, Unit>
    suspend fun scheduleSend(timestamp: Long): Either<SendDraftError, Unit>
    suspend fun undoSend(userId: UserId, messageId: MessageId): Either<UndoSendError, Unit>
    suspend fun attachmentList(): Either<DataError, AttachmentsWrapper>
    suspend fun updateToRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit>
    suspend fun updateCcRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit>
    suspend fun updateBccRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit>
    suspend fun listSenderAddresses(): Either<DataError, LocalSenderAddresses>
    suspend fun changeSender(sender: SenderEmail): Either<ChangeSenderError, Unit>
    suspend fun bodyFields(): Either<DataError, BodyFields>
    suspend fun isPasswordProtected(): Either<DataError, Boolean>
    suspend fun setMessagePassword(password: MessagePassword): Either<MessagePasswordError, Unit>

    suspend fun removeMessagePassword(): Either<MessagePasswordError, Unit>
    suspend fun getMessagePassword(): Either<DataError, MessagePassword?>
    suspend fun getMessageExpiration(): Either<DataError, DraftExpirationTime>
    suspend fun setMessageExpiration(expirationTime: MessageExpirationTime): Either<MessageExpirationError, Unit>
    suspend fun validateSendWithExpiration(): Either<DataError, DraftRecipientExpirationFeatureReport>
    suspend fun validateDraftSenderAddress(): DraftAddressValidationResult?

    fun loadImage(url: String): Either<AttachmentDataError, LocalAttachmentData>
    fun getScheduleSendOptions(): Either<DataError, DraftScheduleSendOptions>

    /**
     * Signal is emit instead of data as this data source is a Singleton. Under the specific scenario where we have
     * two instances of composer at the same time (ie. composing a message in the app and then performing a "share via"
     * from another app), this signal would be received by both instances while only one cares about it.
     *
     * Delegating the call to verify the change (eg. `isPasswordProtected` / `getMessagePassword`) to the
     * view model is a workaround that ensures only the impacted VM reacts to the event (as the VM call is
     * dispatched to their own instance of rust "Draft" object by DraftCache class).
     *
     */
    fun observePasswordUpdatedSignal(): Flow<Unit>

    fun observeRecipientsValidationEvents(): Flow<ValidatedRecipients>
}
