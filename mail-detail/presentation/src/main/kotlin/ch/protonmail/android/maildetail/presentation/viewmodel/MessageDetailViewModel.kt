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
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDetailBottomSheetActions
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.LoadDataForMessageLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.usecase.OnMessageLabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.usecase.PrintMessage
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.UpdateCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class MessageDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMessage: ObserveMessage,
    private val observeMessageWithLabels: ObserveMessageWithLabels,
    private val getDecryptedMessageBody: GetDecryptedMessageBody,
    private val messageDetailReducer: MessageDetailReducer,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeContacts: ObserveContacts,
    private val observeDetailActions: ObserveMessageDetailActions,
    private val getBottomSheetActions: GetDetailBottomSheetActions,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val observeAutoDeleteSetting: ObserveAutoDeleteSetting,
    private val observeMessageAttachmentStatus: ObserveMessageAttachmentStatus,
    private val markUnread: MarkMessageAsUnread,
    private val markRead: MarkMessageAsRead,
    private val getContacts: GetContacts,
    private val starMessages: StarMessages,
    private val unStarMessages: UnStarMessages,
    private val savedStateHandle: SavedStateHandle,
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper,
    private val moveMessage: MoveMessage,
    private val deleteMessages: DeleteMessages,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val getDownloadingAttachmentsForMessages: GetDownloadingAttachmentsForMessages,
    private val getEmbeddedImageAvoidDuplicatedExecution: GetEmbeddedImageAvoidDuplicatedExecution,
    private val observePrivacySettings: ObservePrivacySettings,
    private val updateLinkConfirmationSetting: UpdateLinkConfirmationSetting,
    private val resolveParticipantName: ResolveParticipantName,
    private val reportPhishingMessage: ReportPhishingMessage,
    private val isProtonCalendarInstalled: IsProtonCalendarInstalled,
    private val networkManager: NetworkManager,
    private val printMessage: PrintMessage,
    private val findContactByEmail: FindContactByEmail,
    private val loadDataForMessageLabelAsBottomSheet: LoadDataForMessageLabelAsBottomSheet,
    private val onMessageLabelAsConfirmed: OnMessageLabelAsConfirmed,
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

    private val messageId = requireMessageId()
    private val mutableDetailState = MutableStateFlow(initialState)
    private val attachmentsState = MutableStateFlow<List<MessageAttachment>>(emptyList())

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    private val jobs = CopyOnWriteArrayList<Job>()

    init {
        Timber.d("Open detail screen for message ID: $messageId")
        setupObservers()
        getMessageBody(messageId)
    }

    private fun setupObservers() {
        jobs.addAll(
            listOf(
                observeMessageWithLabels(messageId),
                observeBottomBarActions(messageId),
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
    fun submit(action: MessageViewAction) {
        when (action) {
            is MessageViewAction.ExpandOrCollapseMessageBody -> expandOrCollapseMessageBody()
            is MessageViewAction.Reload -> reloadMessageBody(messageId)
            is MessageViewAction.Star -> starMessage()
            is MessageViewAction.UnStar -> unStarMessage()
            is MessageViewAction.MarkUnread -> markMessageUnread()
            is MessageViewAction.Trash -> trashMessage()
            is MessageViewAction.LoadRemoteAndEmbeddedContent,
            is MessageViewAction.ShowEmbeddedImages,
            is MessageViewAction.LoadRemoteContent,
            is MessageViewAction.DismissBottomSheet,
            is MessageViewAction.DeleteRequested,
            is MessageViewAction.DeleteDialogDismissed,
            is MessageViewAction.SpotlightDismissed,
            is MessageViewAction.ReportPhishingDismissed -> directlyHandleViewAction(action)

            is MessageViewAction.DeleteConfirmed -> handleDeleteConfirmed(action)
            is MessageViewAction.RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(action)
            is MessageViewAction.MoveToDestinationSelected -> moveToDestinationSelected(action.mailLabelId)
            is MessageViewAction.MoveToDestinationConfirmed -> onBottomSheetDestinationConfirmed(action.mailLabelText)
            is MessageViewAction.RequestLabelAsBottomSheet -> showLabelAsBottomSheetAndLoadData(action)
            is MessageViewAction.RequestContactActionsBottomSheet -> showContactActionsBottomSheetAndLoadData(action)
            is MessageViewAction.LabelAsToggleAction -> onLabelSelected(action.labelId)
            is MessageViewAction.LabelAsConfirmed -> onLabelAsConfirmed(action.archiveSelected)
            is MessageViewAction.MessageBodyLinkClicked -> onMessageBodyLinkClicked(action.uri)
            is MessageViewAction.DoNotAskLinkConfirmationAgain -> onDoNotAskLinkConfirmationChecked()
            is MessageViewAction.ShowAllAttachments -> onShowAllAttachmentsClicked()
            is MessageViewAction.OnAttachmentClicked -> onOpenAttachmentClicked(action.attachmentId)
            is MessageViewAction.RequestMoreActionsBottomSheet -> showMoreActionsBottomSheetAndLoadData(action)
            is MessageViewAction.ReportPhishing -> handleReportPhishing(action)
            is MessageViewAction.ReportPhishingConfirmed -> handleReportPhishingConfirmed(action)
            is MessageViewAction.OpenInProtonCalendar -> handleOpenInProtonCalendar()
            is MessageViewAction.SwitchViewMode -> directlyHandleViewAction(action)
            is MessageViewAction.PrintRequested -> directlyHandleViewAction(action)
            is MessageViewAction.Print -> handlePrint(action.context)
            is MessageViewAction.Archive -> handleArchive()
            is MessageViewAction.Spam -> handleSpam()
            is MessageViewAction.SpotlightDisplayed -> updateSpotlightLastSeenTimestamp()
        }
    }

    fun loadEmbeddedImage(messageId: MessageId, contentId: String): GetEmbeddedImageResult? {
        return runBlocking {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId = primaryUserId.first(),
                messageId = messageId,
                contentId = contentId,
                coroutineContext = viewModelScope.coroutineContext
            )
        }
    }

    private fun starMessage() {
        primaryUserId.mapLatest { userId ->
            starMessages(userId, listOf(messageId)).fold(
                ifLeft = { MessageDetailEvent.ErrorAddingStar },
                ifRight = { MessageViewAction.Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarMessage() {
        primaryUserId.mapLatest { userId ->
            unStarMessages(userId, listOf(messageId)).fold(
                ifLeft = { MessageDetailEvent.ErrorRemovingStar },
                ifRight = { MessageViewAction.UnStar }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun markMessageUnread() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = MessageDetailEvent.ErrorMarkingUnread,
                onRight = MessageViewAction.MarkUnread
            ) { userId ->
                markUnread(userId, messageId)
            }
        }
    }

    private fun markMessageRead() {
        primaryUserId.mapLatest { userId ->
            markRead(userId, messageId).getOrNull()
        }.launchIn(viewModelScope)
    }

    private fun trashMessage() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = MessageDetailEvent.ErrorMovingToTrash,
                onRight = MessageViewAction.Trash
            ) { userId ->
                moveMessage(userId, messageId, SystemLabelId.Trash.labelId)
            }
        }
    }

    private fun directlyHandleViewAction(action: MessageViewAction) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun handleDeleteConfirmed(action: MessageViewAction) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val messageWithLabels = observeMessageWithLabels(userId, messageId).first().getOrNull()
            if (messageWithLabels == null) {
                Timber.d("Failed to get message with labels for deleting message")
                emitNewStateFrom(MessageDetailEvent.ErrorDeletingMessage)
                return@launch
            }

            val exclusiveLabels = observeDestinationMailLabels(userId).firstOrNull()
            if (exclusiveLabels == null) {
                Timber.d("Failed to get exclusive labels for deleting message")
                emitNewStateFrom(MessageDetailEvent.ErrorDeletingMessage)
                return@launch
            }

            val currentExclusiveLabel = messageWithLabels.message.labelIds
                .firstOrNull { labelId -> labelId in exclusiveLabels.systemLabels.map { it.id.labelId } }

            if (currentExclusiveLabel == null) {
                Timber.d("Applicable exclusive label not found")
                emitNewStateFrom(MessageDetailEvent.ErrorDeletingNoApplicableFolder)
            } else {
                // Cancel all active observations to prevent inconsistencies upon exiting the screen.
                stopAllJobs()

                deleteMessages(userId, listOf(messageId), currentExclusiveLabel)
                emitNewStateFrom(action)
            }
        }
    }

    private fun moveToDestinationSelected(mailLabelId: MailLabelId) {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.MoveToDestinationSelected(mailLabelId))
        }
    }

    private fun onBottomSheetDestinationConfirmed(mailLabelText: MailLabelText) {
        viewModelScope.launch {
            when (val state = state.value.bottomSheetState?.contentState) {
                is MoveToBottomSheetState.Data -> {
                    state.moveToDestinations.firstOrNull { it.isSelected }?.let {
                        performSafeExitAction(
                            onLeft = MessageDetailEvent.ErrorMovingMessage,
                            onRight = MessageViewAction.MoveToDestinationConfirmed(mailLabelText)
                        ) { userId ->
                            moveMessage(userId, messageId, it.id.labelId)
                        }
                    } ?: emitNewStateFrom(MessageDetailEvent.ErrorMovingMessage)
                }

                // Unsupported flow
                else -> emitNewStateFrom(MessageDetailEvent.ErrorMovingMessage)
            }
        }
    }

    private fun observeMessageWithLabels(messageId: MessageId) = primaryUserId.flatMapLatest { userId ->
        val contacts = getContacts(userId).getOrElse { emptyList() }
        return@flatMapLatest combine(
            observeMessageWithLabels(userId, messageId),
            observeFolderColor(userId),
            observeAutoDeleteSetting()
        ) { messageWithLabelsEither, folderColor, autoDelete ->
            messageWithLabelsEither.fold(
                ifLeft = { MessageDetailEvent.NoCachedMetadata },
                ifRight = { messageWithLabels ->
                    MessageDetailEvent.MessageWithLabelsEvent(
                        messageWithLabels,
                        contacts,
                        folderColor,
                        autoDelete
                    )
                }
            )
        }
    }.onEach { event ->
        emitNewStateFrom(event)
    }.launchIn(viewModelScope)

    private fun getMessageBody(messageId: MessageId) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val decryptionResult = getDecryptedMessageBody(userId, messageId)
            decryptionResult.onRight { markMessageRead() }
            val event = decryptionResult.fold(
                ifLeft = { getDecryptedMessageBodyError ->
                    when (getDecryptedMessageBodyError) {
                        is GetDecryptedMessageBodyError.Decryption -> {
                            MessageDetailEvent.ErrorDecryptingMessageBody(
                                messageBody = messageBodyUiModelMapper.toUiModel(getDecryptedMessageBodyError)
                            )
                        }

                        is GetDecryptedMessageBodyError.Data -> {
                            MessageDetailEvent.ErrorGettingMessageBody(
                                isNetworkError = getDecryptedMessageBodyError.dataError == DataError.Remote.Http(
                                    NetworkError.NoNetwork
                                )
                            )
                        }
                    }
                },
                ifRight = {
                    if (it.attachments.isNotEmpty()) {
                        updateObservedAttachments(it.attachments)
                    }

                    val initialUiModel = messageBodyUiModelMapper.toUiModel(userId, it, null)
                    MessageDetailEvent.MessageBodyEvent(
                        initialUiModel,
                        getInitialBodyExpandCollapseMode(initialUiModel)
                    )
                }
            )
            emitNewStateFrom(event)
        }
    }

    private fun getInitialBodyExpandCollapseMode(uiModel: MessageBodyUiModel): MessageBodyExpandCollapseMode {
        return if (uiModel.shouldShowExpandCollapseButton) {
            MessageBodyExpandCollapseMode.Collapsed
        } else {
            MessageBodyExpandCollapseMode.NotApplicable
        }
    }

    private fun expandOrCollapseMessageBody() {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.ExpandOrCollapseMessageBody)
        }
    }

    private fun reloadMessageBody(messageId: MessageId) {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.Reload)
            getMessageBody(messageId)
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

    private fun observeBottomBarActions(messageId: MessageId) = primaryUserId.flatMapLatest { userId ->
        observeDetailActions(userId, messageId).mapLatest { either ->
            either.fold(
                ifLeft = { MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                ifRight = { actions ->
                    val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }.toImmutableList()
                    MessageDetailEvent.MessageBottomBarEvent(
                        BottomBarEvent.ShowAndUpdateActionsData(actionUiModels)
                    )
                }
            )
        }
    }.onEach { event ->
        emitNewStateFrom(event)
    }.launchIn(viewModelScope)

    private fun showMoreActionsBottomSheetAndLoadData(initialEvent: MessageViewAction.RequestMoreActionsBottomSheet) {
        viewModelScope.launch {
            emitNewStateFrom(initialEvent)

            val userId = primaryUserId.first()
            val contacts = observeContacts(userId).first().getOrNull()
            val message = observeMessage(userId, initialEvent.messageId).first().getOrElse {
                Timber.e("Unable to fetch message data.")
                emitNewStateFrom(MessageViewAction.DismissBottomSheet)
                return@launch
            }

            val sender = contacts?.let {
                return@let resolveParticipantName(message.sender, it)
            }?.name ?: message.sender.name

            val actions = getBottomSheetActions(message = message)

            val event = MessageDetailEvent.MessageBottomSheetEvent(
                DetailMoreActionsBottomSheetState.MessageDetailMoreActionsBottomSheetEvent.DataLoaded(
                    affectingConversation = false,
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

    private fun showMoveToBottomSheetAndLoadData(initialEvent: MessageViewAction) {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeDestinationMailLabels(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                MessageDetailEvent.MessageBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        folders.toUiModels(color).let { it.folders + it.systems }.toImmutableList(),
                        entryPoint = MoveToBottomSheetEntryPoint.Message(messageId)
                    )
                )
            }
        }.onStart {
            emitNewStateFrom(initialEvent)
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showContactActionsBottomSheetAndLoadData(action: MessageViewAction.RequestContactActionsBottomSheet) {
        viewModelScope.launch {
            emitNewStateFrom(action)

            val userId = primaryUserId.first()
            val contact = findContactByEmail(userId, action.participant.participantAddress)

            val event = MessageDetailEvent.MessageBottomSheetEvent(
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

    private fun showLabelAsBottomSheetAndLoadData(initialEvent: MessageViewAction) {
        viewModelScope.launch {
            emitNewStateFrom(initialEvent)

            val userId = primaryUserId.first()
            val event = MessageDetailEvent.MessageBottomSheetEvent(
                loadDataForMessageLabelAsBottomSheet(userId, messageId)
            )
            emitNewStateFrom(event)
        }
    }

    private fun onLabelAsConfirmed(archiveSelected: Boolean) {
        viewModelScope.launch {
            val userId = primaryUserId.first()

            val labelAsData = mutableDetailState.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

            val action = suspend {
                onMessageLabelAsConfirmed(
                    userId = userId,
                    messageId = messageId,
                    labelUiModelsWithSelectedState = labelAsData.labelUiModelsWithSelectedState,
                    archiveSelected = archiveSelected
                )
            }

            if (archiveSelected) {
                performSafeExitAction(
                    onLeft = MessageDetailEvent.ErrorLabelingMessage,
                    onRight = MessageViewAction.LabelAsConfirmed(true)
                ) {
                    action()
                }
            } else {
                val operation = action().fold(
                    ifLeft = { MessageDetailEvent.ErrorLabelingMessage },
                    ifRight = { MessageViewAction.LabelAsConfirmed(false) }
                )
                emitNewStateFrom(operation)
            }
        }
    }

    private fun onShowAllAttachmentsClicked() {
        val state = state.value.messageBodyState as? MessageBodyState.Data
        if (state == null) {
            Timber.e("MessageBodyState is not MessageBodyState.Data")
            return
        }
        val attachmentGroupUiModel = state.messageBodyUiModel.attachments
        val operation = MessageDetailEvent.MessageBodyEvent(
            state.messageBodyUiModel.copy(
                attachments = attachmentGroupUiModel?.copy(
                    limit = attachmentGroupUiModel.attachments.size
                )
            ),
            state.expandCollapseMode
        )
        viewModelScope.launch { emitNewStateFrom(operation) }
    }

    private fun onLabelSelected(labelId: LabelId) {
        viewModelScope.launch { emitNewStateFrom(MessageViewAction.LabelAsToggleAction(labelId)) }
    }

    private fun onMessageBodyLinkClicked(uri: Uri) {
        viewModelScope.launch { emitNewStateFrom(MessageViewAction.MessageBodyLinkClicked(uri)) }
    }

    private fun onDoNotAskLinkConfirmationChecked() {
        viewModelScope.launch { updateLinkConfirmationSetting(false) }
    }

    private fun observeAttachments() = primaryUserId.flatMapLatest { userId ->
        attachmentsState.flatMapLatest { attachments ->
            attachments.asFlow()
                .flatMapMerge { attachment ->
                    observeMessageAttachmentStatus(userId, messageId, attachment.attachmentId)
                        .map { status ->
                            MessageDetailEvent.AttachmentStatusChanged(
                                attachment.attachmentId,
                                status.status
                            )
                        }
                        .distinctUntilChanged()
                }
        }
    }.onEach { event ->
        emitNewStateFrom(event)
    }.launchIn(viewModelScope)

    private fun observeCustomizeToolbarSpotlights() = observeCustomizeToolbarSpotlight()
        .onEach {
            emitNewStateFrom(MessageDetailEvent.RequestCustomizeToolbarSpotlight)
        }
        .launchIn(viewModelScope)

    private fun updateSpotlightLastSeenTimestamp() = viewModelScope.launch {
        updateCustomizeToolbarSpotlight()
    }

    private fun onOpenAttachmentClicked(attachmentId: AttachmentId) {
        viewModelScope.launch {
            // Only one download is allowed at a time
            if (isAttachmentDownloadInProgress().not()) {
                val userId = primaryUserId.first()
                getAttachmentIntentValues(userId, requireMessageId(), attachmentId).fold(
                    ifLeft = {
                        Timber.d("Failed to download attachment: $it")
                        val event = when (it) {
                            is DataError.Local.OutOfMemory -> MessageDetailEvent.ErrorGettingAttachmentNotEnoughSpace
                            else -> MessageDetailEvent.ErrorGettingAttachment
                        }
                        emitNewStateFrom(event)
                    },
                    ifRight = { values -> emitNewStateFrom(MessageDetailEvent.OpenAttachmentEvent(values)) }
                )
            } else {
                emitNewStateFrom(MessageDetailEvent.ErrorAttachmentDownloadInProgress)
            }
        }
    }

    private fun updateObservedAttachments(attachments: List<MessageAttachment>) {
        attachmentsState.update { it + attachments }
    }

    private fun handleReportPhishing(action: MessageViewAction.ReportPhishing) {
        viewModelScope.launch {
            val operation = when (networkManager.networkStatus) {
                NetworkStatus.Disconnected -> MessageDetailEvent.ReportPhishingRequested(
                    messageId = action.messageId,
                    isOffline = true
                )

                else -> MessageDetailEvent.ReportPhishingRequested(messageId = action.messageId, isOffline = false)
            }
            emitNewStateFrom(operation)
        }
    }

    private fun handleReportPhishingConfirmed(action: MessageViewAction.ReportPhishingConfirmed) {
        viewModelScope.launch {
            reportPhishingMessage(primaryUserId.first(), messageId).onLeft {
                Timber.e("Failed to report phishing: $it")
            }
            emitNewStateFrom(action)
        }
    }

    private fun handleOpenInProtonCalendar() {
        viewModelScope.launch {
            val isInstalled = isProtonCalendarInstalled()
            if (isInstalled) {
                val metadata = (state.value.messageMetadataState as? MessageMetadataState.Data)?.messageDetailHeader
                val messageBody = (state.value.messageBodyState as? MessageBodyState.Data)?.messageBodyUiModel
                if (messageBody != null && metadata != null) {
                    handleOpenInProtonCalendar(metadata, messageBody)
                }
            } else {
                val intent = OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore
                emitNewStateFrom(MessageDetailEvent.HandleOpenProtonCalendarRequest(intent))
            }
        }
    }

    private suspend fun handleOpenInProtonCalendar(
        metadata: MessageDetailHeaderUiModel,
        messageBodyUiModel: MessageBodyUiModel
    ) {
        val sender = metadata.sender.participantAddress
        val recipient = messageBodyUiModel.userAddress?.email ?: return
        val firstCalendarAttachment = messageBodyUiModel.attachments?.attachments?.firstOrNull {
            it.mimeType.split(";").any { it == "text/calendar" }
        } ?: return

        getAttachmentIntentValues(
            userId = primaryUserId.first(),
            messageId = messageId,
            attachmentId = AttachmentId(firstCalendarAttachment.attachmentId)
        ).fold(
            ifLeft = { Timber.e("Failed to get attachment intent values: $it") },
            ifRight = { values ->
                val intent = OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar(values.uri, sender, recipient)
                emitNewStateFrom(MessageDetailEvent.HandleOpenProtonCalendarRequest(intent))
            }
        )
    }

    private fun handlePrint(context: Context) {
        val messageMetadataState = state.value.messageMetadataState
        val messageBodyState = state.value.messageBodyState
        if (messageMetadataState is MessageMetadataState.Data && messageBodyState is MessageBodyState.Data) {
            printMessage(
                context,
                messageMetadataState.messageDetailActionBar.subject,
                messageMetadataState.messageDetailHeader,
                messageBodyState.messageBodyUiModel,
                messageBodyState.expandCollapseMode,
                this@MessageDetailViewModel::loadEmbeddedImage
            )
        }
    }

    private fun handleArchive() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = MessageDetailEvent.ErrorMovingToArchive,
                onRight = MessageViewAction.Archive
            ) { userId ->
                moveMessage(userId, messageId, SystemLabelId.Archive.labelId)
            }
        }
    }

    private fun handleSpam() {
        viewModelScope.launch {
            performSafeExitAction(
                onLeft = MessageDetailEvent.ErrorMovingToSpam,
                onRight = MessageViewAction.Spam
            ) { userId ->
                moveMessage(userId, messageId, SystemLabelId.Spam.labelId)
            }
        }
    }

    private suspend fun emitNewStateFrom(operation: MessageDetailOperation) {
        val updatedState = messageDetailReducer.newStateFrom(state.value, operation)
        mutableDetailState.emit(updatedState)
    }

    private fun requireMessageId(): MessageId {
        val messageIdParam = savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY)
            ?: throw IllegalStateException("No Message id given")

        return MessageId(messageIdParam)
    }

    private suspend fun isAttachmentDownloadInProgress() =
        getDownloadingAttachmentsForMessages(primaryUserId.first(), listOf(MessageId(messageId.id))).isNotEmpty()

    /**
     * Equivalent of [ConversationDetailViewModel.performSafeExitAction].
     */
    private suspend fun performSafeExitAction(
        onLeft: MessageDetailOperation,
        onRight: MessageDetailOperation,
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

        val initialState = MessageDetailState.Loading
    }
}
