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

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.decode
import ch.protonmail.android.mailcommon.domain.model.hasEmailData
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.presentation.ui.replaceText
import ch.protonmail.android.mailcomposer.domain.model.AttachmentDeleteError
import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.ChangeSenderError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.DraftFieldsWithSyncStatus
import ch.protonmail.android.mailcomposer.domain.model.DraftHead
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.OpenDraftError
import ch.protonmail.android.mailcomposer.domain.model.PasteMimeType
import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailcomposer.domain.model.SaveDraftError
import ch.protonmail.android.mailcomposer.domain.model.SendWithExpirationTimeResult
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.model.ValidatedRecipients
import ch.protonmail.android.mailcomposer.domain.model.hasAnyRecipient
import ch.protonmail.android.mailcomposer.domain.model.haveBlankRecipients
import ch.protonmail.android.mailcomposer.domain.model.haveBlankSubject
import ch.protonmail.android.mailcomposer.domain.usecase.CanSendWithExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ChangeSenderAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ConvertInlineImageToAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.CreateDraftForAction
import ch.protonmail.android.mailcomposer.domain.usecase.CreateEmptyDraft
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteInlineAttachment
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.GetDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.GetDraftSenderValidationError
import ch.protonmail.android.mailcomposer.domain.usecase.GetMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.GetSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.IsMessagePasswordSet
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.LoadMessageBodyImage
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageAttachments
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePasswordChanged
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveRecipientsValidation
import ch.protonmail.android.mailcomposer.domain.usecase.OpenExistingDraft
import ch.protonmail.android.mailcomposer.domain.usecase.SanitizePastedContent
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ScheduleSendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.SendMessage
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSubject
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateRecipients
import ch.protonmail.android.mailcomposer.presentation.mapper.toDomainModel
import ch.protonmail.android.mailcomposer.presentation.mapper.toDraftRecipient
import ch.protonmail.android.mailcomposer.presentation.mapper.toUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerStates
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ExpirationTimeUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ObservedComposerDataChanges
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.model.operations.AccessoriesEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerStateEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.CompositeEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.EffectsEvent
import ch.protonmail.android.mailcomposer.presentation.model.operations.MainEvent
import ch.protonmail.android.mailcomposer.presentation.model.toDraftRecipients
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerStateReducer
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.usecase.ActiveComposerRegistry
import ch.protonmail.android.mailcomposer.presentation.usecase.AddAttachment
import ch.protonmail.android.mailcomposer.presentation.usecase.BuildDraftDisplayBody
import ch.protonmail.android.mailcomposer.presentation.usecase.GetFormattedScheduleSendOptions
import ch.protonmail.android.mailcontact.domain.usecase.PreloadContactSuggestions
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsPrivacyBundle2601Enabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftAction.Compose
import ch.protonmail.android.mailmessage.domain.model.DraftAction.ComposeToAddresses
import ch.protonmail.android.mailmessage.domain.model.DraftAction.Forward
import ch.protonmail.android.mailmessage.domain.model.DraftAction.MailTo
import ch.protonmail.android.mailmessage.domain.model.DraftAction.PrefillForShare
import ch.protonmail.android.mailmessage.domain.model.DraftAction.Reply
import ch.protonmail.android.mailmessage.domain.model.DraftAction.ReplyAll
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions", "UnusedPrivateMember")
@HiltViewModel(assistedFactory = ComposerViewModel.Factory::class)
class ComposerViewModel @AssistedInject constructor(
    private val storeDraftWithBody: StoreDraftWithBody,
    private val storeDraftWithSubject: StoreDraftWithSubject,
    private val updateRecipients: UpdateRecipients,
    private val composerStateReducer: ComposerStateReducer,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val observeMessageAttachments: ObserveMessageAttachments,
    private val sendMessage: SendMessage,
    private val networkManager: NetworkManager,
    private val addAttachment: AddAttachment,
    private val deleteAttachment: DeleteAttachment,
    private val deleteInlineAttachment: DeleteInlineAttachment,
    private val openExistingDraft: OpenExistingDraft,
    private val createEmptyDraft: CreateEmptyDraft,
    private val createDraftForAction: CreateDraftForAction,
    private val buildDraftDisplayBody: BuildDraftDisplayBody,
    @Assisted private val composerInstanceId: String,
    @Assisted private val recipientsStateManager: RecipientsStateManager,
    private val discardDraft: DiscardDraft,
    private val getDraftId: GetDraftId,
    private val savedStateHandle: SavedStateHandle,
    private val loadMessageBodyImage: LoadMessageBodyImage,
    private val getFormattedScheduleSendOptions: GetFormattedScheduleSendOptions,
    private val scheduleSend: ScheduleSendMessage,
    private val getSenderAddresses: GetSenderAddresses,
    private val changeSenderAddress: ChangeSenderAddress,
    private val composerRegistry: ActiveComposerRegistry,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val observeMessagePasswordChanged: ObserveMessagePasswordChanged,
    private val isMessagePasswordSet: IsMessagePasswordSet,
    private val observeRecipientsValidation: ObserveRecipientsValidation,
    @IsPrivacyBundle2601Enabled private val showEncryptionInfoFeatureFlag: FeatureFlag<Boolean>,
    private val getDraftSenderValidationError: GetDraftSenderValidationError,
    private val preloadContactSuggestions: PreloadContactSuggestions,
    private val saveMessageExpirationTime: SaveMessageExpirationTime,
    private val getMessageExpirationTime: GetMessageExpirationTime,
    private val canSendWithExpirationTime: CanSendWithExpirationTime,
    private val convertInlineToAttachment: ConvertInlineImageToAttachment,
    private val sanitizePastedContent: SanitizePastedContent,
    private val appEventBroadcaster: AppEventBroadcaster,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    internal val subjectTextField = TextFieldState()

    internal val bodyTextField = TextFieldState()

    private val draftHead = MutableStateFlow(DraftHead.Empty)

    // This is what we're going to display in the Webview, keep it separate from other flows
    // as this can't be debounced (or webview recompositions won't receive the updated value)
    internal val displayBody: StateFlow<DraftDisplayBodyUiModel> =
        combine(
            snapshotFlow { bodyTextField.text },
            draftHead
        ) { text, head ->
            val draftBody = DraftBody(text.toString())
            buildDraftDisplayBody(head, draftBody)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
                initialValue = DraftDisplayBodyUiModel("")
            )

    private var pendingStoreDraftJob: Job? = null

    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val isEncryptionInfoEnabled = viewModelScope.async { showEncryptionInfoFeatureFlag.get() }
    private val composerActionsChannel = Channel<ComposerAction>(Channel.BUFFERED)

    private val mutableComposerStates = MutableStateFlow(
        ComposerStates(
            main = ComposerState.Main.initial(),
            attachments = ComposerState.Attachments.initial(),
            accessories = ComposerState.Accessories.initial(),
            effects = ComposerState.Effects.initial()
        )
    )

    internal val composerStates = mutableComposerStates.asStateFlow()


    init {
        composerRegistry.register(composerInstanceId)

        viewModelScope.launch {
            emitNewStateFor(MainEvent.InitialLoadingToggled)
            if (!setupInitialState(savedStateHandle)) return@launch
            emitNewStateFor(MainEvent.LoadingDismissed)

            observeAttachments()
            observeMessagePassword()
            observeComposerFields()
            observeValidatedRecipients()
            observeSenderValidationError()
            preloadContactSuggestions(primaryUserId())

            processActions()

        }
    }

    override fun onCleared() {
        Timber.tag("ComposerViewModel").d("Composer VM cleared from memory, unregistering as active instance")
        composerRegistry.unregister(composerInstanceId)
        super.onCleared()
    }

    @OptIn(FlowPreview::class)
    private fun observeComposerFields() {
        val combinedFlow = combine(
            composerStates.map { it.main.sender }.distinctUntilChanged(),
            recipientsStateManager.recipients,
            snapshotFlow { subjectTextField.text },
            snapshotFlow { bodyTextField.text }
        ) { sender, recipients, subject, body ->
            ObservedComposerDataChanges(
                SenderEmail(sender.email),
                recipients,
                Subject(subject.toString().stripNewLines()),
                DraftBody(body.toString())
            )
        }

        // Keep recipients validations separate from actual saving.
        combinedFlow.map { it.recipients }
            .distinctUntilChanged()
            .onEach {
                emitNewStateFor(MainEvent.RecipientsChanged(recipientsStateManager.hasValidRecipients()))
            }
            .launchIn(viewModelScope)

        combinedFlow.debounce(timeout = 1.seconds).onEach {
            if (!isComposerActive()) {
                Timber.d("Skipping draft save - composer is not active")
                return@onEach
            }

            pendingStoreDraftJob = viewModelScope.launch(defaultDispatcher) {
                Timber.d("Saving draft..")

                val (toParticipants, ccParticipants, bccParticipants) = it.recipients.toDraftRecipients()
                val bodyFields = BodyFields(draftHead.value, it.body)
                val draftFields = DraftFields(
                    it.sender,
                    it.subject,
                    bodyFields,
                    currentMimeType(),
                    RecipientsTo(toParticipants),
                    RecipientsCc(ccParticipants),
                    RecipientsBcc(bccParticipants)
                )

                if (shouldSkipSave(draftFields)) {
                    Timber.d("Not saving draft - shouldSkipSave")
                    return@launch
                }

                updateRecipients(toParticipants, ccParticipants, bccParticipants)
                    .onLeft { saveError ->
                        emitNewStateFor(EffectsEvent.ErrorEvent.OnStoreRecipientError(saveError))
                    }

                storeDraftWithSubject(it.subject)
                    .onLeft { saveError ->
                        emitNewStateFor(EffectsEvent.ErrorEvent.OnStoreSubjectError(saveError))
                    }

                storeDraftWithBody(it.body)
                    .onLeft { saveError ->
                        emitNewStateFor(EffectsEvent.ErrorEvent.OnStoreBodyError(saveError))
                    }

                savedStateHandle[ComposerScreen.HasSavedDraftKey] = true
                Timber.d("Draft saved.")
            }
        }.launchIn(viewModelScope)
    }

    @Suppress("ReturnCount")
    private suspend fun setupInitialState(savedStateHandle: SavedStateHandle): Boolean {
        val inputDraftId = savedStateHandle.get<String>(ComposerScreen.DraftMessageIdKey)
        val draftAction = savedStateHandle.get<String>(ComposerScreen.SerializedDraftActionKey)
            ?.deserialize<DraftAction>()

        val restoredHandle = savedStateHandle.get<Boolean>(ComposerScreen.HasSavedDraftKey) == true

        when {
            restoredHandle -> {
                onComposerRestored()
                return false
            }

            inputDraftId != null -> prefillWithExistingDraft(inputDraftId).onLeft {
                return false
            }

            draftAction != null -> prefillForDraftAction(draftAction).onLeft {
                return false
            }

            else -> prefillForNewDraft().onLeft {
                return false
            }
        }
        return true
    }

    private fun observeValidatedRecipients() {
        observeRecipientsValidation()
            .mapLatest { validated ->
                if (isEncryptionInfoEnabled.await()) validated else validated.withoutPrivacyLocks()
            }
            .onEach {
                recipientsStateManager.setFromDraftRecipients(it.toRecipients, it.ccRecipients, it.bccRecipients)
            }
            .launchIn(viewModelScope)
    }

    private fun ValidatedRecipients.withoutPrivacyLocks() = ValidatedRecipients(
        toRecipients = toRecipients.stripPrivacyLocks(),
        ccRecipients = ccRecipients.stripPrivacyLocks(),
        bccRecipients = bccRecipients.stripPrivacyLocks()
    )

    private fun List<DraftRecipient>.stripPrivacyLocks() = map { recipient ->
        when (recipient) {
            is DraftRecipient.SingleRecipient -> recipient.copy(privacyLock = PrivacyLock.None)
            is DraftRecipient.GroupRecipient -> recipient.copy(
                recipients = recipient.recipients.map { it.copy(privacyLock = PrivacyLock.None) }
            )
        }
    }

    private fun observeMessagePassword() {
        observeMessagePasswordChanged()
            .mapLatest { isMessagePasswordSet() }
            .onEach { emitNewStateFor(AccessoriesEvent.OnPasswordChanged(it)) }
            .launchIn(viewModelScope)
    }

    private suspend fun initMessageExpiration() {
        getMessageExpirationTime()
            .onRight { emitNewStateFor(AccessoriesEvent.OnExpirationChanged(it.toUiModel())) }
    }

    private suspend fun observeSenderValidationError() {
        getDraftSenderValidationError()?.let {
            emitNewStateFor(EffectsEvent.DraftEvent.OnSenderValidationError(it))
        }
    }

    private fun onComposerRestored() {
        // This is hit when process death occurs and the user could be in an inconsistent state:
        // Theoretically we can restore the draft from local storage, but it's not guaranteed that its content is
        // up to date and we don't know if it should overwrite the remote state.
        Timber.tag("ComposerViewModel").d("Restored Composer instance - navigating back.")
        emitNewStateFor(EffectsEvent.ComposerControlEvent.OnCloseRequest)
    }

    private fun prefillForComposeToAction(recipients: List<RecipientUiModel>) {
        recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.TO)
    }

    private suspend fun prefillForNewDraft(): Either<OpenDraftError, DraftFields> {
        // Emitting also for "empty draft" as now signature is returned with the init body, effectively
        // making this the same as other prefill cases (eg. "reply" or "fw")
        return createEmptyDraft(primaryUserId())
            .onRight { draftFields ->
                // ensure the body fields properties the UI uses to set initial values are up-to-date with the signature
                draftHead.value = draftFields.bodyFields.head
                bodyTextField.replaceText(draftFields.bodyFields.body.value, resetRange = true)

                emitNewStateFor(
                    CompositeEvent.DraftContentReady(
                        draftUiModel = draftFields.toDraftUiModel(),
                        isDataRefreshed = true,
                        bodyShouldTakeFocus = false
                    )
                )
            }
            .onLeft { emitNewStateFor(EffectsEvent.DraftEvent.OnDraftCreationFailed) }
    }

    private suspend fun prefillForShareDraftAction(shareDraftAction: PrefillForShare) {
        val fileShareInfo = shareDraftAction.intentShareInfo.decode()

        fileShareInfo.attachmentUris.takeIfNotEmpty()?.let { rawUri ->
            val uriList = rawUri.map { it.toUri() }
            onAttachmentsAdded(uriList)
        }

        if (fileShareInfo.hasEmailData()) {
            val draftFields = prefillDraftFieldsFromShareInfo(fileShareInfo)
            initComposerFields(draftFields)

            emitNewStateFor(
                CompositeEvent.DraftContentUpdated(
                    draftUiModel = draftFields.toDraftUiModel(),
                    shouldForceReload = true
                )
            )
        }
    }

    private fun prefillDraftFieldsFromShareInfo(intentShareInfo: IntentShareInfo): DraftFields {
        val emailBody = when (composerStates.value.main.draftType) {
            DraftMimeType.PlainText -> intentShareInfo.emailBody
            DraftMimeType.Html -> intentShareInfo.emailBody?.replace("\n", "<br>")
        }
        val draftBody = DraftBody(
            // Temporarily concatenate the shared text + the initial Rust body (to include the signature if present)
            buildString {
                append(emailBody ?: "")
                appendLine()
                append(bodyTextField.text)
            }
        )
        val subject = Subject(intentShareInfo.emailSubject ?: "")
        val bodyFields = BodyFields(draftHead.value, draftBody)
        val recipientsTo = RecipientsTo(
            intentShareInfo.emailRecipientTo.takeIfNotEmpty()?.map {
                RecipientUiModel.Valid(it).toDraftRecipient()
            } ?: emptyList()
        )

        val recipientsCc = RecipientsCc(
            intentShareInfo.emailRecipientCc.takeIfNotEmpty()?.map {
                RecipientUiModel.Valid(it).toDraftRecipient()
            } ?: emptyList()
        )

        val recipientsBcc = RecipientsBcc(
            intentShareInfo.emailRecipientBcc.takeIfNotEmpty()?.map {
                RecipientUiModel.Valid(it).toDraftRecipient()
            } ?: emptyList()
        )

        return DraftFields(
            currentSenderEmail(),
            subject,
            bodyFields,
            currentMimeType(),
            recipientsTo,
            recipientsCc,
            recipientsBcc
        )
    }

    private suspend fun prefillForDraftAction(draftAction: DraftAction): Either<OpenDraftError, DraftFields> {
        Timber.d("Opening composer for draft action $draftAction")
        emitNewStateFor(MainEvent.InitialLoadingToggled)
        val focusDraftBody = draftAction is Reply || draftAction is ReplyAll
        return when (draftAction) {
            Compose -> prefillForNewDraft()
            is ComposeToAddresses -> {
                prefillForNewDraft().onRight {
                    prefillForComposeToAction(draftAction.extractRecipients())
                }
            }

            is Forward,
            is Reply,
            is ReplyAll,
            is MailTo -> createDraftForAction(primaryUserId(), draftAction)
                .onRight { draftFields ->
                    initComposerFields(draftFields)
                    initMessageExpiration()
                    emitNewStateFor(
                        CompositeEvent.DraftContentReady(
                            draftUiModel = draftFields.toDraftUiModel(),
                            isDataRefreshed = true,
                            bodyShouldTakeFocus = focusDraftBody
                        )
                    )
                }
                .onLeft { emitNewStateFor(EffectsEvent.DraftEvent.OnDraftCreationFailed) }

            is PrefillForShare -> {
                prefillForNewDraft().onRight {
                    prefillForShareDraftAction(draftAction)
                }
            }
        }
    }

    private suspend fun prefillWithExistingDraft(
        inputDraftId: String
    ): Either<OpenDraftError, DraftFieldsWithSyncStatus> {
        Timber.d("Opening composer with $inputDraftId")
        emitNewStateFor(MainEvent.InitialLoadingToggled)

        return openExistingDraft(primaryUserId(), MessageId(inputDraftId))
            .onRight { draftFieldsWithSyncStatus ->
                val draftFields = draftFieldsWithSyncStatus.draftFields
                initComposerFields(draftFields)
                emitNewStateFor(
                    CompositeEvent.DraftContentReady(
                        draftUiModel = draftFields.toDraftUiModel(),
                        isDataRefreshed = draftFieldsWithSyncStatus is DraftFieldsWithSyncStatus.Remote,
                        bodyShouldTakeFocus = draftFields.hasAnyRecipient()
                    )
                )
            }
            .onLeft { emitNewStateFor(EffectsEvent.DraftEvent.OnDraftLoadingFailed) }
    }

    private suspend fun DraftFields.toDraftUiModel(): DraftUiModel {
        val draftDisplayBody = buildDraftDisplayBody(this.bodyFields.head, this.bodyFields.body)
        return DraftUiModel(this, draftDisplayBody)
    }

    internal fun submit(action: ComposerAction) {
        viewModelScope.launch {
            composerActionsChannel.send(action)
            logViewModelAction(action, "Enqueued")
        }
    }

    private suspend fun processActions() {
        composerActionsChannel.consumeEach { action ->
            logViewModelAction(action, "Executing")
            when (action) {
                is ComposerAction.ChangeSender -> onChangeSenderRequested()
                is ComposerAction.SetSenderAddress -> onChangeSender(action.sender)

                is ComposerAction.OpenExpirationSettings -> emitNewStateFor(EffectsEvent.SetExpirationReady)

                is ComposerAction.SetMessageExpiration -> onSetMessageExpiration(action.expirationTime)

                is ComposerAction.AddAttachmentsRequested ->
                    emitNewStateFor(EffectsEvent.AttachmentEvent.OnAttachFromOptionsRequest)

                is ComposerAction.OpenCameraPicker ->
                    emitNewStateFor(EffectsEvent.AttachmentEvent.OnAddFromCameraRequest)

                is ComposerAction.OpenPhotosPicker -> emitNewStateFor(EffectsEvent.AttachmentEvent.OnAddMediaRequest)
                is ComposerAction.OpenFilePicker -> emitNewStateFor(EffectsEvent.AttachmentEvent.OnAddFileRequest)

                is ComposerAction.AddFileAttachments -> onFileAttachmentsAdded(action.uriList)
                is ComposerAction.AddAttachments -> onAttachmentsAdded(action.uriList)
                is ComposerAction.RemoveAttachment -> onAttachmentsRemoved(action.attachmentId)
                is ComposerAction.RemoveInlineAttachment -> onRemoveInlineImage(action.contentId, true)
                is ComposerAction.InlineAttachmentDeletedFromBody -> onRemoveInlineImage(action.contentId, false)
                is ComposerAction.InlineImageActionsRequested ->
                    emitNewStateFor(EffectsEvent.AttachmentEvent.OnInlineImageActionsRequested)

                is ComposerAction.CloseComposer -> onCloseComposer()
                is ComposerAction.SendMessage -> handleOnSendMessage()

                is ComposerAction.CancelSendWithNoSubject ->
                    emitNewStateFor(EffectsEvent.SendEvent.OnCancelSendNoSubject)

                is ComposerAction.ConfirmSendWithNoSubject -> onSendMessage()

                is ComposerAction.ConfirmSendExpirationSetToExternal -> onSendMessage()

                is ComposerAction.ClearSendingError -> TODO()

                is ComposerAction.DiscardDraftRequested ->
                    emitNewStateFor(EffectsEvent.DraftEvent.OnDiscardDraftRequested)

                is ComposerAction.DiscardDraftConfirmed -> onDiscardDraftConfirmed()
                is ComposerAction.OnScheduleSendRequested -> onScheduleSendRequested()
                is ComposerAction.OnScheduleSend -> handleOnScheduleSendMessage(action.time)
                is ComposerAction.AcknowledgeAttachmentErrors -> handleConfirmAttachmentErrors(action)
                is ComposerAction.ConvertInlineToAttachment -> handleConvertInlineToAttachment(action)
            }
            logViewModelAction(action, "Completed.")
        }
    }

    private suspend fun onSetMessageExpiration(expirationTime: ExpirationTimeUiModel) {
        saveMessageExpirationTime(expirationTime.toDomainModel())
            .onLeft { emitNewStateFor(EffectsEvent.ErrorEvent.OnSetExpirationError) }
            .onRight { emitNewStateFor(AccessoriesEvent.OnExpirationChanged(expirationTime)) }
    }

    private suspend fun onChangeSender(sender: SenderUiModel) {
        val newSender = SenderEmail(sender.email)

        // Keep previous state to restore on failure and clear encryption info
        // before changing sender to avoid race conditions with Rust callbacks.
        val previousRecipientsState = recipientsStateManager.recipients.value
        recipientsStateManager.resetValidationState()

        changeSenderAddress(newSender)
            .onLeft { error ->
                // Restore previous encryption info since sender change failed
                recipientsStateManager.restoreState(previousRecipientsState)

                when (error) {
                    is ChangeSenderError.AddressCanNotSend,
                    is ChangeSenderError.AddressDisabled,
                    is ChangeSenderError.AddressNotFound ->
                        emitNewStateFor(EffectsEvent.ErrorEvent.OnAddressNotValidForSending)

                    is ChangeSenderError.Other -> emitNewStateFor(EffectsEvent.ErrorEvent.OnChangeSenderFailure)
                    ChangeSenderError.RefreshBodyError -> emitNewStateFor(EffectsEvent.ErrorEvent.OnRefreshBodyFailed)
                }

            }
            .onRight { bodyWithNewSignature ->
                draftHead.value = bodyWithNewSignature.head
                bodyTextField.replaceText(bodyWithNewSignature.body.value)

                // This needs to be created directly as we're emitting a state change.
                val draftDisplayBody = buildDraftDisplayBody(
                    bodyWithNewSignature.head,
                    bodyWithNewSignature.body
                )

                emitNewStateFor(CompositeEvent.UserChangedSender(newSender, draftDisplayBody))
            }
    }

    private suspend fun onChangeSenderRequested() {
        getSenderAddresses()
            .onLeft {
                emitNewStateFor(EffectsEvent.ErrorEvent.OnGetAddressesError)
            }
            .onRight { senderAddresses ->
                val addresses = senderAddresses.addresses.map { SenderUiModel(it.value) }
                emitNewStateFor(CompositeEvent.SenderAddressesListReady(addresses))
            }
    }

    private suspend fun handleConvertInlineToAttachment(action: ComposerAction.ConvertInlineToAttachment) {
        convertInlineToAttachment(action.contentId)
            .onLeft {
                Timber.e("composer: failed to convert inline to attachment - contentId: ${action.contentId}")
                emitNewStateFor(EffectsEvent.AttachmentEvent.InlineAttachmentConversionFailed(it))
            }
            .onRight {
                Timber.d("composer: inline attachment ${action.contentId} converted to standard")
                emitNewStateFor(EffectsEvent.AttachmentEvent.StripInlineAttachmentFromBody(action.contentId))
            }
    }

    private suspend fun handleConfirmAttachmentErrors(action: ComposerAction.AcknowledgeAttachmentErrors) {
        val attachmentsWithError = action.attachmentsWithError
        if (attachmentsWithError.isEmpty()) {
            Timber.w("composer: No attachments with error to handle")
            return
        }

        var errorDeletingAttachments: AttachmentDeleteError? = null
        attachmentsWithError.forEach { attachmentId ->
            deleteAttachment(attachmentId)
                .onLeft {
                    Timber.e("Failed to delete attachment: $it")
                    errorDeletingAttachments = it
                }

            errorDeletingAttachments?.let {
                emitNewStateFor(EffectsEvent.AttachmentEvent.RemoveAttachmentError(it))
            }
        }
    }

    fun loadImage(url: String): MessageBodyImage? = if (isComposerActive()) {
        loadMessageBodyImage(url).getOrNull()
    } else {
        Timber.d("Not loading image, composer not active")
        null
    }

    private suspend fun onScheduleSendRequested() {
        getFormattedScheduleSendOptions()
            .onLeft { emitNewStateFor(EffectsEvent.ErrorEvent.OnGetScheduleSendOptionsError) }
            .onRight { emitNewStateFor(CompositeEvent.ScheduleSendOptionsReady(it)) }
    }

    private fun observeAttachments() {
        viewModelScope.launch {
            observeMessageAttachments().onEach { result ->
                result.onLeft {
                    Timber.e("Failed to observe message attachments: $it")
                    emitNewStateFor(EffectsEvent.AttachmentEvent.OnLoadAttachmentsFailed)
                }.onRight { attachments ->
                    emitNewStateFor(CompositeEvent.AttachmentListChanged(attachments))
                }
            }.launchIn(this)
        }
    }

    private fun validateEmailAddress(emailAddress: String): Boolean = isValidEmailAddress(emailAddress)

    private suspend fun onAttachmentsAdded(uriList: List<Uri>) {
        val contentIds = mutableListOf<String>()
        val draftMimeType = currentMimeType()

        uriList.forEach { uri ->
            addAttachment(uri, draftMimeType).onLeft {
                Timber.e("Failed to add attachment: $it")
                emitNewStateFor(EffectsEvent.AttachmentEvent.AddAttachmentError(it))
            }.onRight {
                when (it) {
                    is AddAttachment.AddAttachmentResult.InlineAttachmentAdded -> {
                        contentIds.add(it.cid)
                    }

                    AddAttachment.AddAttachmentResult.StandardAttachmentAdded -> Unit
                }
            }
        }

        if (contentIds.isNotEmpty()) {
            emitNewStateFor(EffectsEvent.AttachmentEvent.InlineAttachmentsAdded(contentIds))
        }
    }

    private suspend fun onFileAttachmentsAdded(uriList: List<Uri>) {
        uriList.forEach { uri ->
            addAttachment.forcingStandardDisposition(uri).onLeft {
                Timber.e("Failed to add standard attachment: $it")
                emitNewStateFor(EffectsEvent.AttachmentEvent.AddAttachmentError(it))
            }
        }
    }

    private suspend fun onAttachmentsRemoved(attachmentId: AttachmentId) {
        deleteAttachment(attachmentId)
            .onLeft {
                Timber.e("Failed to delete attachment: $it")
                emitNewStateFor(EffectsEvent.AttachmentEvent.RemoveAttachmentError(it))
            }
    }

    private suspend fun onRemoveInlineImage(contentId: String, stripFromBody: Boolean) {
        deleteInlineAttachment(contentId)
            .onLeft { Timber.w("Failed to delete inline attachment: $contentId reason: $it") }
            .onRight {
                if (stripFromBody) {
                    emitNewStateFor(EffectsEvent.AttachmentEvent.StripInlineAttachmentFromBody(contentId))
                }
            }
    }

    private suspend fun onCloseComposer() {
        emitNewStateFor(MainEvent.CoreLoadingToggled)
        pendingStoreDraftJob?.cancel()

        val draftFields = currentDraftFields()

        if (!shouldSkipSave(draftFields)) {
            forceDraftSave(draftFields).onLeft { saveError ->
                emitNewStateFor(MainEvent.LoadingDismissed)
                emitNewStateFor(EffectsEvent.ErrorEvent.OnFinalSaveError(saveError))
                return
            }
        }

        val event = getDraftId().fold(
            ifLeft = { EffectsEvent.ComposerControlEvent.OnCloseRequest },
            ifRight = { EffectsEvent.ComposerControlEvent.OnCloseRequestWithDraft(it) }
        )
        emitNewStateFor(event)
    }

    private suspend fun handleOnSendMessage() {
        emitNewStateFor(MainEvent.CoreLoadingToggled)

        if (subjectTextField.text.isBlank()) {
            emitNewStateFor(CompositeEvent.OnSendWithEmptySubject)
            return
        }

        when (val result = canSendWithExpirationTime().getOrNull()) {
            SendWithExpirationTimeResult.CanSend -> onSendMessage()
            is SendWithExpirationTimeResult.ExpirationUnsupportedForSome -> {
                emitNewStateFor(CompositeEvent.OnSendWithExpirationUnsupportedForSome(result.recipients))
            }

            is SendWithExpirationTimeResult.ExpirationSupportUnknown -> {
                emitNewStateFor(CompositeEvent.OnSendWithExpirationSupportUnknown)
            }

            null -> {
                emitNewStateFor(CompositeEvent.OnSendWithExpirationSupportUnknown)
            }

            SendWithExpirationTimeResult.ExpirationUnsupportedForAll ->
                emitNewStateFor(CompositeEvent.OnSendWithExpirationUnsupported)
        }

    }

    private suspend fun onSendMessage() {
        pendingStoreDraftJob?.cancel()

        forceDraftSave(currentDraftFields()).onLeft { saveError ->
            emitNewStateFor(MainEvent.LoadingDismissed)
            emitNewStateFor(EffectsEvent.ErrorEvent.OnFinalSaveError(saveError))
            return
        }

        sendMessage().fold(
            ifLeft = {
                Timber.w("composer: Send message failed. Error: $it")
                emitNewStateFor(EffectsEvent.ErrorEvent.OnSendMessageError(it))
            },
            ifRight = {
                appEventBroadcaster.emit(AppEvent.MessageSent)
                if (networkManager.isConnectedToNetwork()) {
                    emitNewStateFor(EffectsEvent.SendEvent.OnSendMessage)
                } else {
                    emitNewStateFor(EffectsEvent.SendEvent.OnOfflineSendMessage)
                }
            }
        )
    }

    private suspend fun forceDraftSave(fields: DraftFields): Either<SaveDraftError, Unit> = either {
        updateRecipients(
            toRecipients = fields.recipientsTo.value,
            ccRecipients = fields.recipientsCc.value,
            bccRecipients = fields.recipientsBcc.value
        )
            .onLeft { saveError ->
                raise(saveError)
            }

        storeDraftWithSubject(fields.subject)
            .onLeft { saveError ->
                raise(saveError)
            }

        storeDraftWithBody(fields.bodyFields.body)
            .onLeft { saveError ->
                raise(saveError)
            }

        savedStateHandle[ComposerScreen.HasSavedDraftKey] = true
    }

    @Suppress("ReturnCount")
    private fun shouldSkipSave(fields: DraftFields): Boolean {
        val accessoriesState = composerStates.value.accessories
        val attachmentsState = composerStates.value.attachments

        if (accessoriesState != ComposerState.Accessories.initial() ||
            attachmentsState != ComposerState.Attachments.initial()
        ) {
            return false
        }

        if (!fields.haveBlankSubject()) return false

        if (fields.haveBlankRecipients() &&
            fields.haveBlankSubject() &&
            fields.bodyFields.body.value.isEmpty()
        ) {
            return true
        }

        return false
    }

    private suspend fun currentDraftFields() = withContext(defaultDispatcher) {
        val (toParticipants, ccParticipants, bccParticipants) =
            recipientsStateManager.recipients.value.toDraftRecipients()

        val bodyFields = BodyFields(draftHead.value, DraftBody(bodyTextField.text.toString()))

        DraftFields(
            currentSenderEmail(),
            Subject(subjectTextField.text.toString().stripNewLines()),
            bodyFields,
            currentMimeType(),
            RecipientsTo(toParticipants),
            RecipientsCc(ccParticipants),
            RecipientsBcc(bccParticipants)
        )
    }

    private suspend fun handleOnScheduleSendMessage(time: Instant) {
        emitNewStateFor(MainEvent.CoreLoadingToggled)

        onScheduleSend(time)
    }

    private suspend fun onScheduleSend(time: Instant) = scheduleSend(time)
        .onLeft { emitNewStateFor(EffectsEvent.ErrorEvent.OnSendMessageError(it)) }
        .onRight {
            if (networkManager.isConnectedToNetwork()) {
                emitNewStateFor(EffectsEvent.SendEvent.OnScheduleSendMessage)
            } else {
                emitNewStateFor(EffectsEvent.SendEvent.OnOfflineScheduleSendMessage)
            }
        }

    private fun onDiscardDraftConfirmed() {
        viewModelScope.launch {
            getDraftId()
                .onLeft { emitNewStateFor(EffectsEvent.ComposerControlEvent.OnCloseRequestWithDraftDiscarded) }
                .onRight {
                    discardDraft(primaryUserId(), it)
                        .onLeft { emitNewStateFor(EffectsEvent.ErrorEvent.OnDiscardDraftError) }
                        .onRight { emitNewStateFor(EffectsEvent.ComposerControlEvent.OnCloseRequestWithDraftDiscarded) }
                }
        }
    }

    private fun isComposerActive(): Boolean = composerRegistry.isActive(composerInstanceId)

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun currentSenderEmail() = SenderEmail(composerStates.value.main.sender.email)

    private fun currentMimeType(): DraftMimeType = composerStates.value.main.draftType

    private fun initComposerFields(draftFields: DraftFields) {
        draftHead.value = draftFields.bodyFields.head
        subjectTextField.replaceText(draftFields.subject.value)
        bodyTextField.replaceText(draftFields.bodyFields.body.value, resetRange = true)
        recipientsStateManager.setFromDraftRecipients(
            toRecipients = draftFields.recipientsTo.value,
            ccRecipients = draftFields.recipientsCc.value,
            bccRecipients = draftFields.recipientsBcc.value
        )
    }

    private fun emitNewStateFor(event: ComposerStateEvent) {
        mutableComposerStates.update { composerStateReducer.reduceNewState(it, event) }
    }

    private fun ComposeToAddresses.extractRecipients(): List<RecipientUiModel> {
        return this.recipients.map { recipient ->
            when {
                validateEmailAddress(recipient) -> RecipientUiModel.Valid(recipient)
                else -> RecipientUiModel.Invalid(recipient)
            }
        }
    }

    private fun String.stripNewLines() = this.replace("[\n\r]".toRegex(), " ")

    private fun logViewModelAction(action: ComposerAction, message: String) {
        Timber
            .tag("ComposerViewModel")
            .d("Action ${action::class.java.simpleName} ${System.identityHashCode(action)} - $message")
    }

    fun sanitizePastedText(mimeType: String?, text: String): String {
        val pasteMimeType = PasteMimeType.fromJs(mimeType)
        return runCatching { sanitizePastedContent(text, pasteMimeType) }
            .onFailure { Timber.e(it, "sanitizePastedContent failed!") }
            .getOrElse { text }
    }

    @AssistedFactory
    interface Factory {

        fun create(composerInstanceId: String, recipientsStateManager: RecipientsStateManager): ComposerViewModel
    }

    companion object {

        internal const val maxContactAutocompletionCount = 100
    }
}
