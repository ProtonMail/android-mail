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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeader
import ch.protonmail.android.maildetail.presentation.viewmodel.MessageDetailViewModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.DetailMoreActionsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.LabelAsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetContent
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
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
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

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

    if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) { onDispose { viewModel.submit(MessageViewAction.DismissBottomSheet) } }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(MessageViewAction.DismissBottomSheet)
    }

    DeleteDialog(
        state = state.deleteDialogState,
        confirm = { viewModel.submit(MessageViewAction.DeleteConfirmed) },
        dismiss = { viewModel.submit(MessageViewAction.DeleteDialogDismissed) }
    )

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = MoveToBottomSheetContent.Actions(
                        onAddFolderClick = actions.onAddFolder,
                        onFolderSelected = {
                            viewModel.submit(MessageViewAction.MoveToDestinationSelected(it))
                        },
                        onDoneClick = { viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(it)) }
                    )
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(MessageViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { viewModel.submit(MessageViewAction.LabelAsConfirmed(it)) }
                    )
                )

                is DetailMoreActionsBottomSheetState -> DetailMoreActionsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = DetailMoreActionsBottomSheetContent.Actions(
                        onReply = actions.onReply,
                        onReplyAll = actions.onReplyAll,
                        onForward = actions.onForward
                    )
                )

                else -> {
                    if (bottomSheetState.isVisible) {
                        ProtonCenteredProgress()
                    }
                }
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
                onDeleteClick = { viewModel.submit(MessageViewAction.DeleteRequested) },
                onShowAllAttachmentsClicked = { viewModel.submit(MessageViewAction.ShowAllAttachments) },
                onAttachmentClicked = { viewModel.submit(MessageViewAction.OnAttachmentClicked(it)) },
                openAttachment = actions.openAttachment,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                loadEmbeddedImage = { viewModel.loadEmbeddedImage(it) },
                onExpandCollapseButtonClicked = {
                    viewModel.submit(MessageViewAction.ExpandOrCollapseMessageBody)
                },
                onLoadRemoteContent = { viewModel.submit(MessageViewAction.LoadRemoteContent(it)) },
                onLoadEmbeddedImages = { viewModel.submit(MessageViewAction.LoadEmbeddedImages(it)) },
                onLoadRemoteAndEmbeddedContent = {
                    viewModel.submit(MessageViewAction.LoadRemoteAndEmbeddedContent(it))
                }
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
    ConsumableTextEffect(state.exitScreenWithMessageEffect) { string ->
        actions.onExit(string)
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
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostState
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
            BottomActionBar(
                state = state.bottomBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMarkRead = { Timber.d("message onMarkRead clicked") },
                    onMarkUnread = actions.onUnreadClick,
                    onStar = { Timber.d("message onStar clicked") },
                    onUnstar = { Timber.d("message onUnstar clicked") },
                    onMove = actions.onMoveClick,
                    onLabel = actions.onLabelAsClick,
                    onTrash = actions.onTrashClick,
                    onDelete = actions.onDeleteClick,
                    onArchive = { Timber.d("message onArchive clicked") },
                    onSpam = { Timber.d("message onSpam clicked") },
                    onViewInLightMode = { Timber.d("message onViewInLightMode clicked") },
                    onViewInDarkMode = { Timber.d("message onViewInDarkMode clicked") },
                    onPrint = { Timber.d("message onPrint clicked") },
                    onViewHeaders = { Timber.d("message onViewHeaders clicked") },
                    onViewHtml = { Timber.d("message onViewHtml clicked") },
                    onReportPhishing = { Timber.d("message onReportPhishing clicked") },
                    onRemind = { Timber.d("message onRemind clicked") },
                    onSavePdf = { Timber.d("message onSavePdf clicked") },
                    onSenderEmail = { Timber.d("message onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("message onSaveAttachments clicked") },
                    onMore = { Timber.d("message onMore clicked") }
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
                    onLoadRemoteAndEmbeddedContent = actions.onLoadRemoteAndEmbeddedContent

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
        onMore = actions.onMoreActionsClick
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
            MailDivider()
            when (messageBannersState) {
                is MessageBannersState.Loading -> {}
                is MessageBannersState.Data -> MessageBanners(messageBannersState.messageBannersUiModel)
            }
            when (messageBodyState) {
                is MessageBodyState.Loading -> ProtonCenteredProgress()
                is MessageBodyState.Data -> MessageBody(
                    messageBodyUiModel = messageBodyState.messageBodyUiModel,
                    expandCollapseMode = messageBodyState.expandCollapseMode,
                    actions = MessageBody.Actions(
                        onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                        onShowAllAttachments = actions.onShowAllAttachmentsClicked,
                        onExpandCollapseButtonClicked = actions.onExpandCollapseButtonClicked,
                        onAttachmentClicked = actions.onAttachmentClicked,
                        loadEmbeddedImage = { _, contentId -> actions.loadEmbeddedImage(contentId) },
                        onReply = actions.onReply,
                        onReplyAll = actions.onReplyAll,
                        onForward = actions.onForward,
                        onLoadRemoteContent = actions.onLoadRemoteContent,
                        onLoadEmbeddedImages = actions.onLoadEmbeddedImages,
                        onLoadRemoteAndEmbeddedContent = actions.onLoadRemoteAndEmbeddedContent
                    )
                )

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
                            loadEmbeddedImage = { _, contentId -> actions.loadEmbeddedImage(contentId) },
                            onReply = { Timber.d("Message: Reply to message $it") },
                            onReplyAll = { Timber.d("Message: Reply All to message $it") },
                            onForward = { Timber.d("Message: Forward message $it") },
                            onLoadRemoteContent = { actions.onLoadRemoteContent(it) },
                            onLoadEmbeddedImages = { actions.onLoadEmbeddedImages(it) },
                            onLoadRemoteAndEmbeddedContent = { actions.onLoadRemoteAndEmbeddedContent(it) }
                        )
                    )
                }
            }
        }
    }
}

object MessageDetail {

    data class Actions(
        val onExit: (message: String?) -> Unit,
        val openMessageBodyLink: (uri: Uri) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val onAddLabel: () -> Unit,
        val onAddFolder: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
    )
}

object MessageDetailScreen {

    const val MESSAGE_ID_KEY = "message id"

    data class Actions(
        val onExit: (notifyUserMessage: String?) -> Unit,
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
        val onDeleteClick: () -> Unit,
        val onShowAllAttachmentsClicked: () -> Unit,
        val onAttachmentClicked: (attachmentId: AttachmentId) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (contentId: String) -> GetEmbeddedImageResult?,
        val onExpandCollapseButtonClicked: () -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit
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
                onShowAllAttachmentsClicked = {},
                onAttachmentClicked = {},
                openAttachment = {},
                showFeatureMissingSnackbar = {},
                loadEmbeddedImage = { null },
                onExpandCollapseButtonClicked = {},
                onMoreActionsClick = {},
                onLoadRemoteContent = {},
                onLoadEmbeddedImages = {},
                onLoadRemoteAndEmbeddedContent = {}
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
        val loadEmbeddedImage: (contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onExpandCollapseButtonClicked: () -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit
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
