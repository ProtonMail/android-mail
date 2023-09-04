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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses.Error
import ch.protonmail.android.mailcomposer.domain.usecase.GetDecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAllFields
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithRecipients
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.presentation.mapper.ParticipantMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.test.idlingresources.ComposerIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.network.domain.NetworkManager
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val storeAttachments: StoreAttachments,
    private val storeDraftWithBody: StoreDraftWithBody,
    private val storeDraftWithSubject: StoreDraftWithSubject,
    private val storeDraftWithAllFields: StoreDraftWithAllFields,
    private val storeDraftWithRecipients: StoreDraftWithRecipients,
    private val getContacts: GetContacts,
    private val participantMapper: ParticipantMapper,
    private val reducer: ComposerReducer,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val getPrimaryAddress: GetPrimaryAddress,
    private val getComposerSenderAddresses: GetComposerSenderAddresses,
    private val composerIdlingResource: ComposerIdlingResource,
    private val draftUploader: DraftUploader,
    private val observeMailFeature: ObserveMailFeature,
    private val sendMessage: SendMessage,
    private val networkManager: NetworkManager,
    getDecryptedDraftFields: GetDecryptedDraftFields,
    savedStateHandle: SavedStateHandle,
    observePrimaryUserId: ObservePrimaryUserId,
    provideNewDraftId: ProvideNewDraftId
) : ViewModel() {

    private val actionMutex = Mutex()
    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow(
        ComposerDraftState.initial(
            MessageId(savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey) ?: provideNewDraftId().id)
        )
    )
    val state: StateFlow<ComposerDraftState> = mutableState

    init {
        primaryUserId.onEach { userId ->
            getPrimaryAddress(userId)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingDefaultSenderAddress) }
                .onRight { emitNewStateFor(ComposerEvent.DefaultSenderReceived(SenderUiModel(it.email))) }
        }.launchIn(viewModelScope)

        val inputDraftId = savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey)
        if (inputDraftId != null) {
            Timber.d("Opening composer with $inputDraftId / ${currentMessageId()}")
            emitNewStateFor(ComposerEvent.OpenExistingDraft(currentMessageId()))

            viewModelScope.launch {
                getDecryptedDraftFields(primaryUserId(), currentMessageId())
                    .onRight {
                        startDraftContinuousUpload()
                        emitNewStateFor(ComposerEvent.ExistingDraftDataReceived(it))
                    }
                    .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingDraftData) }

            }
        } else {
            viewModelScope.launch { startDraftContinuousUpload() }
        }

        primaryUserId
            .flatMapLatest { userId -> observeMailFeature(userId, MailFeatureId.AddAttachmentsToDraft) }
            .onEach { mutableState.emit(mutableState.value.copy(isAddAttachmentsButtonVisible = it.value)) }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        composerIdlingResource.clear()
    }

    internal fun submit(action: ComposerAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                composerIdlingResource.increment()
                when (action) {
                    is ComposerAction.AttachmentsAdded -> onAttachmentsAdded(action)
                    is ComposerAction.DraftBodyChanged -> emitNewStateFor(onDraftBodyChanged(action))
                    is ComposerAction.SenderChanged -> emitNewStateFor(onSenderChanged(action))
                    is ComposerAction.SubjectChanged -> emitNewStateFor(onSubjectChanged(action))
                    is ComposerAction.ChangeSenderRequested -> emitNewStateFor(onChangeSender())
                    is ComposerAction.RecipientsToChanged -> emitNewStateFor(onToChanged(action))
                    is ComposerAction.RecipientsCcChanged -> emitNewStateFor(onCcChanged(action))
                    is ComposerAction.RecipientsBccChanged -> emitNewStateFor(onBccChanged(action))
                    is ComposerAction.OnBottomSheetOptionSelected -> emitNewStateFor(action)
                    is ComposerAction.OnAddAttachments -> emitNewStateFor(action)
                    is ComposerAction.OnCloseComposer -> emitNewStateFor(onCloseComposer(action))
                    is ComposerAction.OnSendMessage -> emitNewStateFor(onSendMessage(action))
                }
                composerIdlingResource.decrement()
            }
        }
    }

    fun validateEmailAddress(emailAddress: String): Boolean = isValidEmailAddress(emailAddress)

    private fun onAttachmentsAdded(action: ComposerAction.AttachmentsAdded) {
        viewModelScope.launch {
            val fields = DraftFields(
                currentSenderEmail(),
                currentSubject(),
                currentDraftBody(),
                currentValidRecipientsTo(),
                currentValidRecipientsCc(),
                currentValidRecipientsBcc()
            )
            storeDraftWithAllFields(primaryUserId(), currentMessageId(), fields)
            storeAttachments(primaryUserId(), currentMessageId(), action.uriList)
        }
    }

    private suspend fun onCloseComposer(action: ComposerAction.OnCloseComposer): ComposerOperation {
        val draftFields = buildDraftFields()
        return when {
            draftFields.areBlank() -> action
            else -> {
                viewModelScope.launch {
                    withContext(NonCancellable) {
                        draftUploader.stopContinuousUpload()
                        storeDraftWithAllFields(primaryUserId(), currentMessageId(), draftFields)
                        draftUploader.upload(primaryUserId(), currentMessageId())
                    }
                }
                ComposerEvent.OnCloseWithDraftSaved
            }
        }
    }

    private suspend fun onSendMessage(action: ComposerAction.OnSendMessage): ComposerOperation {
        val draftFields = buildDraftFields()
        return when {
            draftFields.areBlank() -> action
            else -> {
                viewModelScope.launch {
                    withContext(NonCancellable) {
                        draftUploader.stopContinuousUpload()
                        storeDraftWithAllFields(primaryUserId(), currentMessageId(), draftFields)
                        sendMessage(primaryUserId(), currentMessageId())
                    }
                }

                if (networkManager.isConnectedToNetwork()) {
                    ComposerAction.OnSendMessage
                } else {
                    ComposerEvent.OnSendMessageOffline
                }
            }
        }
    }

    private suspend fun buildDraftFields() = DraftFields(
        currentSenderEmail(),
        currentSubject(),
        currentDraftBody(),
        currentValidRecipientsTo(),
        currentValidRecipientsCc(),
        currentValidRecipientsBcc()
    )

    private suspend fun onSubjectChanged(action: ComposerAction.SubjectChanged): ComposerOperation =
        storeDraftWithSubject(primaryUserId.first(), currentMessageId(), currentSenderEmail(), action.subject).fold(
            ifLeft = {
                Timber.e("Store draft ${currentMessageId()} with new subject ${action.subject} failed")
                ComposerEvent.ErrorStoringDraftSubject
            },
            ifRight = { action }
        )

    private suspend fun onSenderChanged(action: ComposerAction.SenderChanged): ComposerOperation =
        storeDraftWithBody(currentMessageId(), currentDraftBody(), SenderEmail(action.sender.email), primaryUserId())
            .fold(
                ifLeft = {
                    Timber.e("Store draft ${currentMessageId()} with new sender ${action.sender.email} failed")
                    ComposerEvent.ErrorStoringDraftSenderAddress
                },
                ifRight = { action }
            )

    private suspend fun onDraftBodyChanged(action: ComposerAction.DraftBodyChanged): ComposerOperation =
        storeDraftWithBody(currentMessageId(), action.draftBody, currentSenderEmail(), primaryUserId()).fold(
            ifLeft = { ComposerEvent.ErrorStoringDraftBody },
            ifRight = { ComposerAction.DraftBodyChanged(action.draftBody) }
        )

    private suspend fun CoroutineScope.startDraftContinuousUpload() = draftUploader.startContinuousUpload(
        primaryUserId(), currentMessageId(), DraftAction.Compose, this@startDraftContinuousUpload
    )

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun currentSubject() = Subject(state.value.fields.subject)

    private fun currentDraftBody() = DraftBody(state.value.fields.body)

    private fun currentSenderEmail() = SenderEmail(state.value.fields.sender.email)

    private fun currentMessageId() = state.value.fields.draftId

    private suspend fun currentValidRecipientsTo() = RecipientsTo(
        state.value.fields.to.filterIsInstance<RecipientUiModel.Valid>().map {
            participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty())
        }
    )

    private suspend fun currentValidRecipientsCc() = RecipientsCc(
        state.value.fields.cc.filterIsInstance<RecipientUiModel.Valid>().map {
            participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty())
        }
    )

    private suspend fun currentValidRecipientsBcc() = RecipientsBcc(
        state.value.fields.bcc.filterIsInstance<RecipientUiModel.Valid>().map {
            participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty())
        }
    )

    private suspend fun contactsOrEmpty() = getContacts(primaryUserId()).getOrElse { emptyList() }

    private suspend fun onChangeSender() = getComposerSenderAddresses().fold(
        ifLeft = { changeSenderError ->
            when (changeSenderError) {
                Error.UpgradeToChangeSender -> ComposerEvent.ErrorFreeUserCannotChangeSender
                Error.FailedDeterminingUserSubscription,
                Error.FailedGettingPrimaryUser -> ComposerEvent.ErrorVerifyingPermissionsToChangeSender
            }
        },
        ifRight = { userAddresses ->
            ComposerEvent.SenderAddressesReceived(userAddresses.map { SenderUiModel(it.email) })
        }
    )

    private suspend fun onToChanged(action: ComposerAction.RecipientsToChanged): ComposerOperation =
        action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                to = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty()) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action

    private suspend fun onCcChanged(action: ComposerAction.RecipientsCcChanged): ComposerOperation =
        action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                cc = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty()) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action

    private suspend fun onBccChanged(action: ComposerAction.RecipientsBccChanged): ComposerOperation =
        action.recipients.filterIsInstance<RecipientUiModel.Valid>().takeIfNotEmpty()?.let { validRecipients ->
            storeDraftWithRecipients(
                primaryUserId(),
                currentMessageId(),
                currentSenderEmail(),
                bcc = validRecipients.map { participantMapper.recipientUiModelToParticipant(it, contactsOrEmpty()) }
            ).fold(
                ifLeft = { ComposerEvent.ErrorStoringDraftRecipients },
                ifRight = { action }
            )
        } ?: action

    private fun emitNewStateFor(operation: ComposerOperation) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, operation)
    }
}
