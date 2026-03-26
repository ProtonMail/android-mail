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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.entity.isOfflineError
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversationWithMessages
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.domain.usecase.AnswerRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.BlockSender
import ch.protonmail.android.maildetail.domain.usecase.GetDetailBottomBarActions
import ch.protonmail.android.maildetail.domain.usecase.GetRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.IsMessageSenderBlocked
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsLegitimate
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MessageViewStateCache
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.domain.usecase.UnblockSender
import ch.protonmail.android.maildetail.domain.usecase.UnsubscribeFromNewsletter
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.CollapseMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.DoNotAskLinkConfirmationAgain
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ExpandMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MarkUnread
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MessageBodyLinkClicked
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToInbox
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.MoveToTrash
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ScrollRequestCompleted
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ShowAllAttachmentsForMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.Star
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.UnStar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.RsvpWidgetUiModel
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.usecase.ApplyWebViewDarkModeFallback
import ch.protonmail.android.maildetail.presentation.usecase.GetMoreActionsBottomSheetData
import ch.protonmail.android.maildetail.presentation.usecase.LoadImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.MoreConversationActionsBottomSheetDataPayload
import ch.protonmail.android.maildetail.presentation.usecase.MoreMessageActionsBottomSheetDataPayload
import ch.protonmail.android.maildetail.presentation.usecase.ObservePrimaryUserAddress
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintConfiguration
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessage
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsLastMessageAutoExpandEnabled
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsWebViewDarkModeFallbackEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.maillabel.domain.extension.isOutbox
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ResolveSystemLabelId
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsItemId
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.mapper.MessageBodyTransformationsMapper
import ch.protonmail.android.mailmessage.domain.model.AttachmentDataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentListExpandCollapseMode
import ch.protonmail.android.mailmessage.domain.model.AvatarImageState
import ch.protonmail.android.mailmessage.domain.model.AvatarImageStates
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformationsOverride
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.model.RsvpAnswer
import ch.protonmail.android.mailmessage.domain.usecase.CancelScheduleSendMessage
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageBodyWithClickableLinks
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.attachment.isExpandable
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.SnoozeSheetState
import ch.protonmail.android.mailpadlocks.presentation.EncryptionInfoBottomSheetEvent
import ch.protonmail.android.mailsession.domain.usecase.ExecuteWhenOnline
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.mailsnooze.domain.SnoozeRepository
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeConversationId
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedTrackersBottomSheetEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@HiltViewModel(assistedFactory = ConversationDetailViewModel.Factory::class)
class ConversationDetailViewModel @AssistedInject constructor(
    @Assisted val conversationId: ConversationId,
    @Assisted val isSingleMessageModeEnabled: Boolean,
    @Assisted val initialScrollToMessageId: MessageIdUiModel?,
    @Assisted val openedFromLocation: LabelId,
    @Assisted val conversationEntryPoint: ConversationDetailEntryPoint,
    observePrimaryUserId: ObservePrimaryUserId,
    private val messageIdUiModelMapper: MessageIdUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper,
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper,
    private val markConversationAsRead: MarkConversationAsRead,
    private val markConversationAsUnread: MarkConversationAsUnread,
    private val moveConversation: MoveConversation,
    private val deleteConversations: DeleteConversations,
    private val observeConversationWithMessages: ObserveConversationWithMessages,
    private val getDetailBottomBarActions: GetDetailBottomBarActions,
    private val reducer: ConversationDetailReducer,
    private val starConversations: StarConversations,
    private val unStarConversations: UnStarConversations,
    private val starMessages: StarMessages,
    private val unStarMessages: UnStarMessages,
    private val getMessageBodyWithClickableLinks: GetMessageBodyWithClickableLinks,
    private val markMessageAsRead: MarkMessageAsRead,
    private val messageViewStateCache: MessageViewStateCache,
    private val observeConversationViewState: ObserveConversationViewState,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val loadImageAvoidDuplicatedExecution: LoadImageAvoidDuplicatedExecution,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val observePrivacySettings: ObservePrivacySettings,
    private val updateLinkConfirmationSetting: UpdateLinkConfirmationSetting,
    private val reportPhishingMessage: ReportPhishingMessage,
    private val isProtonCalendarInstalled: IsProtonCalendarInstalled,
    private val markMessageAsUnread: MarkMessageAsUnread,
    private val findContactByEmail: FindContactByEmail,
    private val getMoreActionsBottomSheetData: GetMoreActionsBottomSheetData,
    private val moveMessage: MoveMessage,
    private val deleteMessages: DeleteMessages,
    private val observePrimaryUserAddress: ObservePrimaryUserAddress,
    private val loadAvatarImage: LoadAvatarImage,
    private val observeAvatarImageStates: ObserveAvatarImageStates,
    private val markMessageAsLegitimate: MarkMessageAsLegitimate,
    private val unblockSender: UnblockSender,
    private val blockSender: BlockSender,
    private val isMessageSenderBlocked: IsMessageSenderBlocked,
    private val cancelScheduleSendMessage: CancelScheduleSendMessage,
    private val printMessage: PrintMessage,
    private val getRsvpEvent: GetRsvpEvent,
    private val answerRsvpEvent: AnswerRsvpEvent,
    private val snoozeRepository: SnoozeRepository,
    private val unsubscribeFromNewsletter: UnsubscribeFromNewsletter,
    private val toolbarRefreshSignal: ToolbarActionsRefreshSignal,
    private val executeWhenOnline: ExecuteWhenOnline,
    private val resolveSystemLabelId: ResolveSystemLabelId,
    @IsLastMessageAutoExpandEnabled private val isAutoExpandEnabled: FeatureFlag<Boolean>,
    @IsWebViewDarkModeFallbackEnabled private val isWebViewDarkModeFallbackEnabled: FeatureFlag<Boolean>,
    private val applyWebViewDarkModeFallback: ApplyWebViewDarkModeFallback
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = null
        )
        .filterNotNull()

    // Signal used to trigger a reload on connection change - as offline errors are treated as terminal ops.
    private val reloadSignal = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Signal triggered when an offline state is detected.
    //
    // This VM has multiple observers that can trigger an offline state independently.
    // For this reason, we need to ensure that multiple emissions are counted as one, as the signal
    // will cause a screen reload to display the fetched data to the user.
    private val offlineErrorSignal = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Signal triggered when conversation/message (depending on the view mode) data event is emitted.
    // Actions can't be observed from Rust as they are exposed as a one-time result, so we need
    // to manually re-trigger the loading of available actions.
    private val bottomBarRefreshSignal = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val mutableDetailState = MutableStateFlow(initialState)
    private val attachmentsState = MutableStateFlow<Map<MessageId, List<AttachmentMetadata>>>(emptyMap())
    private val showAllMessages = MutableStateFlow(false)

    val state: StateFlow<ConversationDetailState> = mutableDetailState.asStateFlow()

    val autoExpandLastMessageEnabled: StateFlow<Boolean> = flow {
        emit(isAutoExpandEnabled.get())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = false
    )

    init {
        Timber.d("Open detail screen for conversation ID: $conversationId")
        viewModelScope.launch {
            showAllMessages.value = resolveInitialShowAll()
            setupObservers()
        }

        setupOfflineObserver()
    }

    private fun setupObservers() {
        observeConversationData(conversationId)
        observeBottomBarActions(conversationId)
        observePrivacySettings()
        observeAttachments()
    }

    private fun setupOfflineObserver() {
        offlineErrorSignal
            .take(1)
            .onEach {
                executeWhenOnline(primaryUserId.first()) {
                    viewModelScope.launch {
                        Timber.d("Triggering reload signal for conversation $conversationId")
                        reloadSignal.emit(Unit)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    @Suppress("LongMethod", "ComplexMethod")
    fun submit(action: ConversationDetailViewAction) {
        viewModelScope.launch {
            when (action) {
                is Star -> handleStarAction()
                is UnStar -> handleUnStarAction()
                is ConversationDetailViewAction.MarkRead -> markAsRead()
                is MarkUnread -> handleMarkUnReadAction()
                is MoveToTrash -> handleTrashAction()
                is ConversationDetailViewAction.MoveToArchive -> handleArchiveAction()
                is ConversationDetailViewAction.MoveToSpam -> handleSpamAction()
                is ConversationDetailViewAction.DeleteConfirmed -> handleDeleteConfirmed(action)
                is ConversationDetailViewAction.DeleteMessageConfirmed -> handleDeleteMessageConfirmed(action)
                is ConversationDetailViewAction.RequestConversationMoveToBottomSheet ->
                    handleRequestMoveToBottomSheetAction()

                is ConversationDetailViewAction.MoveToCompleted -> handleMoveToCompleted(action)
                is MoveToInbox -> handleMoveToInboxAction()
                is ConversationDetailViewAction.RequestConversationLabelAsBottomSheet ->
                    handleRequestLabelAsBottomSheetAction()

                is ConversationDetailViewAction.RequestContactActionsBottomSheet ->
                    showContactActionsBottomSheetAndLoadData(action)

                is ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet ->
                    showMessageMoreActionsBottomSheet(action)

                is ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet ->
                    handleRequestMoreBottomSheetAction(action)

                is ConversationDetailViewAction.RequestMessageLabelAsBottomSheet ->
                    requestMessageLabelAsBottomSheet(action)

                is ConversationDetailViewAction.RequestMessageMoveToBottomSheet ->
                    requestMessageMoveToBottomSheet(action)

                is ConversationDetailViewAction.LabelAsCompleted -> handleLabelAsCompleted(action)

                is ExpandMessage -> onExpandMessage(action.messageId)
                is CollapseMessage -> onCollapseMessage(action.messageId)
                is DoNotAskLinkConfirmationAgain -> onDoNotAskLinkConfirmationChecked()
                is ShowAllAttachmentsForMessage -> showAllAttachmentsForMessage(action.messageId)
                is ConversationDetailViewAction.OnAttachmentClicked ->
                    onOpenAttachmentClicked(action.openMode, action.attachmentId)


                is ConversationDetailViewAction.ExpandOrCollapseAttachmentList ->
                    handleExpandOrCollapseAttachmentList(action.messageId)

                is ConversationDetailViewAction.ReportPhishingConfirmed -> handleReportPhishingConfirmed(action)
                is ConversationDetailViewAction.OpenInProtonCalendar -> handleOpenInProtonCalendar(action)
                is ConversationDetailViewAction.MarkMessageUnread -> handleMarkMessageUnread(action)
                is ConversationDetailViewAction.MoveMessage -> handleMoveMessage(action)

                is ConversationDetailViewAction.StarMessage -> handleStarMessage(action)
                is ConversationDetailViewAction.UnStarMessage -> handleUnStarMessage(action)

                is ConversationDetailViewAction.ChangeVisibilityOfMessages -> handleChangeVisibilityOfMessages()

                is ConversationDetailViewAction.DeleteRequested -> handleDeleteRequestedAction()
                is ConversationDetailViewAction.DeleteDialogDismissed,
                is ConversationDetailViewAction.DeleteMessageRequested,
                is MessageBodyLinkClicked,
                is ScrollRequestCompleted,
                is ConversationDetailViewAction.ReportPhishing,
                is ConversationDetailViewAction.ReportPhishingDismissed,
                is ConversationDetailViewAction.BlockSender,
                is ConversationDetailViewAction.BlockSenderDismissed,
                is ConversationDetailViewAction.MarkMessageAsLegitimate,
                is ConversationDetailViewAction.MarkMessageAsLegitimateDismissed,
                is ConversationDetailViewAction.EditScheduleSendMessageDismissed,
                is ConversationDetailViewAction.EditScheduleSendMessageRequested -> directlyHandleViewAction(action)

                is ConversationDetailViewAction.SwitchViewMode -> handleSwitchViewMode(action)

                is ConversationDetailViewAction.OnAvatarImageLoadRequested ->
                    handleOnAvatarImageLoadRequested(action.avatar)

                is ConversationDetailViewAction.ShowEmbeddedImages -> setOrRefreshMessageBody(
                    messageId = action.messageId,
                    override = MessageBodyTransformationsOverride.LoadEmbeddedImages
                )

                is ConversationDetailViewAction.ExpandOrCollapseMessageBody -> setOrRefreshMessageBody(
                    messageId = action.messageId,
                    override = MessageBodyTransformationsOverride.ToggleQuotedText
                )

                is ConversationDetailViewAction.LoadRemoteAndEmbeddedContent -> setOrRefreshMessageBody(
                    messageId = action.messageId,
                    override = MessageBodyTransformationsOverride.LoadRemoteContentAndEmbeddedImages
                )

                is ConversationDetailViewAction.LoadRemoteContent -> setOrRefreshMessageBody(
                    messageId = action.messageId,
                    override = MessageBodyTransformationsOverride.LoadRemoteContent
                )

                is ConversationDetailViewAction.MarkMessageAsLegitimateConfirmed ->
                    handleMarkMessageAsLegitimateConfirmed(action)

                is ConversationDetailViewAction.UnblockSender -> handleUnblockSender(action)
                is ConversationDetailViewAction.BlockSenderConfirmed -> handleBlockSenderConfirmed(action)
                is ConversationDetailViewAction.EditScheduleSendMessageConfirmed ->
                    handleEditScheduleSendMessage(action)

                is ConversationDetailViewAction.PrintMessage -> handlePrintMessage(action.context, action.messageId)
                is ConversationDetailViewAction.RetryRsvpEventLoading ->
                    handleGetRsvpEvent(action.messageId, refresh = true)

                is ConversationDetailViewAction.AnswerRsvpEvent -> handleAnswerRsvpEvent(
                    action.messageId,
                    action.answer
                )

                ConversationDetailViewAction.OnUnsnoozeConversationRequested -> handleUnsnoozeMessage()
                is ConversationDetailViewAction.SnoozeDismissed -> handleSnoozeDismissedAction(action)

                is ConversationDetailViewAction.SnoozeCompleted -> handleSnoozeCompletedAction(action)


                is ConversationDetailViewAction.RequestSnoozeBottomSheet -> requestSnoozeBottomSheet()
                is ConversationDetailViewAction.UnsubscribeFromNewsletter ->
                    handleUnsubscribeFromNewsletter(action.messageId)

                is ConversationDetailViewAction.LoadImagesAfterImageProxyFailure ->
                    handleLoadImagesAfterImageProxyFailure(action.messageId)

                is ConversationDetailViewAction.RequestBlockedTrackersBottomSheet ->
                    requestBlockedTrackersBottomSheet(action)

                is ConversationDetailViewAction.RequestEncryptionInfoBottomSheet ->
                    requestEncryptionInfoBottomSheet(action)
            }
        }
    }

    private fun handlePrintMessage(context: Context, messageId: MessageId) {
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
                        messageHeader = it.messageDetailHeaderUiModel,
                        messageBody = it.messageBodyUiModel,
                        loadEmbeddedImage = this@ConversationDetailViewModel::loadImage,
                        printConfiguration = PrintConfiguration(
                            showRemoteContent = !it.messageBodyUiModel.shouldShowRemoteContentBanner,
                            showEmbeddedImages = !it.messageBodyUiModel.shouldShowEmbeddedImagesBanner
                        )
                    )
                } else {
                    Timber.e("Can't print a message that is not expanded")
                }
            }
        }
    }

    private suspend fun handleSnoozeCompletedAction(action: ConversationDetailViewAction.SnoozeCompleted) {
        emitNewStateFrom(ConversationDetailEvent.ExitScreenWithMessage(action))
    }


    private suspend fun handleSnoozeDismissedAction(action: ConversationDetailViewAction.SnoozeDismissed) {
        emitNewStateFrom(action)
    }

    private suspend fun requestSnoozeBottomSheet() {
        withUserId { userId ->
            val selectedLabelId = openedFromLocation
            val event = SnoozeSheetState.SnoozeOptionsBottomSheetEvent.Ready(
                userId = userId,
                labelId = selectedLabelId,
                itemIds = listOf(SnoozeConversationId(conversationId.id))
            )
            emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event)).right()
        }
    }

    private suspend fun handleEditScheduleSendMessage(
        action: ConversationDetailViewAction.EditScheduleSendMessageConfirmed
    ) {
        emitNewStateFrom(action)
        withUserId { userId ->
            cancelScheduleSendMessage(userId, MessageId(action.messageId.id))
                .onLeft { error ->
                    if (error.isOfflineError()) {
                        emitNewStateFrom(ConversationDetailEvent.OfflineErrorCancellingScheduleSend(action.messageId))
                        return@onLeft
                    }
                    emitNewStateFrom(ConversationDetailEvent.ErrorCancellingScheduleSend(action.messageId))
                }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ScheduleSendCancelled(action.messageId)) }
        }
    }

    private suspend fun handleSwitchViewMode(action: ConversationDetailViewAction.SwitchViewMode) {
        val overrideTheme = when (action.overrideTheme) {
            MessageTheme.Light -> MessageBodyTransformationsOverride.ViewInLightMode(action.currentTheme)
            MessageTheme.Dark -> MessageBodyTransformationsOverride.ViewInDarkMode(action.currentTheme)
        }
        emitNewStateFrom(action)
        setOrRefreshMessageBody(MessageIdUiModel(action.messageId.id), overrideTheme)
    }

    fun loadImage(messageId: MessageId?, url: String) = messageId?.let { messageId ->
        runBlocking {
            loadImageAvoidDuplicatedExecution(
                userId = primaryUserId.first(),
                messageId = messageId,
                url = url,
                shouldLoadImagesSafely = messageViewStateCache.getShouldLoadImagesSafely(messageId),
                coroutineContext = viewModelScope.coroutineContext
            ).fold(
                ifLeft = {
                    when (it) {
                        is AttachmentDataError.ProxyFailed -> {
                            emitNewStateFrom(ConversationDetailEvent.ErrorLoadingImageProxyFailed(messageId))
                            null
                        }

                        is AttachmentDataError.Other -> null
                    }
                },
                ifRight = { it }
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
    }
        .restartableOn(reloadSignal)
        .launchIn(viewModelScope)

    @Suppress("LongMethod")
    private fun observeConversationData(conversationId: ConversationId) = primaryUserId.flatMapLatest { userId ->
        showAllMessages.flatMapLatest { showAllMessages ->
            combine(
                observeConversationWithMessages(
                    userId,
                    conversationId,
                    openedFromLocation,
                    conversationEntryPoint,
                    showAllMessages
                ),
                observeConversationViewState(),
                observePrimaryUserAddress(),
                observeAvatarImageStates()
            ) { conversationWithMessagesEither, conversationViewState, primaryUserAddress, avatarImageStates ->

                val conversationWithMessages = conversationWithMessagesEither.getOrElse { error ->
                    return@combine when {
                        error.isOfflineError() -> {
                            signalOfflineError()
                            ConversationDataLoadingResult.Error(
                                metadataEvent = ConversationDetailEvent.NoNetworkError,
                                messagesEvent = ConversationDetailEvent.NoNetworkError
                            )
                        }

                        error is ConversationError.NullValueReturned -> {
                            ConversationDataLoadingResult.Exit
                        }

                        else -> {
                            ConversationDataLoadingResult.Error(
                                metadataEvent = ConversationDetailEvent.ErrorLoadingConversation,
                                messagesEvent = ConversationDetailEvent.ErrorLoadingMessages
                            )
                        }
                    }
                }

                // Build metadata event
                val metadataEvent = ConversationDetailEvent.ConversationData(
                    conversationUiModel = conversationMetadataMapper.toUiModel(conversationWithMessages.conversation),
                    hiddenMessagesBanner = conversationWithMessages.conversation.hiddenMessagesBanner.takeIf {
                        isSingleMessageModeEnabled.not()
                    },
                    showAllMessages = showAllMessages
                )

                // Filter messages for single message mode
                val displayMessages = when {
                    isSingleMessageModeEnabled -> {
                        val message = conversationWithMessages.messages.filterMessage(initialScrollToMessageId)
                        if (message == null) {
                            Timber.tag("SingleMessageMode")
                                .w("single message requested, message is not in convo $initialScrollToMessageId")
                            return@combine ConversationDataLoadingResult.Error(
                                metadataEvent = metadataEvent,
                                messagesEvent = ConversationDetailEvent.ErrorLoadingSingleMessage
                            )
                        }
                        message
                    }

                    else -> conversationWithMessages.messages.messages
                }

                val messagesUiModels = buildMessagesUiModels(
                    messages = displayMessages,
                    primaryUserAddress = primaryUserAddress,
                    currentViewState = conversationViewState,
                    avatarImageStates = avatarImageStates
                ).toImmutableList()

                val initialScrollTo = initialScrollToMessageId
                    ?: conversationWithMessages.messages.messageIdToOpen
                        ?.let { messageIdUiModelMapper.toUiModel(it) }

                val messagesEvent =
                    if (stateIsLoadingOrOffline() && allCollapsed(conversationViewState.messagesState)) {
                        ConversationDetailEvent.MessagesData(
                            messagesUiModels,
                            initialScrollTo,
                            openedFromLocation
                        )
                    } else {
                        val requestScrollTo =
                            requestScrollToMessageId(conversationViewState.messagesState) ?: initialScrollTo
                        ConversationDetailEvent.MessagesData(
                            messagesUiModels,
                            requestScrollTo,
                            openedFromLocation
                        )
                    }

                ConversationDataLoadingResult.Success(
                    conversationData = metadataEvent,
                    messagesData = messagesEvent
                )
            }
        }
    }
        .filterNotNull()
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .restartableOn(reloadSignal)
        .onEach { result ->
            // Here, we emit both metadata and messages events together, as they represent
            // a single logical update of conversation data.
            //
            // Bundling them in a single event becomes rather messy due to the `Affecting..Component`
            // interfaces in the reducers.
            // Given that the emissions are sequential and ordering is respected, it's safe to emit this way.
            when (result) {
                is ConversationDataLoadingResult.Success -> {
                    emitNewStateFrom(result.conversationData)
                    emitNewStateFrom(result.messagesData)
                    bottomBarRefreshSignal.emit(Unit)
                }

                is ConversationDataLoadingResult.Error -> {
                    emitNewStateFrom(result.metadataEvent)
                    emitNewStateFrom(result.messagesEvent)
                }

                is ConversationDataLoadingResult.Exit -> {
                    emitNewStateFrom(ConversationDetailEvent.ExitScreen)
                }
            }
        }
        .launchIn(viewModelScope)

    private sealed class ConversationDataLoadingResult {

        data class Success(
            val conversationData: ConversationDetailEvent.ConversationData,
            val messagesData: ConversationDetailEvent.MessagesData
        ) : ConversationDataLoadingResult()

        data class Error(
            val metadataEvent: ConversationDetailEvent,
            val messagesEvent: ConversationDetailEvent
        ) : ConversationDataLoadingResult()

        data object Exit : ConversationDataLoadingResult()
    }

    private fun ConversationMessages.filterMessage(idUiModel: MessageIdUiModel?) = this.messages
        .filter { it.messageId.id == idUiModel?.id }
        .toNonEmptyListOrNull()

    private fun stateIsLoadingOrOffline() = state.value.messagesState == ConversationDetailsMessagesState.Loading ||
        state.value.messagesState == ConversationDetailsMessagesState.Offline

    private fun allCollapsed(viewState: Map<MessageId, InMemoryConversationStateRepository.MessageState>): Boolean =
        viewState.values.all { it == InMemoryConversationStateRepository.MessageState.Collapsed }

    private fun isSingleMessageConversation(): Boolean {
        return when (val messagesState = state.value.messagesState) {
            is ConversationDetailsMessagesState.Data -> {
                messagesState.messages.size == 1
            }

            else -> false
        }
    }

    private suspend fun buildMessagesUiModels(
        messages: List<Message>,
        primaryUserAddress: String?,
        currentViewState: InMemoryConversationStateRepository.MessagesState,
        avatarImageStates: AvatarImageStates
    ): List<ConversationDetailMessageUiModel> {
        val messagesList = messages.map { message ->
            val avatarImageState = avatarImageStates.getStateForAddress(message.sender.address)
            val attachmentListExpandCollapseMode = currentViewState.attachmentsListExpandCollapseMode[message.messageId]
            val rsvpEventState = currentViewState.rsvpEvents[message.messageId]
            when (val viewState = currentViewState.messagesState[message.messageId]) {
                is InMemoryConversationStateRepository.MessageState.Expanding -> {
                    buildExpandingMessage(
                        buildCollapsedMessage(message, avatarImageState, primaryUserAddress)
                    )
                }

                is InMemoryConversationStateRepository.MessageState.Expanded -> {
                    buildExpandedMessage(
                        message,
                        avatarImageState,
                        primaryUserAddress,
                        viewState.decryptedBody,
                        attachmentListExpandCollapseMode,
                        rsvpEventState
                    )
                }

                else -> {
                    buildCollapsedMessage(message, avatarImageState, primaryUserAddress)
                }
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

    private fun buildCollapsedMessage(
        message: Message,
        avatarImageState: AvatarImageState,
        primaryUserAddress: String?
    ): ConversationDetailMessageUiModel.Collapsed = conversationMessageMapper.toUiModel(
        message = message,
        primaryUserAddress = primaryUserAddress,
        avatarImageState = avatarImageState
    )

    private fun buildExpandingMessage(
        collapsedMessage: ConversationDetailMessageUiModel.Collapsed
    ): ConversationDetailMessageUiModel.Expanding = conversationMessageMapper.toUiModel(
        collapsedMessage
    )

    private suspend fun buildExpandedMessage(
        message: Message,
        avatarImageState: AvatarImageState,
        primaryUserAddress: String?,
        decryptedBody: DecryptedMessageBody,
        attachmentListExpandCollapseMode: AttachmentListExpandCollapseMode?,
        rsvpEvent: InMemoryConversationStateRepository.RsvpEventState?
    ): ConversationDetailMessageUiModel.Expanded = conversationMessageMapper.toUiModel(
        message,
        avatarImageState,
        primaryUserAddress,
        decryptedBody,
        attachmentListExpandCollapseMode,
        rsvpEvent,
        primaryUserId.first()
    )

    @Suppress("LongMethod")
    private fun observeBottomBarActions(conversationId: ConversationId) = combine(
        primaryUserId,
        toolbarRefreshSignal.refreshEvents.onStart { emit(Unit) },
        bottomBarRefreshSignal.onStart { emit(Unit) }
    ) { userId, _, _ ->
        userId
    }.mapLatest { userId ->
        val errorEvent = ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions)
        val offlineEvent = ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.Offline)
        val labelId = openedFromLocation
        val themeOptions = MessageThemeOptions(MessageTheme.Dark)

        if (resolveLabelIdOrNull()?.labelId?.isOutbox() == true) {
            return@mapLatest ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.HideBottomSheet)
        }

        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) }
            if (messageId == null) {
                return@mapLatest errorEvent
            }
            getDetailBottomBarActions(userId, labelId, messageId, themeOptions).fold(
                ifLeft = {
                    if (it.isOfflineError()) offlineEvent else errorEvent
                },
                ifRight = { actions ->
                    val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }.toImmutableList()
                    ConversationDetailEvent.ConversationBottomBarEvent(
                        BottomBarEvent.ShowAndUpdateActionsData(
                            BottomBarTarget.Message(messageId.id),
                            actionUiModels
                        )
                    )
                }
            )
        } else {
            getDetailBottomBarActions(userId, labelId, conversationId).fold(
                ifLeft = {
                    if (it.isOfflineError()) offlineEvent else errorEvent
                },
                ifRight = { actions ->
                    val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }.toImmutableList()
                    ConversationDetailEvent.ConversationBottomBarEvent(
                        BottomBarEvent.ShowAndUpdateActionsData(
                            BottomBarTarget.Conversation,
                            actionUiModels
                        )
                    )
                }
            )
        }
    }
        .distinctUntilChanged()
        .restartableOn(reloadSignal)
        .onEach { event ->
            emitNewStateFrom(event)
        }
        .launchIn(viewModelScope)

    private suspend fun resolveLabelIdOrNull() = resolveSystemLabelId(
        userId = primaryUserId.first(),
        labelId = openedFromLocation
    ).getOrNull()

    private suspend fun showContactActionsBottomSheetAndLoadData(
        action: ConversationDetailViewAction.RequestContactActionsBottomSheet
    ) {
        emitNewStateFrom(action)

        val userId = primaryUserId.first()
        val contact = findContactByEmail(userId, action.participant.participantAddress)

        val primaryUserAddress = observePrimaryUserAddress().first()
        val isPrimaryUserAddress = primaryUserAddress == action.participant.participantAddress

        val senderBlocked = action.messageId?.let { isMessageSenderBlocked(userId, MessageId(it.id)) } ?: false

        val event = ConversationDetailEvent.ConversationBottomSheetEvent(
            ContactActionsBottomSheetState.ContactActionsBottomSheetEvent.ActionData(
                participant = Participant(
                    address = action.participant.participantAddress,
                    name = action.participant.participantName
                ),
                avatarUiModel = action.avatarUiModel,
                contactId = contact?.id,
                origin = action.messageId?.let {
                    ContactActionsBottomSheetState.Origin.MessageDetails(
                        MessageId(action.messageId.id)
                    )
                } ?: ContactActionsBottomSheetState.Origin.Unknown,
                isSenderBlocked = senderBlocked,
                isPrimaryUserAddress = isPrimaryUserAddress
            )
        )
        emitNewStateFrom(event)
    }

    private suspend fun handleRequestMoveToBottomSheetAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            requestMessageMoveToBottomSheet(
                ConversationDetailViewAction.RequestMessageMoveToBottomSheet(messageId)
            )
        } else {
            requestConversationMoveToBottomSheet(
                ConversationDetailViewAction.RequestConversationMoveToBottomSheet
            )
        }
    }

    private suspend fun requestConversationMoveToBottomSheet(
        operation: ConversationDetailViewAction.RequestConversationMoveToBottomSheet
    ) {
        emitNewStateFrom(operation)

        val event = MoveToBottomSheetState.MoveToBottomSheetEvent.Ready(
            userId = primaryUserId.first(),
            currentLabel = openedFromLocation,
            itemIds = listOf(MoveToItemId(conversationId.id)),
            entryPoint = MoveToBottomSheetEntryPoint.Conversation
        )

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun requestMessageMoveToBottomSheet(
        operation: ConversationDetailViewAction.RequestMessageMoveToBottomSheet
    ) {
        emitNewStateFrom(operation)

        val event = MoveToBottomSheetState.MoveToBottomSheetEvent.Ready(
            userId = primaryUserId.first(),
            currentLabel = openedFromLocation,
            itemIds = listOf(MoveToItemId(operation.messageId.id)),
            entryPoint = MoveToBottomSheetEntryPoint.Message(operation.messageId)
        )

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun handleRequestLabelAsBottomSheetAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            requestMessageLabelAsBottomSheet(
                ConversationDetailViewAction.RequestMessageLabelAsBottomSheet(messageId)
            )
        } else {
            requestConversationLabelAsBottomSheet(
                ConversationDetailViewAction.RequestConversationLabelAsBottomSheet
            )
        }
    }

    private suspend fun requestConversationLabelAsBottomSheet(
        operation: ConversationDetailViewAction.RequestConversationLabelAsBottomSheet
    ) {
        emitNewStateFrom(operation)

        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.Ready(
            userId = primaryUserId.first(),
            currentLabel = openedFromLocation,
            itemIds = listOf(LabelAsItemId(conversationId.id)),
            entryPoint = LabelAsBottomSheetEntryPoint.Conversation
        )

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun requestMessageLabelAsBottomSheet(
        operation: ConversationDetailViewAction.RequestMessageLabelAsBottomSheet
    ) {
        val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.Ready(
            userId = primaryUserId.first(),
            currentLabel = openedFromLocation,
            itemIds = listOf(LabelAsItemId(operation.messageId.id)),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(operation.messageId)
        )

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun requestEncryptionInfoBottomSheet(
        operation: ConversationDetailViewAction.RequestEncryptionInfoBottomSheet
    ) {
        emitNewStateFrom(operation)

        val event = EncryptionInfoBottomSheetEvent.Ready(operation.uiModel)

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun requestBlockedTrackersBottomSheet(
        operation: ConversationDetailViewAction.RequestBlockedTrackersBottomSheet
    ) {
        emitNewStateFrom(operation)

        val event = BlockedTrackersBottomSheetEvent.Ready(operation.elements)

        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(event))
    }

    private suspend fun handleLabelAsCompleted(operation: ConversationDetailViewAction.LabelAsCompleted) {
        val event = if (operation.wasArchived) {
            ConversationDetailEvent.ExitScreenWithMessage(operation)
        } else {
            operation
        }

        emitNewStateFrom(event)
    }

    private suspend fun handleMoveToCompleted(operation: ConversationDetailViewAction.MoveToCompleted) {
        val shouldExit = when (operation.entryPoint) {
            is MoveToBottomSheetEntryPoint.Message -> isSingleMessageModeEnabled
            is MoveToBottomSheetEntryPoint.Conversation -> true
            else -> false
        }

        val event = if (shouldExit) ConversationDetailEvent.ExitScreenWithMessage(operation) else operation

        emitNewStateFrom(event)
    }

    private suspend fun showMessageMoreActionsBottomSheet(
        initialEvent: ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet
    ) {
        emitNewStateFrom(initialEvent)

        val userId = primaryUserId.first()
        val labelId = openedFromLocation

        val bottomSheetDataPayload = MoreMessageActionsBottomSheetDataPayload(
            userId,
            labelId,
            initialEvent.messageId,
            initialEvent.themeOptions,
            initialEvent.entryPoint,
            resolveSubject() ?: ""
        )

        val moreActions = getMoreActionsBottomSheetData.forMessage(bottomSheetDataPayload) ?: return
        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(moreActions))
    }

    private suspend fun handleRequestMoreBottomSheetAction(
        action: ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet
    ) {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            showMessageMoreActionsBottomSheet(
                initialEvent = ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId = messageId,
                    themeOptions = MessageThemeOptions(MessageTheme.Dark),
                    entryPoint = action.entryPoint
                )
            )
        } else {
            showConversationMoreActionsBottomSheet(action)
        }
    }

    private suspend fun showConversationMoreActionsBottomSheet(
        initialEvent: ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet
    ) {
        emitNewStateFrom(initialEvent)

        val userId = primaryUserId.first()
        val labelId = openedFromLocation

        val bottomSheetDataPayload = MoreConversationActionsBottomSheetDataPayload(
            userId,
            labelId,
            conversationId,
            resolveSubject() ?: ""
        )

        val moreActions = getMoreActionsBottomSheetData.forConversation(bottomSheetDataPayload) ?: return
        emitNewStateFrom(ConversationDetailEvent.ConversationBottomSheetEvent(moreActions))
    }

    private suspend fun emitNewStateFrom(event: ConversationDetailOperation) {
        val newState = reducer.newStateFrom(state.value, event)
        mutableDetailState.update { newState }
    }

    private suspend fun handleMarkUnReadAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleMarkMessageUnread(ConversationDetailViewAction.MarkMessageUnread(messageId))
        } else {
            markAsUnread()
        }
    }

    private suspend fun markAsRead() {
        withUserId { userId ->
            markConversationAsRead(userId, openedFromLocation, conversationId)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMarkingAsRead) }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreen) }
        }
    }

    private suspend fun markAsUnread() {
        withUserId { userId ->
            markConversationAsUnread(userId, openedFromLocation, conversationId)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMarkingAsUnread) }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreen) }
        }
    }

    private suspend fun handleTrashAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleMoveMessage(ConversationDetailViewAction.MoveMessage.System.Trash(messageId))
        } else {
            moveConversationToTrash()
        }
    }

    private suspend fun moveConversationToTrash() {
        withUserId { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Trash)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMovingToTrash) }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreenWithMessage(MoveToTrash)) }
        }
    }

    private suspend fun handleSpamAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleMoveMessage(ConversationDetailViewAction.MoveMessage.System.Spam(messageId))
        } else {
            moveConversationToSpam()
        }
    }

    private suspend fun moveConversationToSpam() {
        withUserId { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Spam)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMovingConversation) }
                .onRight {
                    emitNewStateFrom(
                        ConversationDetailEvent.ExitScreenWithMessage(ConversationDetailViewAction.MoveToSpam)
                    )
                }
        }
    }

    private suspend fun handleArchiveAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleMoveMessage(ConversationDetailViewAction.MoveMessage.System.Archive(messageId))
        } else {
            moveConversationToArchive()
        }
    }

    private suspend fun moveConversationToArchive() {
        withUserId { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Archive)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMovingConversation) }
                .onRight {
                    emitNewStateFrom(
                        ConversationDetailEvent.ExitScreenWithMessage(ConversationDetailViewAction.MoveToArchive)
                    )
                }
        }
    }

    private suspend fun handleStarAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleStarMessage(ConversationDetailViewAction.StarMessage(messageId))
        } else {
            starConversation()
        }
    }

    private suspend fun starConversation() {
        withUserId { userId ->
            starConversations(userId, listOf(conversationId))
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorAddStar) }
                .onRight { emitNewStateFrom(ConversationDetailViewAction.Star) }
        }
    }

    private suspend fun handleUnStarAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleUnStarMessage(ConversationDetailViewAction.UnStarMessage(messageId))
        } else {
            unStarConversation()
        }
    }

    private suspend fun unStarConversation() {
        withUserId { userId ->
            val isStarredLocation = resolveSystemLabelId(userId, openedFromLocation)
                .getOrNull() == SystemLabelId.Starred

            val successEvent = if (isStarredLocation) {
                ConversationDetailEvent.ExitScreen
            } else {
                ConversationDetailViewAction.UnStar
            }
            unStarConversations(userId, listOf(conversationId))
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorRemoveStar) }
                .onRight { emitNewStateFrom(successEvent) }
        }
    }

    private suspend fun handleMoveToInboxAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            handleMoveMessage(ConversationDetailViewAction.MoveMessage.System.Inbox(messageId))
        } else {
            moveConversationToInbox()
        }
    }

    private suspend fun moveConversationToInbox() {
        withUserId { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Inbox)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMovingConversation) }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreenWithMessage(MoveToInbox)) }
        }
    }

    private suspend fun handleDeleteRequestedAction() {
        if (isSingleMessageModeEnabled) {
            val messageId = initialScrollToMessageId?.let { MessageId(it.id) } ?: return
            emitNewStateFrom(ConversationDetailViewAction.DeleteMessageRequested(messageId))
        } else {
            emitNewStateFrom(ConversationDetailViewAction.DeleteRequested)
        }
    }

    private suspend fun handleDeleteMessageConfirmed(action: ConversationDetailViewAction.DeleteMessageConfirmed) {
        emitNewStateFrom(action)

        withUserId { userId ->
            val currentLabelId = openedFromLocation

            deleteMessages(userId, listOf(action.messageId), currentLabelId)
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorDeletingMessage) }
                .onRight {
                    if (isSingleMessageModeEnabled) {
                        val event = ConversationDetailEvent.ExitScreenWithMessage(
                            ConversationDetailEvent.LastMessageDeleted
                        )
                        emitNewStateFrom(event)
                    }
                }
        }
    }

    private suspend fun handleDeleteConfirmed(action: ConversationDetailViewAction) {
        withUserId { userId ->
            deleteConversations(userId, listOf(conversationId))
                .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorDeletingConversation) }
                .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreenWithMessage(action)) }
        }
    }

    private suspend fun directlyHandleViewAction(action: ConversationDetailViewAction) {
        emitNewStateFrom(action)
    }

    private suspend fun onExpandMessage(messageId: MessageIdUiModel) = withContext(ioDispatcher) {
        val domainMsgId = MessageId(messageId.id)
        messageViewStateCache.setExpanding(domainMsgId)
        setOrRefreshMessageBody(messageId)
    }

    private suspend fun setOrRefreshMessageBody(
        messageId: MessageIdUiModel,
        override: MessageBodyTransformationsOverride? = null
    ) {
        val domainMsgId = MessageId(messageId.id)

        val currentTransformations = messageViewStateCache.getTransformations(domainMsgId)
            ?: MessageBodyTransformations.MessageDetailsDefaults

        val transformationsWithOverride = MessageBodyTransformationsMapper.applyOverride(
            currentTransformations, override
        )

        val transformationsWithDarkModeFallback = if (isWebViewDarkModeFallbackEnabled.get()) {
            applyWebViewDarkModeFallback(transformationsWithOverride)
        } else transformationsWithOverride

        processMessageBody(
            primaryUserId.first(),
            domainMsgId, messageId, transformationsWithDarkModeFallback
        )
    }

    private suspend fun processMessageBody(
        userId: UserId,
        domainMsgId: MessageId,
        uiMessageId: MessageIdUiModel,
        transformations: MessageBodyTransformations
    ) {
        getMessageBodyWithClickableLinks(userId, domainMsgId, transformations)
            .onRight { message ->
                messageViewStateCache.setExpanded(domainMsgId, message)
                messageViewStateCache.setTransformations(domainMsgId, transformations)

                if (message.attachments.isNotEmpty()) {
                    updateObservedAttachments(mapOf(domainMsgId to message.attachments))
                }

                if (message.isUnread) {
                    markMessageAsRead(userId, domainMsgId)
                }

                if (message.hasCalendarInvite) {
                    handleGetRsvpEvent(domainMsgId, refresh = false)
                }
            }
            .onLeft { error ->
                emitMessageBodyDecryptError(error, uiMessageId)
                messageViewStateCache.setCollapsed(domainMsgId)
            }
    }

    private suspend fun onCollapseMessage(messageId: MessageIdUiModel) {
        messageViewStateCache.setCollapsed(MessageId(messageId.id))
        removeObservedAttachments(MessageId(messageId.id))
    }

    private fun handleChangeVisibilityOfMessages() = showAllMessages.update { showAllMessages.value.not() }

    private suspend fun onDoNotAskLinkConfirmationChecked() {
        updateLinkConfirmationSetting(false)
    }

    private suspend fun emitMessageBodyDecryptError(error: GetMessageBodyError, messageId: MessageIdUiModel) {
        val errorState = when (error) {
            is GetMessageBodyError.Data -> if (error.dataError.isOfflineError()) {
                ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline(messageId)
            } else {
                ConversationDetailEvent.ErrorExpandingRetrieveMessageError(messageId)
            }

            is GetMessageBodyError.Decryption ->
                ConversationDetailEvent.ErrorExpandingDecryptMessageError(messageId)
        }

        emitNewStateFrom(errorState)
    }

    private suspend fun showAllAttachmentsForMessage(messageId: MessageIdUiModel) {
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
                emitNewStateFrom(operation)
            }
    }

    private suspend fun handleExpandOrCollapseAttachmentList(messageId: MessageIdUiModel) {
        val dataState = state.value.messagesState as? ConversationDetailsMessagesState.Data
        if (dataState == null) {
            Timber.e("Messages state is not data to perform expand or collapse attachments")
            return
        }

        val expandedMessage = dataState.messages
            .firstOrNull { it.messageId == messageId }
            as? ConversationDetailMessageUiModel.Expanded
            ?: return // Not found or not expanded

        val attachmentGroup = expandedMessage.messageBodyUiModel.attachments
            ?: return // No attachments

        val expandMode = when {
            !attachmentGroup.isExpandable() -> AttachmentListExpandCollapseMode.NotApplicable
            attachmentGroup.expandCollapseMode == AttachmentListExpandCollapseMode.Expanded ->
                AttachmentListExpandCollapseMode.Collapsed

            else -> AttachmentListExpandCollapseMode.Expanded
        }

        messageViewStateCache.updateAttachmentsExpandCollapseMode(
            MessageId(messageId.id),
            expandMode
        )
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

        }
    }
        .restartableOn(reloadSignal)
        .launchIn(viewModelScope)

    private suspend fun onOpenAttachmentClicked(openMode: AttachmentOpenMode, attachmentId: AttachmentId) {
        if (state.value.downloadingAttachmentId != null) {
            emitNewStateFrom(ConversationDetailEvent.ErrorAttachmentDownloadInProgress)
            return
        }
        emitNewStateFrom(ConversationDetailEvent.AttachmentDownloadStarted(attachmentId))
        withUserId { userId ->
            getAttachmentIntentValues(userId, openMode, attachmentId)
                .onLeft {
                    Timber.d("Failed to download attachment: $it")
                    emitNewStateFrom(ConversationDetailEvent.ErrorGettingAttachment)
                }
                .onRight { emitNewStateFrom(ConversationDetailEvent.OpenAttachmentEvent(it)) }
        }
    }

    private fun updateObservedAttachments(attachments: Map<MessageId, List<AttachmentMetadata>>) {
        attachmentsState.update { it + attachments }
    }

    private fun removeObservedAttachments(messageId: MessageId) {
        attachmentsState.update { it - MessageId(messageId.id) }
    }

    private suspend fun handleReportPhishingConfirmed(action: ConversationDetailViewAction.ReportPhishingConfirmed) {
        withUserId { userId ->
            reportPhishingMessage(userId, action.messageId)
                .onLeft { Timber.e("Error while reporting phishing message: $it") }
        }

        emitNewStateFrom(action)
    }

    private suspend fun handleOpenInProtonCalendar(action: ConversationDetailViewAction.OpenInProtonCalendar) {
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

    @MissingRustApi
    // AddressId not being exposed through with rust Message (for both Message and Participants) resulting in the client
    // not having enough info to determine with "recipient" is the current user for which to open the invite.
    // Currently getting "toRecipients.first()" to keep the API unchanged, this won't work in several cases.
    private suspend fun handleOpenInProtonCalendar(messageUiModel: ConversationDetailMessageUiModel.Expanded) {
        val sender = messageUiModel.messageDetailHeaderUiModel.sender.participantAddress
        val recipient = messageUiModel.messageDetailHeaderUiModel.toRecipients.first().participantAddress
        val firstCalendarAttachment = messageUiModel.messageBodyUiModel
            .attachments
            ?.attachments
            ?.firstOrNull { uiModel -> uiModel.isCalendar }

        if (firstCalendarAttachment == null) {
            getRsvpEventIntentValues(messageUiModel.messageRsvpWidgetUiModel).fold(
                ifLeft = {
                    emitNewStateFrom(ConversationDetailEvent.ErrorOpeningEventInCalendar)
                },
                ifRight = {
                    val intent = OpenProtonCalendarIntentValues.OpenUriInProtonCalendar(
                        it.eventId,
                        it.calendarId,
                        it.recurrenceId
                    )
                    emitNewStateFrom(ConversationDetailEvent.HandleOpenProtonCalendarRequest(intent))
                }
            )
        } else {
            getAttachmentIntentValues(
                userId = primaryUserId.first(),
                openMode = AttachmentOpenMode.Open,
                attachmentId = AttachmentId(firstCalendarAttachment.id.value)
            ).fold(
                ifLeft = {
                    Timber.d("Failed to download attachment: $it")
                    emitNewStateFrom(ConversationDetailEvent.ErrorOpeningEventInCalendar)
                },
                ifRight = {
                    val intent = OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar(it.uri, sender, recipient)
                    emitNewStateFrom(ConversationDetailEvent.HandleOpenProtonCalendarRequest(intent))
                }
            )
        }
    }

    private fun getRsvpEventIntentValues(rsvpWidgetUiModel: RsvpWidgetUiModel): Either<Unit, RsvpEventIntentValues> {
        val event = when (rsvpWidgetUiModel) {
            is RsvpWidgetUiModel.Shown -> rsvpWidgetUiModel.event
            else -> null
        }
        return if (event?.eventId != null && event.calendar?.calendarId != null) {
            RsvpEventIntentValues(
                event.eventId.id,
                event.calendar.calendarId.id,
                event.startsAt
            ).right()
        } else {
            Unit.left()
        }
    }

    private suspend fun handleMarkMessageUnread(action: ConversationDetailViewAction.MarkMessageUnread) {
        withUserId { userId ->
            if (isSingleMessageConversation()) {
                markMessageAsUnread(userId, action.messageId)
                    .onLeft { emitNewStateFrom(ConversationDetailEvent.ErrorMarkingAsUnread) }
                    .onRight { emitNewStateFrom(ConversationDetailEvent.ExitScreen) }
            } else {
                markMessageAsUnread(userId, action.messageId).onRight {
                    onCollapseMessage(MessageIdUiModel(action.messageId.id))
                    emitNewStateFrom(action)
                }
            }
        }
    }

    private suspend fun handleMoveMessage(action: ConversationDetailViewAction.MoveMessage) {
        val mailLabelId = when (action) {
            is ConversationDetailViewAction.MoveMessage.CustomFolder -> MailLabelId.Custom.Folder(action.labelId)
            is ConversationDetailViewAction.MoveMessage.System -> MailLabelId.System(action.labelId.labelId)
        }

        handleMoveMessage(mailLabelId = mailLabelId, mailLabelText = action.mailLabelText, messageId = action.messageId)
    }

    private suspend fun handleMoveMessage(
        mailLabelId: MailLabelId,
        mailLabelText: MailLabelText,
        messageId: MessageId
    ) {
        val userId = primaryUserId.first()

        if (mailLabelId is MailLabelId.System) {
            moveMessage(userId, messageId, SystemLabelId.enumOf(mailLabelId.labelId.id)).getOrNull()
        } else {
            moveMessage(userId, messageId, mailLabelId.labelId).getOrNull()
        } ?: return emitNewStateFrom(ConversationDetailEvent.ErrorMovingMessage)

        val event = if (isSingleMessageModeEnabled) {
            ConversationDetailEvent.LastMessageMoved(mailLabelText)
        } else {
            ConversationDetailEvent.MessageMoved(mailLabelText)
        }

        emitNewStateFrom(event)
    }

    private suspend fun handleStarMessage(starAction: ConversationDetailViewAction.StarMessage) {
        starMessages(primaryUserId.first(), listOf(starAction.messageId))
        emitNewStateFrom(starAction)
    }

    private suspend fun handleUnStarMessage(unStarAction: ConversationDetailViewAction.UnStarMessage) {
        unStarMessages(primaryUserId.first(), listOf(unStarAction.messageId))
        emitNewStateFrom(unStarAction)
    }

    private fun handleOnAvatarImageLoadRequested(avatarUiModel: AvatarUiModel) {
        (avatarUiModel as? AvatarUiModel.ParticipantAvatar)?.let { avatar ->
            loadAvatarImage(avatar.address, avatar.bimiSelector)
        }
    }

    private suspend fun handleMarkMessageAsLegitimateConfirmed(
        action: ConversationDetailViewAction.MarkMessageAsLegitimateConfirmed
    ) {
        withUserId { userId ->
            markMessageAsLegitimate(
                userId = userId,
                messageId = action.messageId
            )
                .onLeft { Timber.e("Failed to mark message ${action.messageId.id} as legitimate") }
                .onRight {
                    setOrRefreshMessageBody(MessageIdUiModel(action.messageId.id))
                }

        }
        emitNewStateFrom(action)
    }

    private suspend fun handleUnblockSender(action: ConversationDetailViewAction.UnblockSender) {
        withUserId { userId ->
            unblockSender(
                userId = userId,
                email = action.email
            )
                .onLeft { Timber.e("Failed to unblock sender in message ${action.messageId?.id}") }
                .onRight { action.messageId?.let { setOrRefreshMessageBody(it) } }
        }
    }

    private suspend fun handleBlockSenderConfirmed(action: ConversationDetailViewAction.BlockSenderConfirmed) {
        withUserId { userId ->
            blockSender(
                userId = userId,
                email = action.email
            )
                .onLeft { Timber.e("Failed to block sender in message ${action.messageId?.id}") }
                .onRight { action.messageId?.let { setOrRefreshMessageBody(it) } }

        }
        emitNewStateFrom(action)
    }

    private suspend fun handleGetRsvpEvent(messageId: MessageId, refresh: Boolean) {
        messageViewStateCache.updateRsvpEventLoading(messageId, refresh)
        getRsvpEvent(primaryUserId.first(), messageId).fold(
            ifLeft = { messageViewStateCache.updateRsvpEventError(messageId) },
            ifRight = { rsvpEvent ->
                messageViewStateCache.updateRsvpEventShown(messageId, rsvpEvent)
            }
        )
    }

    private suspend fun handleAnswerRsvpEvent(messageId: MessageId, answer: RsvpAnswer) {
        messageViewStateCache.updateRsvpEventAnswering(messageId, answer)
        answerRsvpEvent(primaryUserId.first(), messageId, answer).onLeft {
            emitNewStateFrom(ConversationDetailEvent.ErrorAnsweringRsvpEvent)
        }
        handleGetRsvpEvent(messageId, refresh = false)
    }

    private suspend fun handleUnsnoozeMessage() {
        withUserId { userId ->
            snoozeRepository.unSnoozeConversation(
                userId = userId,
                labelId = openedFromLocation,
                conversationIds = listOf(conversationId)
            ).onLeft { _ ->
                emitNewStateFrom(ConversationDetailEvent.ErrorUnsnoozing)
            }.onRight {
                emitNewStateFrom(
                    ConversationDetailEvent.ExitScreenWithMessage(
                        ConversationDetailEvent.UnsnoozeCompleted
                    )
                )
            }
        }
    }

    private suspend fun handleUnsubscribeFromNewsletter(messageId: MessageId) {
        unsubscribeFromNewsletter(primaryUserId.first(), messageId).fold(
            ifLeft = {
                Timber.e("Failed to unsubscribe from newsletter in message ${messageId.id}")
                emitNewStateFrom(ConversationDetailEvent.ErrorUnsubscribingFromNewsletter)
            },
            ifRight = { setOrRefreshMessageBody(MessageIdUiModel(messageId.id)) }
        )
    }

    private fun handleLoadImagesAfterImageProxyFailure(messageId: MessageIdUiModel) = viewModelScope.launch {
        messageViewStateCache.setShouldLoadImagesSafely(MessageId(messageId.id), false)
        emitNewStateFrom(ConversationDetailEvent.OnLoadImagesAfterImageProxyFailure(messageId))
    }

    /**
     * Resolves the initial state of the `showAllMessages` parameter.
     *
     * This is required as when the message is opened from the AllMail folder (not Almost All Mail)
     * the "Hidden messages" banner is not returned by Rust and the value should always be `true`.
     *
     * If the screen is also opened from the "single message mode" variant of the details screen, we need to make
     * sure that opening a trashed message from Starred folder works as expected.
     */
    private suspend fun resolveInitialShowAll(): Boolean {
        if (isSingleMessageModeEnabled) return true

        val label = resolveSystemLabelId(
            userId = primaryUserId.first(),
            labelId = openedFromLocation
        ).getOrNull() ?: return false

        return label == SystemLabelId.AllMail
    }

    private fun resolveSubject(): String? {
        return (state.value.conversationState as? ConversationDetailMetadataState.Data)
            ?.conversationUiModel
            ?.subject
    }

    private suspend fun withUserId(block: suspend (userId: UserId) -> Either<*, *>) {
        val userId = primaryUserId.first()
        block(userId)
    }

    private suspend fun signalOfflineError() {
        offlineErrorSignal.emit(Unit)
    }

    private fun <T> Flow<T>.restartableOn(signal: Flow<Unit>): Flow<T> = signal.onStart { emit(Unit) }
        .flatMapLatest { this@restartableOn }

    @AssistedFactory
    interface Factory {

        fun create(
            conversationId: ConversationId,
            isSingleMessageModeEnabled: Boolean,
            initialScrollToMessageId: MessageIdUiModel?,
            openedFromLocation: LabelId,
            conversationEntryPoint: ConversationDetailEntryPoint
        ): ConversationDetailViewModel
    }

    private companion object {

        val initialState = ConversationDetailState.Loading
    }

    data class RsvpEventIntentValues(val eventId: String, val calendarId: String, val recurrenceId: Long)
}
