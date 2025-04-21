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

package ch.protonmail.android.maildetail.presentation.viewmodel

import java.util.concurrent.CopyOnWriteArrayList
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.domain.usecase.DelayedMarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDetailBottomSheetActions
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveRemoteMessageAndLocalConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveRemoteMessageAndLocalConversation.ConversationLabelingOptions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.domain.usecase.SetMessageViewState
import ch.protonmail.android.maildetail.presentation.GetMessageIdToExpand
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent.MessageMoved
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.CollapseMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.DismissBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.DoNotAskLinkConfirmationAgain
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ExpandMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.LabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.LabelAsToggleAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MarkUnread
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MessageBodyLinkClicked
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToDestinationConfirmed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToDestinationSelected
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestContactActionsBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestConversationLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestMoveToBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestScrollTo
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ScrollRequestCompleted
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ShowAllAttachmentsForMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.Star
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.Trash
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.UnStar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.MessageBody
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.LoadDataForMessageLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.usecase.OnMessageLabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.usecase.PrintMessage
import ch.protonmail.android.maildetail.presentation.usecase.ShouldMessageBeHidden
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.UpdateCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class ConversationDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val messageIdUiModelMapper: MessageIdUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper,
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper,
    private val markConversationAsUnread: MarkConversationAsUnread,
    private val moveConversation: MoveConversation,
    private val deleteConversations: DeleteConversations,
    private val relabelConversation: RelabelConversation,
    private val observeContacts: ObserveContacts,
    private val observeConversation: ObserveConversation,
    private val observeConversationMessages: ObserveConversationMessagesWithLabels,
    private val observeDetailActions: ObserveConversationDetailActions,
    private val getBottomSheetActions: GetDetailBottomSheetActions,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val observeAutoDeleteSetting: ObserveAutoDeleteSetting,
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val observeMessage: ObserveMessage,
    private val observeMessageAttachmentStatus: ObserveMessageAttachmentStatus,
    private val getDownloadingAttachmentsForMessages: GetDownloadingAttachmentsForMessages,
    private val reducer: ConversationDetailReducer,
    private val starConversations: StarConversations,
    private val unStarConversations: UnStarConversations,
    private val savedStateHandle: SavedStateHandle,
    private val getDecryptedMessageBody: GetDecryptedMessageBody,
    private val markMessageAndConversationReadIfAllMessagesRead: DelayedMarkMessageAndConversationReadIfAllMessagesRead,
    private val setMessageViewState: SetMessageViewState,
    private val observeConversationViewState: ObserveConversationViewState,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val getEmbeddedImageAvoidDuplicatedExecution: GetEmbeddedImageAvoidDuplicatedExecution,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val observePrivacySettings: ObservePrivacySettings,
    private val updateLinkConfirmationSetting: UpdateLinkConfirmationSetting,
    private val resolveParticipantName: ResolveParticipantName,
    private val reportPhishingMessage: ReportPhishingMessage,
    private val isProtonCalendarInstalled: IsProtonCalendarInstalled,
    private val networkManager: NetworkManager,
    private val printMessage: PrintMessage,
    private val markMessageAsUnread: MarkMessageAsUnread,
    private val findContactByEmail: FindContactByEmail,
    private val getMessageIdToExpand: GetMessageIdToExpand,
    private val loadDataForMessageLabelAsBottomSheet: LoadDataForMessageLabelAsBottomSheet,
    private val onMessageLabelAsConfirmed: OnMessageLabelAsConfirmed,
    private val moveRemoteMessageAndLocalConversation: MoveRemoteMessageAndLocalConversation,
    private val observeMailLabels: ObserveMailLabels,
    private val shouldMessageBeHidden: ShouldMessageBeHidden,
    private val observeCustomizeToolbarSpotlight: ObserveCustomizeToolbarSpotlight,
    private val updateCustomizeToolbarSpotlight: UpdateCustomizeToolbarSpotlight
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )
        .filterNotNull()

    private val mutableDetailState = MutableStateFlow(initialState)
    private val conversationId = requireConversationId()
    private val initialScrollToMessageId = getInitialScrollToMessageId()
    private val filterByLocation = getFilterByLocation()
    private val attachmentsState = MutableStateFlow<Map<MessageId, List<MessageAttachment>>>(emptyMap())

    val state: StateFlow<ConversationDetailState> = mutableDetailState.asStateFlow()

    private val jobs = CopyOnWriteArrayList<Job>()

    init {
        Timber.d("Open detail screen for conversation ID: $conversationId")
        setupObservers()
    }

    private fun setupObservers() {
        jobs.addAll(
            listOf(
                observeConversationMetadata(conversationId),
                observeConversationMessages(conversationId),
                observeBottomBarActions(conversationId),
                observePrivacySettings(),
                observeAttachments(),
                observeCustomizeToolbarSpotlights()
            )
        )
    }

    private suspend fun stopAllJobs() {
        jobs.forEach { it.cancelAndJoin() }
        jobs.clear()
    }

    private suspend fun restartAllJobs() {
        stopAllJobs()
        setupObservers()
    }

    @Suppress("ComplexMethod")
    fun submit(action: ConversationDetailViewAction) {
        when (action) {
            is Star -> starConversation()
            is UnStar -> unStarConversation()
            is MarkUnread -> markAsUnread()
            is Trash -> moveConversationToTrash()
            is ConversationDetailViewAction.DeleteConfirmed -> handleDeleteConfirmed(action)
            is RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(action)
            is MoveToDestinationConfirmed -> onMoveToDestinationConfirmed(action.mailLabelText, action.entryPoint)
            is RequestConversationLabelAsBottomSheet -> showConversationLabelAsBottomSheet(action)
            is RequestContactActionsBottomSheet -> showContactActionsBottomSheetAndLoadData(action)
            is LabelAsConfirmed -> onLabelAsConfirmed(action)
            is ConversationDetailViewAction.RequestMoreActionsBottomSheet ->
                showMoreActionsBottomSheetAndLoadData(action)
            is ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet ->
                showConversationMoreActionsBottomSheet()

            is ConversationDetailViewAction.RequestMessageLabelAsBottomSheet -> showMessageLabelAsBottomSheet(action)
            is ConversationDetailViewAction.RequestMessageMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(action)

            is ExpandMessage -> onExpandMessage(action.messageId, null)
            is CollapseMessage -> onCollapseMessage(action.messageId)
            is DoNotAskLinkConfirmationAgain -> onDoNotAskLinkConfirmationChecked()
            is ShowAllAttachmentsForMessage -> showAllAttachmentsForMessage(action.messageId)
            is ConversationDetailViewAction.OnAttachmentClicked -> {
                onOpenAttachmentClicked(action.messageId, action.attachmentId)
            }

            is ConversationDetailViewAction.ReportPhishing -> handleReportPhishing(action)
            is ConversationDetailViewAction.ReportPhishingConfirmed -> handleReportPhishingConfirmed(action)
            is ConversationDetailViewAction.OpenInProtonCalendar -> handleOpenInProtonCalendar(action)
            is ConversationDetailViewAction.Print -> handlePrint(action.context, action.messageId)
            is ConversationDetailViewAction.MarkMessageUnread -> handleMarkMessageUnread(action)
            is ConversationDetailViewAction.MoveMessage -> handleMoveMessage(action)
            is ConversationDetailViewAction.ChangeVisibilityOfMessages -> handleChangeVisibilityOfMessages()

            is ConversationDetailViewAction.DeleteRequested,
            is ConversationDetailViewAction.DeleteDialogDismissed,
            is DismissBottomSheet,
            is MoveToDestinationSelected,
            is LabelAsToggleAction,
            is MessageBodyLinkClicked,
            is RequestScrollTo,
            is ScrollRequestCompleted,
            is ConversationDetailViewAction.ExpandOrCollapseMessageBody,
            is ConversationDetailViewAction.ShowEmbeddedImages,
            is ConversationDetailViewAction.LoadRemoteAndEmbeddedContent,
            is ConversationDetailViewAction.LoadRemoteContent,
            is ConversationDetailViewAction.ReportPhishingDismissed,
            ConversationDetailViewAction.SpotlightDismissed,
            is ConversationDetailViewAction.SwitchViewMode,
            is ConversationDetailViewAction.PrintRequested -> directlyHandleViewAction(action)

            is ConversationDetailViewAction.ReplyToLastMessage -> replyToLastMessage(action)
            is ConversationDetailViewAction.ForwardLastMessage -> forwardLastMessage()
            is ConversationDetailViewAction.Archive -> archiveConversation()
            is ConversationDetailViewAction.ReportPhishingLastMessage -> reportPhishingLastMessage()
            is ConversationDetailViewAction.MoveToSpam -> moveToSpam()
            is ConversationDetailViewAction.PrintLastMessage -> printLastMessage(action.context)

            is ConversationDetailViewAction.SpotlightDisplayed -> updateSpotlightLastSeenTimestamp()
            is ConversationDetailViewAction.EffectConsumed -> consumeEffect(action)
        }
    }

    fun loadEmbeddedImage(messageId: MessageId?, contentId: String) = messageId?.let {
        runBlocking {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId = primaryUserId.first(),
                messageId = it,
                contentId = contentId,
                coroutineContext = viewModelScope.coroutineContext
            )
        }
    }

    private fun observePrivacySettings() = primaryUserId.flatMapLatest { userId ->
        observePrivacySettings(userId).mapLatest { either ->
            either.fold(
                ifLeft = { Timber.e("Error getting Privacy Settings for user: $userId") },
                ifRight = { privacySettings ->
                    mutableDetailState.emit(
                        mutableDetailState.value.copy(
                            requestLinkConfirmation = privacySettings.requestLinkConfirmation
                        )
                    )
                }
            )
        }
    }.launchIn(viewModelScope)

    private fun observeConversationMetadata(conversationId: ConversationId) = primaryUserId.flatMapLatest { userId ->
        observeConversation(userId, conversationId, refreshData = true)
            .mapLatest { either ->
                either.fold(
                    ifLeft = {
                        if (it.isOfflineError()) {
                            ConversationDetailEvent.NoNetworkError
                        } else {
                            ConversationDetailEvent.ErrorLoadingConversation
                        }
                    },
                    ifRight = { ConversationDetailEvent.ConversationData(conversationMetadataMapper.toUiModel(it)) }
                )
            }
    }
        .onEach { event ->
            emitNewStateFrom(event)
        }
        .launchIn(viewModelScope)

    private fun observeConversationMessages(conversationId: ConversationId) = primaryUserId
        .flatMapLatest { userId ->
            combine(
                observeContacts(userId),
                observeConversationMessages(userId, conversationId).ignoreLocalErrors(),
                observeFolderColor(userId),
                observeAutoDeleteSetting(),
                observeConversationViewState()
            ) { contactsEither, messagesEither, folderColorSettings, autoDelete, conversationViewState ->
                val contacts = contactsEither.getOrElse {
                    Timber.i("Failed getting contacts for displaying initials. Fallback to display name")
                    emptyList()
                }
                val messages = messagesEither.getOrElse {
                    return@combine ConversationDetailEvent.ErrorLoadingMessages
                }
                val messagesLabelIds = messages.associate { it.message.messageId to it.message.labelIds }
                val messagesUiModels = buildMessagesUiModels(
                    messages = messages,
                    contacts = contacts,
                    folderColorSettings = folderColorSettings,
                    autoDeleteSetting = autoDelete,
                    currentViewState = conversationViewState
                ).toImmutableList()

                Timber.i("Retrieved ${messagesUiModels.size} messages")

                val stateIsLoadingOrOffline = stateIsLoadingOrOffline()
                val initialScrollTo = initialScrollToMessageId
                    ?: getMessageIdToExpand(
                        messages, filterByLocation, conversationViewState.shouldHideMessagesBasedOnTrashFilter
                    )?.let { messageIdUiModelMapper.toUiModel(it) }
                if (stateIsLoadingOrOffline && initialScrollTo != null &&
                    allCollapsed(conversationViewState.messagesState)
                ) {
                    ConversationDetailEvent.MessagesData(
                        messagesUiModels,
                        messagesLabelIds,
                        initialScrollTo,
                        filterByLocation,
                        conversationViewState.shouldHideMessagesBasedOnTrashFilter
                    )
                } else {
                    val requestScrollTo = requestScrollToMessageId(conversationViewState.messagesState)
                    ConversationDetailEvent.MessagesData(
                        messagesUiModels,
                        messagesLabelIds,
                        requestScrollTo,
                        filterByLocation,
                        conversationViewState.shouldHideMessagesBasedOnTrashFilter
                    )
                }
            }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .onEach { event ->
            emitNewStateFrom(event)
        }
        .launchIn(viewModelScope)

    private fun stateIsLoadingOrOffline(): Boolean = state.value.messagesState.let {
        it == ConversationDetailsMessagesState.Loading || it is ConversationDetailsMessagesState.Offline
    }

    private fun allCollapsed(viewState: Map<MessageId, InMemoryConversationStateRepository.MessageState>): Boolean =
        viewState.values.all { it == InMemoryConversationStateRepository.MessageState.Collapsed }

    private suspend fun buildMessagesUiModels(
        messages: NonEmptyList<MessageWithLabels>,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting,
        currentViewState: InMemoryConversationStateRepository.MessagesState
    ): NonEmptyList<ConversationDetailMessageUiModel> {
        val messagesList = messages.map { messageWithLabels ->
            val existingMessageState = getExistingExpandedMessageUiState(messageWithLabels.message.messageId)
            if (
                shouldMessageBeHidden(
                    filterByLocation,
                    messageWithLabels.message.labelIds,
                    currentViewState.shouldHideMessagesBasedOnTrashFilter
                )
            ) {
                buildHiddenMessage(messageWithLabels)
            } else {
                when (val viewState = currentViewState.messagesState[messageWithLabels.message.messageId]) {
                    is InMemoryConversationStateRepository.MessageState.Expanding ->
                        buildExpandingMessage(
                            buildCollapsedMessage(
                                messageWithLabels,
                                contacts,
                                folderColorSettings,
                                autoDeleteSetting
                            )
                        )

                    is InMemoryConversationStateRepository.MessageState.Expanded -> {
                        buildExpandedMessage(
                            messageWithLabels,
                            existingMessageState,
                            contacts,
                            viewState.decryptedBody,
                            folderColorSettings,
                            autoDeleteSetting,
                            effect = viewState.effect
                        )
                    }

                    else -> {
                        buildCollapsedMessage(messageWithLabels, contacts, folderColorSettings, autoDeleteSetting)
                    }
                }
            }
        }
        return messagesList
    }

    private fun getExistingExpandedMessageUiState(messageId: MessageId): ConversationDetailMessageUiModel.Expanded? {
        return when (val messagesState = state.value.messagesState) {
            is ConversationDetailsMessagesState.Data -> {
                messagesState.messages
                    .filterIsInstance<ConversationDetailMessageUiModel.Expanded>()
                    .firstOrNull { it.messageId.id == messageId.id }
            }

            else -> null
        }
    }

    private fun requestScrollToMessageId(
        conversationViewState: Map<MessageId, InMemoryConversationStateRepository.MessageState>
    ): MessageIdUiModel? {
        val expandedMessageIds = conversationViewState
            .filterValues { it is InMemoryConversationStateRepository.MessageState.Expanded }
            .keys

        val requestScrollTo = if (conversationViewState.size == 1 && expandedMessageIds.size == 1) {
            messageIdUiModelMapper.toUiModel(expandedMessageIds.first())
        } else {
            val messageWithEffect = conversationViewState.entries.firstOrNull {
                (it.value as? InMemoryConversationStateRepository.MessageState.Expanded)?.effect != null
            }
            messageWithEffect?.let { messageIdUiModelMapper.toUiModel(it.key) }
        }
        return requestScrollTo
    }

    private fun buildHiddenMessage(messageWithLabels: MessageWithLabels): ConversationDetailMessageUiModel.Hidden =
        conversationMessageMapper.toUiModel(messageWithLabels)

    private suspend fun buildCollapsedMessage(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting
    ): ConversationDetailMessageUiModel.Collapsed = conversationMessageMapper.toUiModel(
        messageWithLabels,
        contacts,
        folderColorSettings,
        autoDeleteSetting
    )

    private fun buildExpandingMessage(
        collapsedMessage: ConversationDetailMessageUiModel.Collapsed
    ): ConversationDetailMessageUiModel.Expanding = conversationMessageMapper.toUiModel(
        collapsedMessage
    )

    private suspend fun buildExpandedMessage(
        messageWithLabels: MessageWithLabels,
        existingMessageUiState: ConversationDetailMessageUiModel.Expanded?,
        contacts: List<Contact>,
        decryptedBody: DecryptedMessageBody,
        folderColorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting,
        effect: InMemoryConversationStateRepository.PostExpandEffect?
    ): ConversationDetailMessageUiModel.Expanded = conversationMessageMapper.toUiModel(
        messageWithLabels = messageWithLabels,
        contacts = contacts,
        effect = effect,
        decryptedMessageBody = decryptedBody,
        folderColorSettings = folderColorSettings,
        autoDeleteSetting = autoDeleteSetting,
        userAddress = decryptedBody.userAddress,
        existingMessageUiState = existingMessageUiState
    )

    private fun observeBottomBarActions(conversationId: ConversationId) = primaryUserId.flatMapLatest { userId ->
        observeDetailActions(userId, conversationId, refreshConversations = false).mapLatest { either ->
            either.fold(
                ifLeft = { ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                ifRight = { actions ->
                    val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }.toImmutableList()
                    ConversationDetailEvent.ConversationBottomBarEvent(
                        BottomBarEvent.ShowAndUpdateActionsData(actionUiModels)
                    )
                }
            )
        }
    }
        .onEach { event ->
            emitNewStateFrom(event)
        }
        .launchIn(viewModelScope)

    private fun showMoveToBottomSheetAndLoadData(initialEvent: ConversationDetailViewAction) {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeDestinationMailLabels(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                ConversationDetailEvent.ConversationBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        moveToDestinations = folders.toUiModels(color).let {
                            it.folders + it.systems
                        }.toImmutableList(),
                        entryPoint = when (initialEvent) {
                            is ConversationDetailViewAction.RequestMessageMoveToBottomSheet ->
                                MoveToBottomSheetEntryPoint.Message(initialEvent.messageId)
                            else -> MoveToBottomSheetEntryPoint.Conversation
                        }
                    )
                )
            }
        }.onStart {
            emitNewStateFrom(initialEvent)
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showContactActionsBottomSheetAndLoadData(action: RequestContactActionsBottomSheet) {
        viewModelScope.launch {
            emitNewStateFrom(action)

            val userId = primaryUserId.first()
            val contact = findContactByEmail(userId, action.participant.participantAddress)

            val event = ConversationDetailEvent.ConversationBottomSheetEvent(
                ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData(
                    participant = Participant(
                        address = action.participant.participantAddress,
                        name = action.participant.participantName
                    ),
                    avatarUiModel = action.avatarUiModel,
                    contactId = contact?.id
                )
            )
            emitNewStateFrom(event)
        }
    }

    private fun showConversationLabelAsBottomSheet(initialEvent: ConversationDetailViewAction) {
        viewModelScope.launch {
            emitNewStateFrom(initialEvent)

            val userId = primaryUserId.first()
            val labels = observeCustomMailLabels(userId).first()
            val color = observeFolderColor(userId).first()
            val conversationWithMessagesAndLabels = observeConversationMessages(userId, conversationId).first()

            val mappedLabels = labels.onLeft {
                Timber.e("Error while observing custom labels")
            }.getOrElse { emptyList() }

            val messagesWithLabels = conversationWithMessagesAndLabels.onLeft {
                Timber.e("Error while observing conversation messages")
            }.getOrElse { emptyList() }

            val (selectedLabels, partiallySelectedLabels) = mappedLabels.getLabelSelectionState(messagesWithLabels)

            val event = ConversationDetailEvent.ConversationBottomSheetEvent(
                LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = mappedLabels.map { it.toCustomUiModel(color, emptyMap(), null) }
                        .toImmutableList(),
                    selectedLabels = selectedLabels.toImmutableList(),
                    partiallySelectedLabels = partiallySelectedLabels.toImmutableList(),
                    entryPoint = LabelAsBottomSheetEntryPoint.Conversation
                )
            )
            emitNewStateFrom(event)
        }
    }

    private fun showMessageLabelAsBottomSheet(
        initialEvent: ConversationDetailViewAction.RequestMessageLabelAsBottomSheet
    ) {
        viewModelScope.launch {
            emitNewStateFrom(initialEvent)

            val userId = primaryUserId.first()
            val event = ConversationDetailEvent.MessageBottomSheetEvent(
                loadDataForMessageLabelAsBottomSheet(userId, initialEvent.messageId)
            )
            emitNewStateFrom(event)
        }
    }

    private fun onLabelAsConfirmed(operation: LabelAsConfirmed) {
        when (operation.entryPoint) {
            LabelAsBottomSheetEntryPoint.Conversation -> onConversationLabelAsConfirmed(operation.archiveSelected)
            is LabelAsBottomSheetEntryPoint.Message ->
                onMessageLabelAsConfirmed(operation.archiveSelected, operation.entryPoint.messageId)
            else -> throw IllegalStateException("Invalid entry point for label as confirmed")
        }
    }

    private fun onMessageLabelAsConfirmed(archiveSelected: Boolean, messageId: MessageId) {
        viewModelScope.launch {
            val userId = primaryUserId.first()

            val labelAsData =
                mutableDetailState.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                    ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

            val operation =
                onMessageLabelAsConfirmed(
                    userId = userId,
                    messageId = messageId,
                    labelUiModelsWithSelectedState = labelAsData.labelUiModelsWithSelectedState,
                    archiveSelected = archiveSelected
                ).fold(
                    ifLeft = {
                        Timber.e("Relabel message failed: $it")
                        ConversationDetailEvent.ErrorLabelingConversation
                    },
                    ifRight = { LabelAsConfirmed(archiveSelected, LabelAsBottomSheetEntryPoint.Message(messageId)) }
                )
            emitNewStateFrom(operation)
        }
    }

    private fun onConversationLabelAsConfirmed(archiveSelected: Boolean) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val labels = observeCustomMailLabels(userId).first().onLeft {
                Timber.e("Error while observing custom labels when relabeling got confirmed: $it")
            }.getOrElse { emptyList() }
            val messagesWithLabels = observeConversationMessages(userId, conversationId).first().onLeft {
                Timber.e("Error while observing conversation message when relabeling got confirmed: $it")
            }.getOrElse { emptyList() }

            val previousSelection = labels.getLabelSelectionState(messagesWithLabels)

            val labelAsData = mutableDetailState.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

            val updatedSelections = labelAsData.getLabelSelectionState()

            val relabelAction = suspend {
                relabelConversation(
                    userId = userId,
                    conversationId = conversationId,
                    currentSelections = previousSelection,
                    updatedSelections = updatedSelections
                )
            }

            if (archiveSelected) {
                moveConversation(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Archive.labelId
                ).onLeft {
                    Timber.e("Error while archiving conversation when relabeling got confirmed: $it")
                    return@launch emitNewStateFrom(ConversationDetailEvent.ErrorLabelingConversation)
                }

                performSafeExitAction(
                    onLeft = ConversationDetailEvent.ErrorLabelingConversation,
                    onRight = LabelAsConfirmed(true, LabelAsBottomSheetEntryPoint.Conversation)
                ) {
                    relabelAction()
                }
            } else {
                val operation = relabelAction().fold(
                    ifLeft = { ConversationDetailEvent.ErrorLabelingConversation },
                    ifRight = { LabelAsConfirmed(false, LabelAsBottomSheetEntryPoint.Conversation) }
                )
                emitNewStateFrom(operation)
            }
        }
    }

    private fun archiveConversation() {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            performSafeExitAction(
                onLeft = ConversationDetailEvent.ErrorLabelingConversation,
                onRight = LabelAsConfirmed(true, LabelAsBottomSheetEntryPoint.Conversation)
            ) {
                moveConversation(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Archive.labelId
                ).onLeft {
                    Timber.e("Error while archiving conversation: $it")
                }
            }
        }
    }

    private fun List<MailLabel.Custom>.getLabelSelectionState(messages: List<MessageWithLabels>): LabelSelectionList {
        val previousSelectedLabels = mutableListOf<LabelId>()
        val previousPartiallySelectedLabels = mutableListOf<LabelId>()
        this.forEach { label ->
            if (messages.allContainsLabel(label.id.labelId)) {
                previousSelectedLabels.add(label.id.labelId)
            } else if (messages.partiallyContainsLabel(label.id.labelId)) {
                previousPartiallySelectedLabels.add(label.id.labelId)
            }
        }
        return LabelSelectionList(
            selectedLabels = previousSelectedLabels,
            partiallySelectionLabels = previousPartiallySelectedLabels
        )
    }

    private fun LabelAsBottomSheetState.Data.getLabelSelectionState(): LabelSelectionList {
        val selectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.Selected }
            .map { it.labelUiModel.id.labelId }

        val partiallySelectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.PartiallySelected }
            .map { it.labelUiModel.id.labelId }
        return LabelSelectionList(
            selectedLabels = selectedLabels,
            partiallySelectionLabels = partiallySelectedLabels
        )
    }

    private fun List<MessageWithLabels>.allContainsLabel(labelId: LabelId): Boolean {
        return this.all { messageWithLabel ->
            messageWithLabel.labels.any { it.labelId == labelId }
        }
    }

    private fun List<MessageWithLabels>.partiallyContainsLabel(labelId: LabelId): Boolean {
        return this.any { messageWithLabel ->
            messageWithLabel.labels.any { it.labelId == labelId }
        }
    }

    private fun showConversationMoreActionsBottomSheet() {
        val lastMessage = retrieveLastMessageId() ?: return
        showMoreActionsBottomSheetAndLoadData(
            ConversationDetailViewAction.RequestMoreActionsBottomSheet(MessageId(lastMessage.messageId.id)),
            affectingConversation = true
        )
    }

    private fun showMoreActionsBottomSheetAndLoadData(
        initialEvent: ConversationDetailViewAction.RequestMoreActionsBottomSheet,
        affectingConversation: Boolean = false
    ) {
        viewModelScope.launch {
            emitNewStateFrom(initialEvent)

            val userId = primaryUserId.first()
            val contacts = observeContacts(userId).first().getOrNull()
            val conversation = observeConversation(userId, conversationId, false).first().getOrNull()
            val message = observeMessage(userId, initialEvent.messageId).first().getOrElse {
                Timber.e("Unable to fetch message data.")
                emitNewStateFrom(DismissBottomSheet)
                return@launch
            }

            val sender = contacts?.let {
                return@let resolveParticipantName(message.sender, it)
            }?.name ?: message.sender.name

            val actions = getBottomSheetActions(
                conversation = conversation,
                message = message,
                affectingConversation = affectingConversation
            )

            val event = ConversationDetailEvent.ConversationBottomSheetEvent(
                DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded(
                    affectingConversation = affectingConversation,
                    messageSender = sender,
                    messageSubject = message.subject,
                    messageId = message.messageId.id,
                    participantsCount = message.allRecipientsDeduplicated.size,
                    actions = actions
                )
            )

            emitNewStateFrom(event)
        }
    }

    private fun requireConversationId(): ConversationId {
        val conversationId = savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey)
            ?: throw IllegalStateException("No Conversation id given")
        return ConversationId(conversationId)
    }

    private fun getInitialScrollToMessageId(): MessageIdUiModel? {
        val messageIdStr = savedStateHandle.get<String>(ConversationDetailScreen.ScrollToMessageIdKey)
        return messageIdStr?.let { if (it == "null") null else MessageIdUiModel(it) }
    }

    private fun getFilterByLocation(): LabelId? {
        val labelId = savedStateHandle.get<String>(ConversationDetailScreen.FilterByLocationKey)
        return labelId?.let { if (it == "null") null else LabelId(it) }
    }

    private fun emitNewStateFrom(event: ConversationDetailOperation) {
        val newState = reducer.newStateFrom(state.value, event)
        mutableDetailState.update { newState }
    }

    private fun markAsUnread() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = ConversationDetailEvent.ErrorMarkingAsUnread,
                onRight = MarkUnread
            ) { userId ->
                markConversationAsUnread(userId, conversationId)
            }
        }
    }

    private fun moveConversationToTrash() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = ConversationDetailEvent.ErrorMovingToTrash,
                onRight = Trash
            ) { userId ->
                moveConversation(userId, conversationId, SystemLabelId.Trash.labelId)
            }
        }
    }

    private fun starConversation() {
        primaryUserId.mapLatest { userId ->
            starConversations(userId, listOf(conversationId)).fold(
                ifLeft = { ConversationDetailEvent.ErrorAddStar },
                ifRight = { Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarConversation() {
        primaryUserId.mapLatest { userId ->
            unStarConversations(userId, listOf(conversationId)).fold(
                ifLeft = { ConversationDetailEvent.ErrorRemoveStar },
                ifRight = { UnStar }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun handleDeleteConfirmed(action: ConversationDetailViewAction) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val conversation = observeConversation(userId, conversationId, false).first().getOrNull()
            if (conversation == null) {
                Timber.e("Failed to get conversation for deletion")
                emitNewStateFrom(ConversationDetailEvent.ErrorDeletingConversation)
                return@launch
            }

            val currentDeletableLabel = conversation.labels.firstOrNull { isDeletable(it) }

            if (currentDeletableLabel == null) {
                Timber.e("Failed to delete conversation: no applicable folder")
                emitNewStateFrom(ConversationDetailEvent.ErrorDeletingNoApplicableFolder)
                return@launch
            } else {
                // We manually cancel the observations since the following deletion calls cause all the observers
                // to emit, which could lead to race conditions as the observers re-insert the conversation and/or
                // the messages in the DB on late changes, making the entry still re-appear in the mailbox list.
                stopAllJobs()

                deleteConversations(userId, listOf(conversationId), currentDeletableLabel.labelId)
                emitNewStateFrom(action)
            }
        }
    }

    private fun directlyHandleViewAction(action: ConversationDetailViewAction) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun onMoveToDestinationConfirmed(mailLabelText: MailLabelText, entryPoint: MoveToBottomSheetEntryPoint) {
        when (entryPoint) {
            is MoveToBottomSheetEntryPoint.Conversation -> onConversationMoveToDestinationConfirmed(
                mailLabelText, entryPoint
            )

            is MoveToBottomSheetEntryPoint.Message -> onMessageMoveToDestinationConfirmed(
                mailLabelText,
                entryPoint.messageId
            )

            else -> throw IllegalStateException("Invalid entry point for move to destination confirmed")
        }
    }

    private fun onConversationMoveToDestinationConfirmed(
        mailLabelText: MailLabelText,
        entryPoint: MoveToBottomSheetEntryPoint.Conversation
    ) {
        viewModelScope.launch {
            when (val state = state.value.bottomSheetState?.contentState) {
                is MoveToBottomSheetState.Data -> {
                    state.selected?.let { mailLabelUiModel ->
                        performSafeExitAction(
                            onLeft = ConversationDetailEvent.ErrorMovingConversation,
                            onRight = MoveToDestinationConfirmed(mailLabelText, entryPoint)
                        ) { userId ->
                            moveConversation(userId, conversationId, mailLabelUiModel.id.labelId)
                        }
                    } ?: emitNewStateFrom(ConversationDetailEvent.ErrorMovingConversation)
                }

                // Unsupported flow
                else -> emitNewStateFrom(ConversationDetailEvent.ErrorMovingConversation)
            }
        }
    }

    private fun onMessageMoveToDestinationConfirmed(mailLabelText: MailLabelText, messageId: MessageId) {
        val bottomSheetState = state.value.bottomSheetState?.contentState
        if (bottomSheetState is MoveToBottomSheetState.Data) {
            bottomSheetState.selected?.let { mailLabelUiModel ->
                handleMoveMessage(
                    ConversationDetailViewAction.MoveMessage.MoveTo(
                        messageId,
                        mailLabelUiModel.id.labelId,
                        mailLabelText
                    )
                )
            } ?: throw IllegalStateException("No destination selected")
        } else {
            emitNewStateFrom(ConversationDetailEvent.ErrorMovingMessage)
        }
    }

    private fun onExpandMessage(
        messageId: MessageIdUiModel,
        effect: InMemoryConversationStateRepository.PostExpandEffect?
    ) {
        viewModelScope.launch(ioDispatcher) {
            val domainMsgId = MessageId(messageId.id)
            setMessageViewState.expanding(domainMsgId)
            getDecryptedMessageBody(primaryUserId.first(), domainMsgId)
                .onRight { message ->
                    setMessageViewState.expanded(domainMsgId, message, effect)

                    if (message.attachments.isNotEmpty()) {
                        updateObservedAttachments(mapOf(domainMsgId to message.attachments))
                    }

                    markMessageAndConversationReadIfAllMessagesRead(
                        primaryUserId.first(),
                        domainMsgId,
                        conversationId
                    )
                }
                .onLeft {
                    emitMessageBodyDecryptError(it, messageId)
                    setMessageViewState.collapsed(domainMsgId)
                }
                .getOrNull()
        }
    }

    private fun onCollapseMessage(messageId: MessageIdUiModel) {
        viewModelScope.launch {
            setMessageViewState.collapsed(MessageId(messageId.id))
            removeObservedAttachments(MessageId(messageId.id))
        }
    }

    private fun handleChangeVisibilityOfMessages() {
        viewModelScope.launch {
            setMessageViewState.switchTrashedMessagesFilter()
        }
    }

    private fun replyToLastMessage(action: ConversationDetailViewAction.ReplyToLastMessage) {
        val lastMessage = retrieveLastMessageId() ?: return
        when (lastMessage) {
            is ConversationDetailMessageUiModel.Expanded -> {
                if (action.replyToAll) {
                    emitNewStateFrom(ConversationDetailEvent.ReplyAllToMessageRequested(lastMessage.messageId))
                } else {
                    emitNewStateFrom(ConversationDetailEvent.ReplyToMessageRequested(lastMessage.messageId))
                }
            }
            else -> {
                emitNewStateFrom(DismissBottomSheet)
                onExpandMessage(
                    lastMessage.messageId,
                    effect = if (action.replyToAll) {
                        InMemoryConversationStateRepository.PostExpandEffect.ReplyAllRequested
                    } else {
                        InMemoryConversationStateRepository.PostExpandEffect.ReplyRequested
                    }
                )
            }
        }
    }

    private fun forwardLastMessage() {
        val lastMessage = retrieveLastMessageId() ?: return
        when (lastMessage) {
            is ConversationDetailMessageUiModel.Expanded -> {
                emitNewStateFrom(ConversationDetailEvent.ForwardMessageRequested(lastMessage.messageId))
            }
            else -> {
                emitNewStateFrom(DismissBottomSheet)
                onExpandMessage(
                    lastMessage.messageId,
                    effect = InMemoryConversationStateRepository.PostExpandEffect.ForwardRequested
                )
            }
        }
    }

    private fun reportPhishingLastMessage() {
        val lastMessageId = retrieveLastMessageId() ?: return
        handleReportPhishing(ConversationDetailViewAction.ReportPhishing(MessageId(lastMessageId.messageId.id)))
    }

    private fun moveToSpam() {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            performSafeExitAction(
                onLeft = ConversationDetailEvent.ErrorMovingConversation,
                onRight = ConversationDetailEvent.MovedToSpam
            ) {
                moveConversation(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Spam.labelId
                ).onLeft {
                    Timber.e("Error while moving conversation to spam: $it")
                }
            }
        }
    }

    private fun printLastMessage(context: Context) {
        val lastMessage = retrieveLastMessageId() ?: return
        handlePrint(context, MessageId(lastMessage.messageId.id))
    }

    private fun retrieveLastMessageId(): ConversationDetailMessageUiModel? {
        val dataState = state.value.messagesState as? ConversationDetailsMessagesState.Data
        if (dataState == null) {
            Timber.e("Messages state is not data to perform this operation")
            return null
        }
        return dataState.messages.lastOrNull {
            when (it) {
                is ConversationDetailMessageUiModel.Collapsed -> it.isDraft.not()
                is ConversationDetailMessageUiModel.Expanded -> true
                is ConversationDetailMessageUiModel.Expanding -> it.collapsed.isDraft.not()
                is ConversationDetailMessageUiModel.Hidden -> false
            }
        }
    }

    private fun onDoNotAskLinkConfirmationChecked() {
        viewModelScope.launch { updateLinkConfirmationSetting(false) }
    }

    private fun emitMessageBodyDecryptError(error: GetDecryptedMessageBodyError, messageId: MessageIdUiModel) {
        val errorState = when (error) {
            is GetDecryptedMessageBodyError.Data -> if (error.dataError.isOfflineError()) {
                ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline(messageId)
            } else {
                ConversationDetailEvent.ErrorExpandingRetrieveMessageError(messageId)
            }

            is GetDecryptedMessageBodyError.Decryption ->
                ConversationDetailEvent.ErrorExpandingDecryptMessageError(messageId)
        }

        emitNewStateFrom(errorState)
    }

    private fun showAllAttachmentsForMessage(messageId: MessageIdUiModel) {
        val dataState = state.value.messagesState as? ConversationDetailsMessagesState.Data
        if (dataState == null) {
            Timber.e("Messages state is not data to perform show all attachments operation")
            return
        }
        dataState.messages.firstOrNull { it.messageId == messageId }
            ?.takeIf { it is ConversationDetailMessageUiModel.Expanded }
            ?.let { it as ConversationDetailMessageUiModel.Expanded }
            ?.let {
                val attachmentGroupUiModel = it.messageBodyUiModel.attachments
                val operation = ConversationDetailEvent.ShowAllAttachmentsForMessage(
                    messageId = messageId,
                    conversationDetailMessageUiModel = it.copy(
                        messageBodyUiModel = it.messageBodyUiModel.copy(
                            attachments = attachmentGroupUiModel?.copy(
                                limit = attachmentGroupUiModel.attachments.size
                            )
                        )
                    )
                )
                viewModelScope.launch { emitNewStateFrom(operation) }
            }
    }

    private fun observeAttachments() = primaryUserId.flatMapLatest { userId ->
        attachmentsState.flatMapLatest { attachmentsMap ->
            flow {
                attachmentsMap.forEach { (messageId, attachments) ->
                    attachments.forEach { attachment ->
                        emit(messageId to attachment.attachmentId)
                    }
                }
            }
                .flatMapMerge { (messageId, attachmentId) ->
                    observeMessageAttachmentStatus(userId, messageId, attachmentId)
                        .mapLatest {
                            ConversationDetailEvent.AttachmentStatusChanged(
                                MessageIdUiModel(messageId.id),
                                attachmentId,
                                it.status
                            )
                        }
                        .distinctUntilChanged()
                }
        }
    }
        .onEach { event ->
            emitNewStateFrom(event)
        }
        .launchIn(viewModelScope)

    private fun observeCustomizeToolbarSpotlights() = observeCustomizeToolbarSpotlight()
        .onEach {
            emitNewStateFrom(ConversationDetailEvent.RequestCustomizeToolbarSpotlight)
        }
        .launchIn(viewModelScope)

    private fun updateSpotlightLastSeenTimestamp() = viewModelScope.launch {
        updateCustomizeToolbarSpotlight()
    }

    private fun onOpenAttachmentClicked(messageId: MessageIdUiModel, attachmentId: AttachmentId) {
        val domainMsgId = MessageId(messageId.id)
        viewModelScope.launch {
            if (isAttachmentDownloadInProgress().not()) {
                val userId = primaryUserId.first()
                getAttachmentIntentValues(userId, domainMsgId, attachmentId).fold(
                    ifLeft = {
                        Timber.d("Failed to download attachment: $it")
                        val event = when (it) {
                            is DataError.Local.OutOfMemory ->
                                ConversationDetailEvent.ErrorGettingAttachmentNotEnoughSpace

                            else -> ConversationDetailEvent.ErrorGettingAttachment
                        }
                        emitNewStateFrom(event)
                    },
                    ifRight = { emitNewStateFrom(ConversationDetailEvent.OpenAttachmentEvent(it)) }
                )
            } else {
                emitNewStateFrom(ConversationDetailEvent.ErrorAttachmentDownloadInProgress)
            }
        }
    }

    private suspend fun isAttachmentDownloadInProgress(): Boolean {
        val userId = primaryUserId.first()
        val messagesState = mutableDetailState.value.messagesState
        return if (messagesState is ConversationDetailsMessagesState.Data) {
            getDownloadingAttachmentsForMessages(
                userId,
                messagesState.messages.map { MessageId(it.messageId.id) }
            ).isNotEmpty()
        } else false
    }

    private fun updateObservedAttachments(attachments: Map<MessageId, List<MessageAttachment>>) {
        attachmentsState.update { it + attachments }
    }

    private fun removeObservedAttachments(messageId: MessageId) {
        attachmentsState.update { it - MessageId(messageId.id) }
    }

    private fun handleReportPhishingConfirmed(action: ConversationDetailViewAction.ReportPhishingConfirmed) {
        viewModelScope.launch {
            reportPhishingMessage(primaryUserId.first(), action.messageId)
                .onLeft { Timber.e("Error while reporting phishing message: $it") }
            emitNewStateFrom(action)
        }
    }

    private fun handleReportPhishing(action: ConversationDetailViewAction.ReportPhishing) {
        viewModelScope.launch {
            val operation = when (networkManager.networkStatus) {
                NetworkStatus.Disconnected -> ConversationDetailEvent.ReportPhishingRequested(
                    messageId = action.messageId,
                    isOffline = true
                )

                else -> ConversationDetailEvent.ReportPhishingRequested(messageId = action.messageId, isOffline = false)
            }
            emitNewStateFrom(operation)
        }
    }

    private fun consumeEffect(event: ConversationDetailViewAction.EffectConsumed) = viewModelScope.launch {
        setMessageViewState.effectConsumed(event.messageId)
        val messageId = MessageIdUiModel(event.messageId.id)
        val operation = when (event.effect) {
            MessageBody.DoOnDisplayedEffect.Forward -> ConversationDetailEvent.ForwardMessageRequested(messageId)
            MessageBody.DoOnDisplayedEffect.Reply -> ConversationDetailEvent.ReplyToMessageRequested(messageId)
            MessageBody.DoOnDisplayedEffect.ReplyAll -> ConversationDetailEvent.ReplyAllToMessageRequested(messageId)
        }
        emitNewStateFrom(operation)
    }

    private fun handleOpenInProtonCalendar(action: ConversationDetailViewAction.OpenInProtonCalendar) {
        viewModelScope.launch {
            val isProtonCalendarInstalled = isProtonCalendarInstalled()
            if (isProtonCalendarInstalled) {
                val dataState = mutableDetailState.value.messagesState as? ConversationDetailsMessagesState.Data
                dataState?.messages?.mapNotNull { it as? ConversationDetailMessageUiModel.Expanded }
                    ?.first { it.messageId.id == action.messageId.id }
                    ?.let { messageUiModel -> handleOpenInProtonCalendar(messageUiModel) }
            } else {
                val intent = OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore
                emitNewStateFrom(ConversationDetailEvent.HandleOpenProtonCalendarRequest(intent))
            }
        }
    }

    private suspend fun handleOpenInProtonCalendar(messageUiModel: ConversationDetailMessageUiModel.Expanded) {
        val sender = messageUiModel.messageDetailHeaderUiModel.sender.participantAddress
        val recipient = messageUiModel.userAddress.email
        val firstCalendarAttachment = messageUiModel.messageBodyUiModel
            .attachments
            ?.attachments
            ?.firstOrNull { uiModel -> uiModel.mimeType.split(";").any { it == "text/calendar" } }

        if (firstCalendarAttachment == null) return

        getAttachmentIntentValues(
            userId = primaryUserId.first(),
            messageId = MessageId(messageUiModel.messageId.id),
            attachmentId = AttachmentId(firstCalendarAttachment.attachmentId)
        ).fold(
            ifLeft = { Timber.d("Failed to download attachment: $it") },
            ifRight = {
                val intent = OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar(it.uri, sender, recipient)
                emitNewStateFrom(ConversationDetailEvent.HandleOpenProtonCalendarRequest(intent))
            }
        )
    }

    private fun handlePrint(context: Context, messageId: MessageId) {
        val conversationState = state.value.conversationState
        val messagesState = state.value.messagesState
        if (
            conversationState is ConversationDetailMetadataState.Data &&
            messagesState is ConversationDetailsMessagesState.Data
        ) {
            messagesState.messages.find { it.messageId.id == messageId.id }?.let {
                if (it is ConversationDetailMessageUiModel.Expanded) {
                    printMessage(
                        context = context,
                        subject = conversationState.conversationUiModel.subject,
                        messageHeaderUiModel = it.messageDetailHeaderUiModel,
                        messageBodyUiModel = it.messageBodyUiModel,
                        messageBodyExpandCollapseMode = it.expandCollapseMode,
                        loadEmbeddedImage = this@ConversationDetailViewModel::loadEmbeddedImage
                    )
                } else {
                    emitNewStateFrom(DismissBottomSheet)
                    onExpandMessage(
                        MessageIdUiModel(messageId.id),
                        effect = InMemoryConversationStateRepository.PostExpandEffect.PrintRequested
                    )
                }
            }
        }
    }

    private fun handleMarkMessageUnread(action: ConversationDetailViewAction.MarkMessageUnread) {
        viewModelScope.launch {
            markMessageAsUnread(primaryUserId.first(), action.messageId)
            onCollapseMessage(MessageIdUiModel(action.messageId.id))
            emitNewStateFrom(action)
        }
    }

    @Suppress("LongMethod")
    private fun handleMoveMessage(action: ConversationDetailViewAction.MoveMessage) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val conversationWithMessagesAndLabels = observeConversationMessages(userId, conversationId).first()
            val mailFolders = observeMailLabels(userId).first().allById.mapKeys { it.key.labelId }
                .filter {
                    it.key !in listOf(
                        SystemLabelId.AllMail.labelId,
                        SystemLabelId.AllSent.labelId,
                        SystemLabelId.AllDrafts.labelId,
                        SystemLabelId.AlmostAllMail.labelId,
                        SystemLabelId.Starred.labelId
                    ) &&
                        it.value.id !is MailLabelId.Custom.Label
                }

            val conversationMessages = conversationWithMessagesAndLabels
                .getOrElse {
                    return@launch emitNewStateFrom(ConversationDetailEvent.ErrorMovingMessage)
                }
                .map { it.message }
                .associateWith { it.labelIds.firstOrNull { labelId -> labelId in mailFolders } }

            val currentLabel = conversationMessages
                .filter { it.key.id == action.messageId.id }
                .values
                .firstOrNull()

            val messagesInCurrentLocation = conversationMessages.filter { it.value == currentLabel }

            when {
                // If it's just one message, we close the screen and return to Mailbox.
                messagesInCurrentLocation.size == 1 -> {
                    performSafeExitAction(
                        onLeft = ConversationDetailEvent.ErrorMovingMessage,
                        onRight = ConversationDetailEvent.LastMessageMoved(action.mailLabelText)
                    ) {
                        moveRemoteMessageAndLocalConversation(
                            userId,
                            action.messageId,
                            conversationId,
                            conversationLabelingOptions = ConversationLabelingOptions(
                                removeCurrentLabel = true,
                                fromLabel = currentLabel,
                                toLabel = action.labelId
                            )
                        )
                    }
                }

                else -> {
                    moveRemoteMessageAndLocalConversation(
                        userId,
                        action.messageId,
                        conversationId,
                        conversationLabelingOptions = ConversationLabelingOptions(
                            removeCurrentLabel = false,
                            fromLabel = currentLabel,
                            toLabel = action.labelId
                        )
                    )

                    emitNewStateFrom(MessageMoved(action.mailLabelText))
                }
            }
        }
    }

    /**
     * A helper function that allows to perform actions that eventually cause the user to leave the screen, while
     * still making sure that observers are not being triggered during the execution of the action.
     *
     * At start, all observer jobs are stopped. If the [action] completes with success, they are not resumed as the
     * [onRight] emitted operation is expected to cause a screen exit.
     *
     * In case the [action] fails, other than emitting [onLeft], the observation jobs will be restarted, as the user
     * will still be in the Conversation Details screen.
     */
    private suspend fun performSafeExitAction(
        onLeft: ConversationDetailOperation,
        onRight: ConversationDetailOperation,
        action: suspend (userId: UserId) -> Either<*, *>
    ) {
        stopAllJobs()

        val userId = primaryUserId.first()
        val event = action(userId).fold(
            ifLeft = {
                restartAllJobs()
                onLeft
            },
            ifRight = { onRight }
        )

        emitNewStateFrom(event)
    }

    companion object {

        val initialState = ConversationDetailState.Loading
    }
}

private fun isDeletable(it: ConversationLabel) =
    it.labelId == SystemLabelId.Trash.labelId || it.labelId == SystemLabelId.Spam.labelId

/**
 * Filters [DataError.Local] from messages flow, as we don't want to show them to the user, because the fetch is being
 *  done on the conversation flow.
 */
private fun Flow<Either<DataError, NonEmptyList<MessageWithLabels>>>.ignoreLocalErrors():
    Flow<Either<DataError, NonEmptyList<MessageWithLabels>>> =
    filter { either ->
        either.fold(
            ifLeft = { error -> error !is DataError.Local },
            ifRight = { true }
        )
    }
