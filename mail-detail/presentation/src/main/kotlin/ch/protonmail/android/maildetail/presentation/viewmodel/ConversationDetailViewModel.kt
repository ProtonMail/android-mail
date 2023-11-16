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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.model.LabelSelectionList
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAndConversationReadIfAllMessagesRead
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.RelabelConversation
import ch.protonmail.android.maildetail.domain.usecase.SetMessageViewState
import ch.protonmail.android.maildetail.domain.usecase.StarConversation
import ch.protonmail.android.maildetail.domain.usecase.UnStarConversation
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.CollapseMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.DismissBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ExpandMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.LabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.LabelAsToggleAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MarkUnread
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MessageBodyLinkClicked
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToDestinationConfirmed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToDestinationSelected
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestMoveToBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestScrollTo
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ScrollRequestCompleted
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ShowAllAttachmentsForMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.Star
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.Trash
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.UnStar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
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
    private val relabelConversation: RelabelConversation,
    private val observeContacts: ObserveContacts,
    private val observeConversation: ObserveConversation,
    private val observeConversationMessages: ObserveConversationMessagesWithLabels,
    private val observeDetailActions: ObserveConversationDetailActions,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val observeMessageAttachmentStatus: ObserveMessageAttachmentStatus,
    private val getDownloadingAttachmentsForMessages: GetDownloadingAttachmentsForMessages,
    private val reducer: ConversationDetailReducer,
    private val starConversation: StarConversation,
    private val unStarConversation: UnStarConversation,
    private val savedStateHandle: SavedStateHandle,
    private val getDecryptedMessageBody: GetDecryptedMessageBody,
    private val markMessageAndConversationReadIfAllMessagesRead: MarkMessageAndConversationReadIfAllMessagesRead,
    private val setMessageViewState: SetMessageViewState,
    private val observeConversationViewState: ObserveConversationViewState,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val getEmbeddedImageAvoidDuplicatedExecution: GetEmbeddedImageAvoidDuplicatedExecution,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val observeMailFeature: ObserveMailFeature
) : ViewModel() {

    private val primaryUserId: Flow<UserId> = observePrimaryUserId().filterNotNull()
    private val mutableDetailState = MutableStateFlow(initialState)
    private val conversationId = requireConversationId()
    private val observedAttachments = mutableListOf<AttachmentId>()

    val state: StateFlow<ConversationDetailState> = mutableDetailState.asStateFlow()

    init {
        Timber.d("Open detail screen for conversation ID: $conversationId")
        observeConversationMetadata(conversationId)
        observeConversationMessages(conversationId)
        observeBottomBarActions(conversationId)
        observeReplyActionsFeatureFlag()
    }

    @Suppress("ComplexMethod")
    fun submit(action: ConversationDetailViewAction) {
        when (action) {
            is Star -> starConversation()
            is UnStar -> unStarConversation()
            is MarkUnread -> markAsUnread()
            is Trash -> moveConversationToTrash()
            is DismissBottomSheet -> dismissBottomSheet(action)
            is RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(action)
            is MoveToDestinationSelected -> moveToDestinationSelected(action.mailLabelId)
            is MoveToDestinationConfirmed -> onBottomSheetDestinationConfirmed(action.mailLabelText)
            is RequestLabelAsBottomSheet -> showLabelAsBottomSheetAndLoadData(action)
            is LabelAsToggleAction -> onLabelToggled(action.labelId)
            is LabelAsConfirmed -> onLabelAsConfirmed(action.archiveSelected)
            is ExpandMessage -> onExpandMessage(action.messageId)
            is CollapseMessage -> onCollapseMessage(action.messageId)
            is MessageBodyLinkClicked -> onMessageBodyLinkClicked(action)
            is RequestScrollTo -> onRequestScrollTo(action)
            is ScrollRequestCompleted -> onScrollRequestCompleted(action)
            is ShowAllAttachmentsForMessage -> showAllAttachmentsForMessage(action.messageId)
            is ConversationDetailViewAction.OnAttachmentClicked -> {
                onOpenAttachmentClicked(action.messageId, action.attachmentId)
            }
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

    private fun observeReplyActionsFeatureFlag() {
        primaryUserId.flatMapLatest {
            observeMailFeature(it, MailFeatureId.MessageActions).onEach { feature ->
                mutableDetailState.emit(mutableDetailState.value.copy(showReplyActionsFeatureFlag = feature.value))
            }
        }.launchIn(viewModelScope)
    }

    private fun observeConversationMetadata(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
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
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeConversationMessages(conversationId: ConversationId) {
        primaryUserId
            .flatMapLatest { userId ->
                combine(
                    observeContacts(userId),
                    observeConversationMessages(userId, conversationId).ignoreLocalErrors(),
                    observeFolderColor(userId),
                    observeConversationViewState()
                ) { contactsEither, messagesEither, folderColorSettings, conversationViewState ->
                    val contacts = contactsEither.getOrElse {
                        Timber.i("Failed getting contacts for displaying initials. Fallback to display name")
                        emptyList()
                    }
                    val messages = messagesEither.getOrElse {
                        return@combine ConversationDetailEvent.ErrorLoadingMessages
                    }
                    val messagesUiModels = buildMessagesUiModels(
                        messages = messages,
                        contacts = contacts,
                        folderColorSettings = folderColorSettings,
                        currentViewState = conversationViewState
                    ).toImmutableList()

                    val firstNonDraftMessageId = getFirstNonDraftMessageId(messages)
                    if (stateIsLoading() && firstNonDraftMessageId != null && allCollapsed(conversationViewState)) {
                        ConversationDetailEvent.MessagesData(messagesUiModels, firstNonDraftMessageId)
                    } else {
                        val requestScrollTo = requestScrollToMessageId(conversationViewState)
                        ConversationDetailEvent.MessagesData(messagesUiModels, requestScrollTo)
                    }
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { event ->
                emitNewStateFrom(event)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    private fun stateIsLoading(): Boolean = state.value.messagesState == ConversationDetailsMessagesState.Loading

    private fun allCollapsed(viewState: Map<MessageId, InMemoryConversationStateRepository.MessageState>): Boolean =
        viewState.values.all { it == InMemoryConversationStateRepository.MessageState.Collapsed }

    private fun getFirstNonDraftMessageId(messages: List<MessageWithLabels>): MessageIdUiModel? {
        val messageId = messages
            .filterNot { it.message.isDraft() }
            .sortedByDescending { it.message.order }
            .maxByOrNull { it.message.time }
            ?.message
            ?.messageId

        return messageId?.let { messageIdUiModelMapper.toUiModel(it) }
    }

    private suspend fun buildMessagesUiModels(
        messages: NonEmptyList<MessageWithLabels>,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings,
        currentViewState: Map<MessageId, InMemoryConversationStateRepository.MessageState>
    ): NonEmptyList<ConversationDetailMessageUiModel> {
        val messagesList = messages.map { messageWithLabels ->
            when (val viewState = currentViewState[messageWithLabels.message.messageId]) {
                is InMemoryConversationStateRepository.MessageState.Expanding ->
                    buildExpandingMessage(buildCollapsedMessage(messageWithLabels, contacts, folderColorSettings))

                is InMemoryConversationStateRepository.MessageState.Expanded -> buildExpandedMessage(
                    messageWithLabels,
                    contacts,
                    viewState.decryptedBody,
                    folderColorSettings
                )

                else -> buildCollapsedMessage(messageWithLabels, contacts, folderColorSettings)
            }
        }
        return messagesList
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
            null
        }
        return requestScrollTo
    }

    private suspend fun buildCollapsedMessage(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        folderColorSettings: FolderColorSettings
    ): ConversationDetailMessageUiModel.Collapsed = conversationMessageMapper.toUiModel(
        messageWithLabels,
        contacts,
        folderColorSettings
    )

    private fun buildExpandingMessage(
        collapsedMessage: ConversationDetailMessageUiModel.Collapsed
    ): ConversationDetailMessageUiModel.Expanding = conversationMessageMapper.toUiModel(
        collapsedMessage
    )

    private suspend fun buildExpandedMessage(
        messageWithLabels: MessageWithLabels,
        contacts: List<Contact>,
        decryptedBody: DecryptedMessageBody,
        folderColorSettings: FolderColorSettings
    ): ConversationDetailMessageUiModel.Expanded = conversationMessageMapper.toUiModel(
        messageWithLabels,
        contacts,
        decryptedBody,
        folderColorSettings
    )

    private fun observeBottomBarActions(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
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
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showMoveToBottomSheetAndLoadData(initialEvent: ConversationDetailViewAction) {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeDestinationMailLabels(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                ConversationDetailEvent.ConversationBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        folders.toUiModels(color).let { it.folders + it.systems }.toImmutableList()
                    )
                )
            }
        }.onStart {
            emitNewStateFrom(initialEvent)
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showLabelAsBottomSheetAndLoadData(initialEvent: ConversationDetailViewAction) {
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
                    partiallySelectedLabels = partiallySelectedLabels.toImmutableList()
                )
            )
            emitNewStateFrom(event)
        }
    }

    private fun onLabelToggled(labelId: LabelId) {
        viewModelScope.launch { emitNewStateFrom(LabelAsToggleAction(labelId)) }
    }

    private fun onLabelAsConfirmed(archiveSelected: Boolean) {
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

            if (archiveSelected) {
                moveConversation(
                    userId = userId,
                    conversationId = conversationId,
                    labelId = SystemLabelId.Archive.labelId
                ).onLeft { Timber.e("Error while archiving conversation when relabeling got confirmed: $it") }
            }
            val operation = relabelConversation(
                userId = userId,
                conversationId = conversationId,
                currentSelections = previousSelection,
                updatedSelections = updatedSelections
            ).fold(
                ifLeft = {
                    Timber.e("Error while relabeling conversation: $it")
                    ConversationDetailEvent.ErrorLabelingConversation
                },
                ifRight = { LabelAsConfirmed(archiveSelected) }
            )
            emitNewStateFrom(operation)
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

    private fun requireConversationId(): ConversationId {
        val conversationId = savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey)
            ?: throw IllegalStateException("No Conversation id given")
        return ConversationId(conversationId)
    }

    private suspend fun emitNewStateFrom(event: ConversationDetailOperation) {
        val newState: ConversationDetailState = reducer.newStateFrom(state.value.copy(), event)
        mutableDetailState.emit(newState)
    }

    private fun markAsUnread() {
        primaryUserId.mapLatest { userId ->
            markConversationAsUnread(userId, conversationId).fold(
                ifLeft = { ConversationDetailEvent.ErrorMarkingAsUnread },
                ifRight = { MarkUnread }
            )
        }.onEach(::emitNewStateFrom)
            .launchIn(viewModelScope)
    }

    private fun moveConversationToTrash() {
        primaryUserId.mapLatest { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Trash.labelId).fold(
                ifLeft = { ConversationDetailEvent.ErrorMovingToTrash },
                ifRight = { Trash }
            )
        }.onEach(::emitNewStateFrom)
            .launchIn(viewModelScope)
    }

    private fun starConversation() {
        primaryUserId.mapLatest { userId ->
            starConversation(userId, conversationId).fold(
                ifLeft = { ConversationDetailEvent.ErrorAddStar },
                ifRight = { Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarConversation() {
        Timber.d("UnStar conversation clicked")
        primaryUserId.mapLatest { userId ->
            unStarConversation(userId, conversationId).fold(
                ifLeft = { ConversationDetailEvent.ErrorRemoveStar },
                ifRight = { UnStar }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun dismissBottomSheet(action: ConversationDetailViewAction) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun moveToDestinationSelected(mailLabelId: MailLabelId) {
        viewModelScope.launch {
            emitNewStateFrom(MoveToDestinationSelected(mailLabelId))
        }
    }

    private fun onBottomSheetDestinationConfirmed(mailLabelText: String) {
        primaryUserId.mapLatest { userId ->
            val bottomSheetState = state.value.bottomSheetState?.contentState
            if (bottomSheetState is MoveToBottomSheetState.Data) {
                bottomSheetState.selected?.let { mailLabelUiModel ->
                    moveConversation(userId, conversationId, mailLabelUiModel.id.labelId).fold(
                        ifLeft = { ConversationDetailEvent.ErrorMovingConversation },
                        ifRight = { MoveToDestinationConfirmed(mailLabelText) }
                    )
                } ?: throw IllegalStateException("No destination selected")
            } else {
                ConversationDetailEvent.ErrorMovingConversation
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun onExpandMessage(messageId: MessageIdUiModel) {
        viewModelScope.launch(ioDispatcher) {
            val domainMsgId = MessageId(messageId.id)
            setMessageViewState.expanding(domainMsgId)
            getDecryptedMessageBody(primaryUserId.first(), domainMsgId)
                .onRight {
                    observeAttachments(messageId, it.attachments)
                    markMessageAndConversationReadIfAllMessagesRead(primaryUserId.first(), domainMsgId, conversationId)
                    setMessageViewState.expanded(domainMsgId, it)
                }
                .onLeft {
                    emitMessageBodyDecryptError(it, messageId)
                    setMessageViewState.collapsed(domainMsgId)
                }
                .getOrNull()
        }
    }

    private fun onCollapseMessage(messageId: MessageIdUiModel) {
        viewModelScope.launch { setMessageViewState.collapsed(MessageId(messageId.id)) }
    }

    private fun onMessageBodyLinkClicked(action: MessageBodyLinkClicked) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun onRequestScrollTo(action: RequestScrollTo) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun onScrollRequestCompleted(action: ScrollRequestCompleted) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private suspend fun emitMessageBodyDecryptError(error: GetDecryptedMessageBodyError, messageId: MessageIdUiModel) {
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

    private suspend fun observeAttachments(messageId: MessageIdUiModel, attachments: List<MessageAttachment>) {
        val domainMsgId = MessageId(messageId.id)
        attachments.map { it.attachmentId }.filterNot { observedAttachments.contains(it) }.forEach { attachmentId ->
            primaryUserId.flatMapLatest { userId ->
                observedAttachments.add(attachmentId)
                observeMessageAttachmentStatus(userId, domainMsgId, attachmentId).mapLatest {
                    ConversationDetailEvent.AttachmentStatusChanged(messageId, attachmentId, it.status)
                }
            }.onEach { event ->
                emitNewStateFrom(event)
            }.launchIn(viewModelScope)
        }
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
            getDownloadingAttachmentsForMessages(userId, messagesState.messages.map { MessageId(it.messageId.id) })
                .isNotEmpty()
        } else false
    }

    companion object {

        val initialState = ConversationDetailState.Loading
    }
}

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
