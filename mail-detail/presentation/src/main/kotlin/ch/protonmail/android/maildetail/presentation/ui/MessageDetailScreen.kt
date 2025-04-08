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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.domain.model.BasicContactInfo
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltip
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.dialog.ReportPhishingDialog
import ch.protonmail.android.maildetail.presentation.ui.footer.MessageDetailFooter
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeader
import ch.protonmail.android.maildetail.presentation.viewmodel.MessageDetailViewModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.ContactActionsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.DetailMoreActionsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.LabelAsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetContent
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageDetailScreen(
    modifier: Modifier = Modifier,
    actions: MessageDetail.Actions,
    viewModel: MessageDetailViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = MessageDetailViewModel.initialState)
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isSystemBackButtonClickEnabled = remember { mutableStateOf(true) }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    state.bottomSheetState?.let {
        // Avoids a "jumping" of the bottom sheet
        if (it.isShowEffectWithoutContent()) return@let

        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
                viewModel.submit(MessageViewAction.DismissBottomSheet)
            }
        }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(MessageViewAction.DismissBottomSheet)
    }

    BackHandler(!bottomSheetState.isVisible && isSystemBackButtonClickEnabled.value) {
        actions.recordMailboxScreenView()
        isSystemBackButtonClickEnabled.value = false
        scope.launch {
            awaitFrame()
            onBackPressedDispatcher?.onBackPressed()
        }
    }

    DeleteDialog(
        state = state.deleteDialogState,
        confirm = { viewModel.submit(MessageViewAction.DeleteConfirmed) },
        dismiss = { viewModel.submit(MessageViewAction.DeleteDialogDismissed) }
    )

    ReportPhishingDialog(
        state = state.reportPhishingDialogState,
        onConfirm = { viewModel.submit(MessageViewAction.ReportPhishingConfirmed) },
        onDismiss = { viewModel.submit(MessageViewAction.ReportPhishingDismissed) }
    )

    SpotlightTooltip(
        dialogState = state.spotlightTooltip,
        ctaClick = actions.navigateToCustomizeToolbar,
        dismiss = { viewModel.submit(MessageViewAction.SpotlightDismissed) },
        displayed = { viewModel.submit(MessageViewAction.SpotlightDisplayed) }
    )

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = MoveToBottomSheetContent.Actions(
                        onAddFolderClick = actions.onAddFolder,
                        onFolderSelected = {
                            viewModel.submit(MessageViewAction.MoveToDestinationSelected(it))
                        },
                        onDoneClick = { mailLabelText, _ ->
                            viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(mailLabelText))
                        },
                        onDismiss = {
                            viewModel.submit(MessageViewAction.DismissBottomSheet)
                        }
                    )
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(MessageViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { archiveSelected, _ ->
                            viewModel.submit(MessageViewAction.LabelAsConfirmed(archiveSelected))
                        }
                    )
                )

                is DetailMoreActionsBottomSheetState -> DetailMoreActionsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = DetailMoreActionsBottomSheetContent.Actions(
                        onReply = actions.onReply,
                        onReplyAll = actions.onReplyAll,
                        onForward = actions.onForward,
                        onMarkUnread = { viewModel.submit(MessageViewAction.MarkUnread) },
                        onLabel = { viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet) },
                        onViewInLightMode = {
                            viewModel.submit(MessageViewAction.SwitchViewMode(ViewModePreference.LightMode))
                        },
                        onViewInDarkMode = {
                            viewModel.submit(MessageViewAction.SwitchViewMode(ViewModePreference.DarkMode))
                        },
                        onMoveToTrash = { viewModel.submit(MessageViewAction.Trash) },
                        onDelete = { },
                        onDeleteMessage = { viewModel.submit(MessageViewAction.DeleteRequested) },
                        onMoveToArchive = { viewModel.submit(MessageViewAction.Archive) },
                        onMoveToSpam = { viewModel.submit(MessageViewAction.Spam) },
                        onMove = { viewModel.submit(MessageViewAction.RequestMoveToBottomSheet) },
                        onPrint = { viewModel.submit(MessageViewAction.PrintRequested) },
                        onReportPhishing = { viewModel.submit(MessageViewAction.ReportPhishing(it)) },
                        onMoveToTrashConversation = {},
                        onMoveToArchiveConversation = {},
                        onLabelConversation = {},
                        onMoveConversation = {},
                        onMoveToSpamConversation = {},
                        onMarkUnreadConversation = {},
                        onPrintLastMessage = {},
                        onReplyConversation = {},
                        onForwardConversation = {},
                        onReplyAllConversation = {},
                        onOpenCustomizeToolbar = actions.navigateToCustomizeToolbar
                    )
                )

                is ContactActionsBottomSheetState -> ContactActionsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = ContactActionsBottomSheetContent.Actions(
                        onCopyAddressClicked = {
                            val message = context.getString(R.string.contact_actions_copy_address_performed)
                            context.copyTextToClipboard(
                                label = message,
                                text = it.address
                            )
                            viewModel.submit(MessageViewAction.DismissBottomSheet)
                            actions.showSnackbar(message)
                        },
                        onCopyNameClicked = {
                            val message = context.getString(R.string.contact_actions_copy_name_performed)
                            context.copyTextToClipboard(
                                label = message,
                                text = it.name
                            )
                            viewModel.submit(MessageViewAction.DismissBottomSheet)
                            actions.showSnackbar(message)
                        },
                        onAddContactClicked = { actions.onAddContact(BasicContactInfo(it.name, it.address)) },
                        onNewMessageClicked = { actions.onComposeNewMessage(it.address) },
                        onViewContactDetailsClicked = { actions.onViewContactDetails(it) }
                    )
                )

                else -> Unit
            }
        }
    ) {
        MessageDetailScreen(
            modifier = modifier,
            state = state,
            actions = MessageDetailScreen.Actions(
                onExit = actions.onExit,
                onReload = { viewModel.submit(MessageViewAction.Reload) },
                onStarClick = { viewModel.submit(MessageViewAction.Star) },
                onTrashClick = { viewModel.submit(MessageViewAction.Trash) },
                onUnStarClick = { viewModel.submit(MessageViewAction.UnStar) },
                onUnreadClick = { viewModel.submit(MessageViewAction.MarkUnread) },
                onMoveClick = { viewModel.submit(MessageViewAction.RequestMoveToBottomSheet) },
                onLabelAsClick = { viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet) },
                onMoreActionsClick = { viewModel.submit(MessageViewAction.RequestMoreActionsBottomSheet(it)) },
                onMessageBodyLinkClicked = { viewModel.submit(MessageViewAction.MessageBodyLinkClicked(it)) },
                onDoNotAskLinkConfirmationAgain = { viewModel.submit(MessageViewAction.DoNotAskLinkConfirmationAgain) },
                onOpenMessageBodyLink = actions.openMessageBodyLink,
                onReplyClick = { actions.onReply(it) },
                onReplyAllClick = { actions.onReplyAll(it) },
                onForwardClick = { actions.onForward(it) },
                onArchiveClick = { viewModel.submit(MessageViewAction.Archive) },
                onMoveToSpamClick = { viewModel.submit(MessageViewAction.Spam) },
                onReportPhishing = { viewModel.submit(MessageViewAction.ReportPhishing(it)) },
                onDeleteClick = { viewModel.submit(MessageViewAction.DeleteRequested) },
                onShowAllAttachmentsClicked = { viewModel.submit(MessageViewAction.ShowAllAttachments) },
                onAttachmentClicked = { viewModel.submit(MessageViewAction.OnAttachmentClicked(it)) },
                openAttachment = actions.openAttachment,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                loadEmbeddedImage = { messageId, contentId -> viewModel.loadEmbeddedImage(messageId, contentId) },
                onExpandCollapseButtonClicked = { viewModel.submit(MessageViewAction.ExpandOrCollapseMessageBody) },
                onLoadRemoteContent = { viewModel.submit(MessageViewAction.LoadRemoteContent(it)) },
                onLoadEmbeddedImages = { viewModel.submit(MessageViewAction.ShowEmbeddedImages(it)) },
                onLoadRemoteAndEmbeddedContent = {
                    viewModel.submit(MessageViewAction.LoadRemoteAndEmbeddedContent(it))
                },
                onOpenInProtonCalendar = { viewModel.submit(MessageViewAction.OpenInProtonCalendar(it)) },
                handleProtonCalendarRequest = actions.handleProtonCalendarRequest,
                onViewInLightMode = {
                    viewModel.submit(MessageViewAction.SwitchViewMode(ViewModePreference.LightMode))
                },
                onViewInDarkMode = { viewModel.submit(MessageViewAction.SwitchViewMode(ViewModePreference.DarkMode)) },
                onPrint = { viewModel.submit(MessageViewAction.Print(context)) },
                onAvatarClicked = { participantUiModel, avatarUiModel ->
                    viewModel.submit(
                        MessageViewAction.RequestContactActionsBottomSheet(
                            participantUiModel,
                            avatarUiModel
                        )
                    )
                },
                onParticipantClicked = { participantUiModel, avatarUiModel ->
                    viewModel.submit(
                        MessageViewAction.RequestContactActionsBottomSheet(
                            participantUiModel,
                            avatarUiModel
                        )
                    )
                },
                onViewEntireMessageClicked = actions.onViewEntireMessageClicked,
                navigateToCustomizeToolbar = actions.navigateToCustomizeToolbar
            )
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MessageDetailScreen(
    state: MessageDetailState,
    actions: MessageDetailScreen.Actions,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(snapAnimationSpec = null)
    val snackbarHostState = ProtonSnackbarHostState()
    val linkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }
    val phishingLinkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }

    ConsumableLaunchedEffect(state.exitScreenEffect) { actions.onExit(null) }

    state.exitScreenWithMessageEffect.consume()?.let {
        actions.onExit(it)
    }
    ConsumableTextEffect(state.error) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = string)
    }
    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) {
        if (state.requestPhishingLinkConfirmation) {
            phishingLinkConfirmationDialogState.value = it
        } else if (state.requestLinkConfirmation) {
            linkConfirmationDialogState.value = it
        } else {
            actions.onOpenMessageBodyLink(it)
        }
    }
    ConsumableLaunchedEffect(effect = state.openAttachmentEffect) {
        actions.openAttachment(it)
    }
    ConsumableLaunchedEffect(effect = state.openProtonCalendarIntent) {
        actions.handleProtonCalendarRequest(it)
    }

    if (linkConfirmationDialogState.value != null) {
        ExternalLinkConfirmationDialog(
            onCancelClicked = {
                linkConfirmationDialogState.value = null
            },
            onContinueClicked = { doNotShowAgain ->
                linkConfirmationDialogState.value?.let { actions.onOpenMessageBodyLink(it) }
                linkConfirmationDialogState.value = null
                if (doNotShowAgain) {
                    actions.onDoNotAskLinkConfirmationAgain()
                }
            },
            linkUri = linkConfirmationDialogState.value
        )
    }

    if (phishingLinkConfirmationDialogState.value != null) {
        PhishingLinkConfirmationDialog(
            onCancelClicked = { phishingLinkConfirmationDialogState.value = null },
            onContinueClicked = {
                phishingLinkConfirmationDialogState.value?.let { actions.onOpenMessageBodyLink(it) }
            },
            linkUri = phishingLinkConfirmationDialogState.value
        )
    }

    // When SubjectHeader is first time composed, we need to get the its actual height to be able to calculate yOffset
    // for collapsing effect
    val subjectHeaderSizeCallback: (Int) -> Unit = {
        scrollBehavior.state.heightOffsetLimit = -it.toFloat()
    }

    Scaffold(
        modifier = modifier
            .testTag(MessageDetailScreenTestTags.RootItem)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                protonSnackbarHostState = snackbarHostState
            )
        },
        topBar = {
            val uiModel = (state.messageMetadataState as? MessageMetadataState.Data)?.messageDetailActionBar
            DetailScreenTopBar(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = scrollBehavior.state.heightOffset / 2f
                    },
                title = uiModel?.subject ?: DetailScreenTopBar.NoTitle,
                isStarred = uiModel?.isStarred,
                messageCount = null,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = { actions.onExit(null) },
                    onStarClick = actions.onStarClick,
                    onUnStarClick = actions.onUnStarClick
                ),
                subjectHeaderSizeCallback = subjectHeaderSizeCallback,
                topAppBarState = scrollBehavior.state
            )
        },
        bottomBar = {
            val messageId = remember(state.messageMetadataState) {
                (state.messageMetadataState as? MessageMetadataState.Data)?.messageDetailHeader?.messageIdUiModel?.id
            }
            BottomActionBar(
                state = state.bottomBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMarkRead = { Timber.d("message onMarkRead clicked") },
                    onMarkUnread = actions.onUnreadClick,
                    onStar = actions.onStarClick,
                    onUnstar = actions.onUnStarClick,
                    onMove = actions.onMoveClick,
                    onLabel = actions.onLabelAsClick,
                    onTrash = actions.onTrashClick,
                    onDelete = actions.onDeleteClick,
                    onReply = { messageId?.let { actions.onReplyClick(MessageId(it)) } },
                    onReplyAll = { messageId?.let { actions.onReplyAllClick(MessageId(it)) } },
                    onForward = { messageId?.let { actions.onForwardClick(MessageId(it)) } },
                    onArchive = actions.onArchiveClick,
                    onSpam = actions.onMoveToSpamClick,
                    onViewInLightMode = actions.onViewInLightMode,
                    onViewInDarkMode = actions.onViewInDarkMode,
                    onPrint = { messageId?.let { actions.onPrint(MessageId(it)) } },
                    onViewHeaders = { Timber.d("message onViewHeaders clicked") },
                    onViewHtml = { Timber.d("message onViewHtml clicked") },
                    onReportPhishing = { messageId?.let { actions.onReportPhishing(MessageId(it)) } },
                    onRemind = { Timber.d("message onRemind clicked") },
                    onSavePdf = { Timber.d("message onSavePdf clicked") },
                    onSenderEmail = { Timber.d("message onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("message onSaveAttachments clicked") },
                    onMore = { messageId?.let { actions.onMoreActionsClick(MessageId(it)) } },
                    onCustomizeToolbar = actions.navigateToCustomizeToolbar
                )
            )
        }
    ) { innerPadding ->
        when (state.messageMetadataState) {
            is MessageMetadataState.Data -> {
                val messageDetailContentActions = MessageDetailContent.Actions(
                    onReload = actions.onReload,
                    onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                    onShowAllAttachmentsClicked = actions.onShowAllAttachmentsClicked,
                    onAttachmentClicked = actions.onAttachmentClicked,
                    showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                    loadEmbeddedImage = actions.loadEmbeddedImage,
                    onReply = actions.onReplyClick,
                    onReplyAll = actions.onReplyAllClick,
                    onForward = actions.onForwardClick,
                    onExpandCollapseButtonClicked = actions.onExpandCollapseButtonClicked,
                    onMoreActionsClick = actions.onMoreActionsClick,
                    onLoadRemoteContent = actions.onLoadRemoteContent,
                    onLoadEmbeddedImages = actions.onLoadEmbeddedImages,
                    onLoadRemoteAndEmbeddedContent = actions.onLoadRemoteAndEmbeddedContent,
                    onOpenInProtonCalendar = actions.onOpenInProtonCalendar,
                    onPrint = actions.onPrint,
                    onAvatarClicked = actions.onAvatarClicked,
                    onParticipantClicked = actions.onParticipantClicked,
                    onViewEntireMessageClicked = actions.onViewEntireMessageClicked
                )
                MessageDetailContent(
                    padding = innerPadding,
                    messageMetadataState = state.messageMetadataState,
                    messageBannersState = state.messageBannersState,
                    messageBodyState = state.messageBodyState,
                    actions = messageDetailContentActions,
                    paddingOffsetDp = scrollBehavior.state.heightOffset.pxToDp()
                )
            }

            is MessageMetadataState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )
        }.exhaustive
    }
}

@Composable
@Suppress("UseComposableActions")
private fun MessageDetailContent(
    modifier: Modifier = Modifier,
    padding: PaddingValues,
    messageMetadataState: MessageMetadataState.Data,
    messageBannersState: MessageBannersState,
    messageBodyState: MessageBodyState,
    actions: MessageDetailContent.Actions,
    paddingOffsetDp: Dp = 0f.dp
) {
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = remember(padding, paddingOffsetDp) {
        PaddingValues(
            start = padding.calculateStartPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            end = padding.calculateEndPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            top = (
                padding.calculateTopPadding() + ProtonDimens.SmallSpacing + paddingOffsetDp
                ).coerceAtLeast(0f.dp),
            bottom = padding.calculateBottomPadding() + ProtonDimens.SmallSpacing
        )
    }

    val headerActions = MessageDetailHeader.Actions.Empty.copy(
        onReply = actions.onReply,
        onReplyAll = actions.onReplyAll,
        onMore = actions.onMoreActionsClick,
        onAvatarClicked = actions.onAvatarClicked,
        onParticipantClicked = actions.onParticipantClicked
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm),
        contentPadding = contentPadding
    ) {
        item {
            MessageDetailHeader(
                uiModel = messageMetadataState.messageDetailHeader,
                headerActions = headerActions
            )
            when (messageBannersState) {
                is MessageBannersState.Loading -> {}
                is MessageBannersState.Data -> MessageBanners(messageBannersState.messageBannersUiModel)
            }
            when (messageBodyState) {
                is MessageBodyState.Loading -> ProtonCenteredProgress()
                is MessageBodyState.Data -> {
                    MessageBody(
                        messageBodyUiModel = messageBodyState.messageBodyUiModel,
                        expandCollapseMode = messageBodyState.expandCollapseMode,
                        actions = MessageBody.Actions(
                            onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                            onShowAllAttachments = actions.onShowAllAttachmentsClicked,
                            onExpandCollapseButtonClicked = actions.onExpandCollapseButtonClicked,
                            onAttachmentClicked = actions.onAttachmentClicked,
                            loadEmbeddedImage = { messageId, contentId ->
                                actions.loadEmbeddedImage(messageId, contentId)
                            },
                            onReply = actions.onReply,
                            onReplyAll = actions.onReplyAll,
                            onForward = actions.onForward,
                            onEffectConsumed = { _, _ -> },
                            onLoadRemoteContent = actions.onLoadRemoteContent,
                            onLoadEmbeddedImages = actions.onLoadEmbeddedImages,
                            onLoadRemoteAndEmbeddedContent = actions.onLoadRemoteAndEmbeddedContent,
                            onOpenInProtonCalendar = actions.onOpenInProtonCalendar,
                            onPrint = actions.onPrint,
                            onViewEntireMessageClicked = actions.onViewEntireMessageClicked
                        )
                    )
                    MessageDetailFooter(
                        uiModel = messageMetadataState.messageDetailFooter,
                        actions = MessageDetailFooter.Actions.fromMessageDetailContentActions(actions)
                    )
                }

                is MessageBodyState.Error.Data -> MessageBodyLoadingError(
                    messageBodyState = messageBodyState,
                    onReload = actions.onReload
                )

                is MessageBodyState.Error.Decryption -> {
                    ProtonErrorMessage(errorMessage = stringResource(id = R.string.decryption_error))
                    MessageBody(
                        messageBodyUiModel = messageBodyState.encryptedMessageBody,
                        expandCollapseMode = MessageBodyExpandCollapseMode.NotApplicable,
                        actions = MessageBody.Actions(
                            onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                            onShowAllAttachments = actions.onShowAllAttachmentsClicked,
                            onExpandCollapseButtonClicked = actions.onExpandCollapseButtonClicked,
                            onAttachmentClicked = actions.onAttachmentClicked,
                            loadEmbeddedImage = { messageId, contentId ->
                                actions.loadEmbeddedImage(messageId, contentId)
                            },
                            onReply = { Timber.d("Message: Reply to message $it") },
                            onReplyAll = { Timber.d("Message: Reply All to message $it") },
                            onForward = { Timber.d("Message: Forward message $it") },
                            onEffectConsumed = { _, _ -> },
                            onLoadRemoteContent = { actions.onLoadRemoteContent(it) },
                            onLoadEmbeddedImages = { actions.onLoadEmbeddedImages(it) },
                            onLoadRemoteAndEmbeddedContent = { actions.onLoadRemoteAndEmbeddedContent(it) },
                            onOpenInProtonCalendar = { actions.onOpenInProtonCalendar(it) },
                            onPrint = actions.onPrint,
                            onViewEntireMessageClicked = actions.onViewEntireMessageClicked
                        )
                    )
                }
            }
        }
    }
}

object MessageDetail {

    data class Actions(
        val onExit: (message: ActionResult?) -> Unit,
        val openMessageBodyLink: (uri: Uri) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val handleProtonCalendarRequest: (values: OpenProtonCalendarIntentValues) -> Unit,
        val onAddLabel: () -> Unit,
        val onAddFolder: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onViewContactDetails: (ContactId) -> Unit,
        val onAddContact: (basicContactInfo: BasicContactInfo) -> Unit,
        val onComposeNewMessage: (recipientAddress: String) -> Unit,
        val showSnackbar: (message: String) -> Unit,
        val recordMailboxScreenView: () -> Unit,
        val navigateToCustomizeToolbar: () -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

object MessageDetailScreen {

    const val MESSAGE_ID_KEY = "message id"

    data class Actions(
        val onExit: (notifyUserMessage: ActionResult?) -> Unit,
        val onReload: () -> Unit,
        val onStarClick: () -> Unit,
        val onTrashClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onUnreadClick: () -> Unit,
        val onMoveClick: () -> Unit,
        val onLabelAsClick: () -> Unit,
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onOpenMessageBodyLink: (uri: Uri) -> Unit,
        val onDoNotAskLinkConfirmationAgain: () -> Unit,
        val onReplyClick: (MessageId) -> Unit,
        val onReplyAllClick: (MessageId) -> Unit,
        val onForwardClick: (MessageId) -> Unit,
        val onMoveToSpamClick: () -> Unit,
        val onDeleteClick: () -> Unit,
        val onArchiveClick: () -> Unit,
        val onShowAllAttachmentsClicked: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val handleProtonCalendarRequest: (values: OpenProtonCalendarIntentValues) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (MessageId, contentId: String) -> GetEmbeddedImageResult?,
        val onExpandCollapseButtonClicked: () -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageId) -> Unit,
        val onViewInLightMode: () -> Unit,
        val onViewInDarkMode: () -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onReportPhishing: (MessageId) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit,
        val navigateToCustomizeToolbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExit = {},
                onReload = {},
                onStarClick = {},
                onTrashClick = {},
                onUnStarClick = {},
                onUnreadClick = {},
                onMoveClick = {},
                onLabelAsClick = {},
                onMessageBodyLinkClicked = {},
                onOpenMessageBodyLink = {},
                onDoNotAskLinkConfirmationAgain = {},
                onReplyClick = {},
                onReplyAllClick = {},
                onForwardClick = {},
                onDeleteClick = {},
                onArchiveClick = {},
                onMoveToSpamClick = {},
                onReportPhishing = {},
                onShowAllAttachmentsClicked = {},
                onAttachmentClicked = {},
                openAttachment = {},
                showFeatureMissingSnackbar = {},
                loadEmbeddedImage = { _, _ -> null },
                onExpandCollapseButtonClicked = {},
                onMoreActionsClick = {},
                onLoadRemoteContent = {},
                onLoadEmbeddedImages = {},
                onLoadRemoteAndEmbeddedContent = {},
                onOpenInProtonCalendar = {},
                handleProtonCalendarRequest = {},
                onViewInLightMode = {},
                onViewInDarkMode = {},
                onPrint = {},
                onAvatarClicked = { _, _ -> },
                onParticipantClicked = { _, _ -> },
                onViewEntireMessageClicked = { _, _, _, _ -> },
                navigateToCustomizeToolbar = {}
            )
        }
    }
}

object MessageDetailContent {

    data class Actions(
        val onReload: () -> Unit,
        val onMessageBodyLinkClicked: (uri: Uri) -> Unit,
        val onShowAllAttachmentsClicked: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (MessageId, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onExpandCollapseButtonClicked: () -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageId) -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview(
    @PreviewParameter(MessageDetailsPreviewProvider::class) state: MessageDetailState
) {
    ProtonTheme3 {
        MessageDetailScreen(state = state, actions = MessageDetailScreen.Actions.Empty)
    }
}

object MessageDetailScreenTestTags {

    const val RootItem = "MessageDetailScreenRootItem"
}
