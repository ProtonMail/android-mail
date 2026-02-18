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

import java.time.Duration
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toChangeSenderError
import ch.protonmail.android.composer.data.mapper.toComposerRecipients
import ch.protonmail.android.composer.data.mapper.toDraftCreateMode
import ch.protonmail.android.composer.data.mapper.toDraftSendError
import ch.protonmail.android.composer.data.mapper.toGroupRecipients
import ch.protonmail.android.composer.data.mapper.toLocalDraft
import ch.protonmail.android.composer.data.mapper.toLocalDraftWithSyncStatus
import ch.protonmail.android.composer.data.mapper.toLocalExpirationTime
import ch.protonmail.android.composer.data.mapper.toLocalSenderAddresses
import ch.protonmail.android.composer.data.mapper.toMessageExpirationError
import ch.protonmail.android.composer.data.mapper.toMessagePassword
import ch.protonmail.android.composer.data.mapper.toMessagePasswordError
import ch.protonmail.android.composer.data.mapper.toSaveDraftError
import ch.protonmail.android.composer.data.mapper.toSingleRecipientEntry
import ch.protonmail.android.composer.data.mapper.toSingleRecipients
import ch.protonmail.android.composer.data.usecase.CreateRustDraft
import ch.protonmail.android.composer.data.usecase.DiscardRustDraft
import ch.protonmail.android.composer.data.usecase.OpenRustDraft
import ch.protonmail.android.composer.data.usecase.RustDraftUndoSend
import ch.protonmail.android.composer.data.worker.SendingStatusWorker
import ch.protonmail.android.composer.data.wrapper.AttachmentsWrapper
import ch.protonmail.android.composer.data.wrapper.ComposerRecipientListWrapper
import ch.protonmail.android.mailcommon.data.mapper.LocalAttachmentData
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
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
import ch.protonmail.android.mailmessage.data.mapper.toAttachmentDataError
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.AttachmentDataResult
import uniffi.mail_uniffi.ComposerRecipientValidationCallback
import uniffi.mail_uniffi.DraftAddressValidationResult
import uniffi.mail_uniffi.DraftChangeSenderAddressResult
import uniffi.mail_uniffi.DraftExpirationTime
import uniffi.mail_uniffi.DraftExpirationTimeResult
import uniffi.mail_uniffi.DraftGetPasswordResult
import uniffi.mail_uniffi.DraftIsPasswordProtectedResult
import uniffi.mail_uniffi.DraftListSenderAddressesResult
import uniffi.mail_uniffi.DraftMessageIdResult
import uniffi.mail_uniffi.DraftRecipientExpirationFeatureReport
import uniffi.mail_uniffi.DraftScheduleSendOptions
import uniffi.mail_uniffi.DraftScheduleSendOptionsResult
import uniffi.mail_uniffi.DraftValidateRecipientsExpirationFeatureResult
import uniffi.mail_uniffi.VoidDraftExpirationResult
import uniffi.mail_uniffi.VoidDraftPasswordResult
import uniffi.mail_uniffi.VoidDraftSaveResult
import uniffi.mail_uniffi.VoidDraftSendResult
import javax.inject.Inject

@Suppress("TooManyFunctions")
class RustDraftDataSourceImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val createRustDraft: CreateRustDraft,
    private val openRustDraft: OpenRustDraft,
    private val discardRustDraft: DiscardRustDraft,
    private val rustDraftUndoSend: RustDraftUndoSend,
    private val enqueuer: Enqueuer,
    private val draftCache: DraftCache,
    private val composerSignals: ComposerSignals
) : RustDraftDataSource {

    private val mutableRecipientsUpdatedFlow = MutableSharedFlow<ValidatedRecipients>(
        replay = 1,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val recipientsUpdatedCallback = object : ComposerRecipientValidationCallback {
        override fun onUpdate() {
            val draft = try {
                draftCache.get()
            } catch (e: IllegalStateException) {
                Timber.w("Recipient update received after draft was closed. $e")
                return
            }
            val toRecipients = draft.recipientsTo().recipients().toComposerRecipients()
            val ccRecipients = draft.recipientsCc().recipients().toComposerRecipients()
            val bccRecipients = draft.recipientsBcc().recipients().toComposerRecipients()
            val updatedRecipients = ValidatedRecipients(toRecipients, ccRecipients, bccRecipients)

            mutableRecipientsUpdatedFlow.tryEmit(updatedRecipients)
        }
    }

    private fun registerRecipientCallbacks() {
        val draft = draftCache.get()
        draft.recipientsTo().registerCallback(recipientsUpdatedCallback)
        draft.recipientsCc().registerCallback(recipientsUpdatedCallback)
        draft.recipientsBcc().registerCallback(recipientsUpdatedCallback)
    }

    override suspend fun getMessageId(): Either<DataError, MessageId> =
        when (val result = draftCache.get().messageId()) {
            is DraftMessageIdResult.Error -> result.v1.toDataError().left()
            is DraftMessageIdResult.Ok -> result.v1?.toMessageId()?.right() ?: DataError.Local.NotFound.left()
        }

    override suspend fun open(userId: UserId, messageId: MessageId): Either<OpenDraftError, LocalDraftWithSyncStatus> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-draft: Trying to open draft with null session; Failing.")
            return OpenDraftError.Other(DataError.Local.NoUserSession).left()
        }

        Timber.d("rust-draft: Opening draft...")
        return openRustDraft(session, messageId.toLocalMessageId())
            .onRight {
                Timber.d("rust-draft: Draft opened successfully.")
                draftCache.add(it.draftWrapper)
                registerRecipientCallbacks()
            }
            .onLeft { Timber.d("rust-draft: Unable to open draft - $it") }
            .map { it.toLocalDraftWithSyncStatus() }
    }

    override suspend fun create(userId: UserId, action: DraftAction): Either<OpenDraftError, LocalDraft> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-draft: Trying to create draft with null session; Failing.")
            return OpenDraftError.Other(DataError.Local.NoUserSession).left()
        }

        val draftCreateMode = action.toDraftCreateMode()
        if (draftCreateMode == null) {
            Timber.e("rust-draft: Trying to create draft with invalid create mode; Failing.")
            return OpenDraftError.Other(DataError.Local.UnsupportedOperation).left()
        }

        return createRustDraft(session, draftCreateMode)
            .onRight {
                draftCache.add(it)
                registerRecipientCallbacks()
            }
            .onLeft { Timber.d("rust-draft: Unable to create draft - $it") }
            .map { it.toLocalDraft() }
    }

    override suspend fun discard(userId: UserId, messageId: MessageId): Either<DiscardDraftError, Unit> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-draft: Trying to discard draft with null session; Failing.")
            return DiscardDraftError.Other(DataError.Local.NoUserSession).left()
        }

        return discardRustDraft(session, messageId.toLocalMessageId())
    }

    override suspend fun saveSubject(subject: Subject): Either<SaveDraftError, Unit> =
        when (val result = draftCache.get().setSubject(subject.value)) {
            is VoidDraftSaveResult.Error -> result.v1.toSaveDraftError().left()
            is VoidDraftSaveResult.Ok -> Unit.right()
        }

    override suspend fun saveBody(body: DraftBody): Either<SaveDraftError, Unit> =
        when (val result = draftCache.get().setBody(body.value)) {
            is VoidDraftSaveResult.Error -> result.v1.toSaveDraftError().left()
            is VoidDraftSaveResult.Ok -> Unit.right()
        }

    override suspend fun updateToRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit> =
        updateRecipients(draftCache.get().recipientsTo(), recipients)

    override suspend fun updateCcRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit> =
        updateRecipients(draftCache.get().recipientsCc(), recipients)

    override suspend fun updateBccRecipients(recipients: List<DraftRecipient>): Either<SaveDraftError, Unit> =
        updateRecipients(draftCache.get().recipientsBcc(), recipients)

    override suspend fun listSenderAddresses(): Either<DataError, LocalSenderAddresses> =
        when (val result = draftCache.get().listSenderAddresses()) {
            is DraftListSenderAddressesResult.Error -> result.v1.toDataError().left()
            is DraftListSenderAddressesResult.Ok -> result.v1.toLocalSenderAddresses().right()
        }

    override suspend fun changeSender(sender: SenderEmail): Either<ChangeSenderError, Unit> =
        when (val result = draftCache.get().changeSender(sender.value)) {
            is DraftChangeSenderAddressResult.Error -> result.v1.toChangeSenderError().left()
            DraftChangeSenderAddressResult.Ok -> Unit.right()
        }

    override suspend fun send(): Either<SendDraftError, Unit> = when (val result = draftCache.get().send()) {
        is VoidDraftSendResult.Error -> result.v1.toDraftSendError().left()
        is VoidDraftSendResult.Ok -> {
            startSendingStatusWorker()
            Unit.right()
        }
    }

    override suspend fun scheduleSend(timestamp: Long): Either<SendDraftError, Unit> =
        when (val result = draftCache.get().scheduleSend(timestamp.toULong())) {
            is VoidDraftSendResult.Error -> result.v1.toDraftSendError().left()
            VoidDraftSendResult.Ok -> Unit.right()
        }

    override suspend fun undoSend(userId: UserId, messageId: MessageId): Either<UndoSendError, Unit> {
        Timber.d("rust-draft: Undo sending draft...")
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-draft: Trying to undo send with null session; Failing.")
            return UndoSendError.Other(DataError.Local.NoUserSession).left()
        }

        return rustDraftUndoSend(session, messageId.toLocalMessageId()).onRight {
            enqueuer.cancelWork(SendingStatusWorker.id(userId, messageId))
        }
    }

    override suspend fun attachmentList(): Either<DataError, AttachmentsWrapper> {
        val wrapper = draftCache.get()
        return wrapper.attachmentList().right()
    }

    override fun loadImage(url: String): Either<AttachmentDataError, LocalAttachmentData> =
        when (val result = draftCache.get().loadImage(url)) {
            is AttachmentDataResult.Error -> result.v1.toAttachmentDataError().left()
            is AttachmentDataResult.Ok -> result.v1.right()
        }

    override fun getScheduleSendOptions(): Either<DataError, DraftScheduleSendOptions> =
        when (val result = draftCache.get().scheduleSendOptions()) {
            is DraftScheduleSendOptionsResult.Error -> result.v1.toDataError().left()
            is DraftScheduleSendOptionsResult.Ok -> result.v1.right()
        }

    override fun observePasswordUpdatedSignal(): Flow<Unit> = composerSignals.observePasswordChanged()

    override fun observeRecipientsValidationEvents(): Flow<ValidatedRecipients> {
        Timber.tag("RecipientValidation").d("Registering recipients observer on data source...")
        return mutableRecipientsUpdatedFlow.asSharedFlow()
    }

    override suspend fun bodyFields(): Either<DataError, BodyFields> = draftCache.get().bodyFields().right()

    override suspend fun isPasswordProtected(): Either<DataError, Boolean> =
        when (val result = draftCache.get().isPasswordProtected()) {
            is DraftIsPasswordProtectedResult.Error -> result.v1.toDataError().left()
            is DraftIsPasswordProtectedResult.Ok -> result.v1.right()
        }

    override suspend fun setMessagePassword(password: MessagePassword): Either<MessagePasswordError, Unit> =
        when (val result = draftCache.get().setPassword(password.password, password.hint)) {
            is VoidDraftPasswordResult.Error -> result.v1.toMessagePasswordError().left()
            VoidDraftPasswordResult.Ok -> Unit.right()
        }.also {
            composerSignals.emitPasswordChanged()
        }

    override suspend fun removeMessagePassword(): Either<MessagePasswordError, Unit> =
        when (val result = draftCache.get().removePassword()) {
            is VoidDraftPasswordResult.Error -> result.v1.toMessagePasswordError().left()
            VoidDraftPasswordResult.Ok -> Unit.right()
        }.also {
            composerSignals.emitPasswordChanged()
        }

    override suspend fun getMessagePassword(): Either<DataError, MessagePassword?> =
        when (val result = draftCache.get().getPassword()) {
            is DraftGetPasswordResult.Error -> result.v1.toDataError().left()
            is DraftGetPasswordResult.Ok -> result.v1.toMessagePassword().right()
        }

    override suspend fun getMessageExpiration(): Either<DataError, DraftExpirationTime> =
        when (val result = draftCache.get().getMessageExpiration()) {
            is DraftExpirationTimeResult.Error -> result.v1.toDataError().left()
            is DraftExpirationTimeResult.Ok -> result.v1.right()
        }

    override suspend fun setMessageExpiration(
        expirationTime: MessageExpirationTime
    ): Either<MessageExpirationError, Unit> =
        when (val result = draftCache.get().setMessageExpiration(expirationTime.toLocalExpirationTime())) {
            is VoidDraftExpirationResult.Error -> result.v1.toMessageExpirationError().left()
            VoidDraftExpirationResult.Ok -> Unit.right()
        }

    override suspend fun validateSendWithExpiration(): Either<DataError, DraftRecipientExpirationFeatureReport> =
        when (val result = draftCache.get().validateRecipientsExpirationFeature()) {
            is DraftValidateRecipientsExpirationFeatureResult.Error -> {
                Timber.e("rust-draft: Failed to validate send with expiration: ${result.v1}")
                result.v1.toDataError().left()
            }
            is DraftValidateRecipientsExpirationFeatureResult.Ok -> result.v1.right()
        }

    override suspend fun validateDraftSenderAddress(): DraftAddressValidationResult? =
        draftCache.get().getAddressValidationResult()

    private fun updateRecipients(
        recipientsWrapper: ComposerRecipientListWrapper,
        updatedRecipients: List<DraftRecipient>
    ): Either<SaveDraftError, Unit> = either {
        val currentSingles = recipientsWrapper.recipients().toSingleRecipients()
        val currentGroups = recipientsWrapper.recipients().toGroupRecipients()

        // single recipients diff
        val updatedSingleRecipients = updatedRecipients.filterIsInstance<DraftRecipient.SingleRecipient>()
        val recipientsToAdd = updatedSingleRecipients.filterNot { updatedRecipient ->
            updatedRecipient.address in currentSingles.map { it.address }
        }.toSet()
        val recipientsToRemove = currentSingles.filterNot { currentRecipient ->
            currentRecipient.address in updatedSingleRecipients.map { it.address }
        }.toSet()

        var foundDuplicate = false
        recipientsToAdd.forEach {
            recipientsWrapper.addSingleRecipient(it.toSingleRecipientEntry())
                .onLeft { error ->
                    when (error) {
                        is SaveDraftError.DuplicateRecipient -> foundDuplicate = true
                        else -> raise(error)
                    }
                }
        }
        recipientsToRemove.forEach {
            recipientsWrapper.removeSingleRecipient(it.toSingleRecipientEntry())
                .onLeft { error -> raise(error) }
        }

        // group recipients diff
        val updatedGroups = updatedRecipients.filterIsInstance<DraftRecipient.GroupRecipient>()
        val groupsToAdd = updatedGroups.filterNot { u -> u.name in currentGroups.map { it.name } }
        val groupsToRemove = currentGroups.filterNot { c -> c.name in updatedGroups.map { it.name } }

        groupsToAdd.forEach { group ->
            recipientsWrapper.addGroupRecipient(
                groupName = group.name,
                recipients = group.recipients.map { it.toSingleRecipientEntry() },
                totalContactsInGroup = group.recipients.size.toULong()
            ).onLeft { error -> raise(error) }
        }
        groupsToRemove.forEach { group ->
            recipientsWrapper.removeGroup(group.name)
                .onLeft { error -> raise(error) }
        }

        // Force a Rust -> UI sync to remove any stale Validating chips for the rejected duplicates,
        // then signal the caller so it can show the appropriate toast.
        if (foundDuplicate) {
            recipientsUpdatedCallback.onUpdate()
            raise(SaveDraftError.DuplicateRecipient)
        }
    }

    private suspend fun startSendingStatusWorker() {
        val userId = userSessionRepository.observePrimaryUserId().firstOrNull()
        if (userId == null) {
            Timber.e("rust-draft: Trying to start sending status worker with null userId; Failing.")
            return
        }

        val messageId = this.getMessageId()
            .onLeft { Timber.e("rust-draft: Failed to get messageId due to error: $it") }
            .getOrNull()


        if (messageId == null) {
            Timber.e("rust-draft: Trying to start sending status worker with null messageId; Failing.")
            return
        }

        Timber.d("rust-draft: Starting sending status worker...")
        enqueuer.enqueueUniqueWork<SendingStatusWorker>(
            userId = userId,
            workerId = SendingStatusWorker.id(userId, messageId),
            params = SendingStatusWorker.params(userId, messageId),
            backoffCriteria = Enqueuer.BackoffCriteria.DefaultLinear,
            initialDelay = InitialDelayForSendingStatusWorker
        )
    }

    companion object {

        val InitialDelayForSendingStatusWorker: Duration = Duration.ofMillis(10_000L)
    }
}
