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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.SubjectHeaderTransform
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.MessageDetailViewModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
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

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    onFolderSelected = { viewModel.submit(MessageViewAction.MoveToDestinationSelected(it)) },
                    onDoneClick = { viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(it)) }
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(MessageViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { viewModel.submit(MessageViewAction.LabelAsConfirmed(it)) }
                    )
                )

                null -> {
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
                onMessageBodyLinkClicked = { viewModel.submit(MessageViewAction.MessageBodyLinkClicked(it)) },
                onDoNotAskLinkConfirmationAgain = { viewModel.submit(MessageViewAction.DoNotAskLinkConfirmationAgain) },
                onOpenMessageBodyLink = actions.openMessageBodyLink,
                onReplyClick = { actions.onReply(it) },
                onReplyAllClick = { actions.onReplyAll(it) },
                onForwardClick = { actions.onForward(it) },
                onDeleteClick = { actions.showFeatureMissingSnackbar() },
                onShowAllAttachmentsClicked = { viewModel.submit(MessageViewAction.ShowAllAttachments) },
                onAttachmentClicked = { viewModel.submit(MessageViewAction.OnAttachmentClicked(it)) },
                openAttachment = actions.openAttachment,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                loadEmbeddedImage = { viewModel.loadEmbeddedImage(it) }
            )
        )
    }
}

@Composable
fun MessageDetailScreen(
    state: MessageDetailState,
    actions: MessageDetailScreen.Actions,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = ProtonSnackbarHostState()
    val linkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }

    ConsumableLaunchedEffect(state.exitScreenEffect) { actions.onExit(null) }
    ConsumableTextEffect(state.exitScreenWithMessageEffect) { string ->
        actions.onExit(string)
    }
    ConsumableTextEffect(state.error) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = string)
    }
    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) {
        if (state.requestLinkConfirmation) {
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

    // When we scroll up, subject header will collapse. We will not change the text alpha values until
    // the remaining height is minOffsetPxForAlphaChange, After that, we will start changing the alpha values linearly.
    val minOffsetPxForAlphaChange = with(LocalDensity.current) {
        SubjectHeaderTransform.minOffsetForAlphaChangeDp.dp.roundToPx().toFloat()
    }

    // Offset values from onPreScroll will be accumulated to decide the translationY of the subject header. This will
    // create collapsing effect for the subject header.
    val subjectHeaderTransform = remember {
        mutableStateOf(
            SubjectHeaderTransform(0f, 0f, minOffsetPxForAlphaChange)
        )
    }

    // When SubjectHeader is first time composed, we need to get the its actual height to be able to calculate yOffset
    // for collapsing effect
    val subjectHeaderSizeCallback: (Int) -> Unit = {
        val currentTransform = subjectHeaderTransform.value
        subjectHeaderTransform.value = currentTransform.copyWithUpdatedHeaderHeight(it.toFloat())
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val currentTransform = subjectHeaderTransform.value
                val newOffset = subjectHeaderTransform.value.yOffsetPx + consumed.y

                subjectHeaderTransform.value = currentTransform.copyWithUpdatedYOffset(
                    newOffset.coerceIn(-currentTransform.headerHeightPx, 0f)
                )

                // We're basically watching scroll without taking it
                return super.onPostScroll(consumed, available, source)
            }

        }
    }

    Scaffold(
        modifier = modifier
            .testTag(MessageDetailScreenTestTags.RootItem)
            .nestedScroll(nestedScrollConnection),
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
                        translationY = subjectHeaderTransform.value.yOffsetPx / 2f
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
                subjectHeaderTransform = subjectHeaderTransform.value
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
                    onForward = actions.onForwardClick
                )
                MessageDetailContent(
                    padding = innerPadding,
                    messageMetadataState = state.messageMetadataState,
                    messageBodyState = state.messageBodyState,
                    actions = messageDetailContentActions,
                    showMessageActionsFeatureFlag = state.showReplyActionsFeatureFlag
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
    messageBodyState: MessageBodyState,
    actions: MessageDetailContent.Actions,
    showMessageActionsFeatureFlag: Boolean
) {
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = remember(padding) {
        PaddingValues(
            start = padding.calculateStartPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            end = padding.calculateEndPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            top = padding.calculateTopPadding() + ProtonDimens.SmallSpacing,
            bottom = padding.calculateBottomPadding() + ProtonDimens.SmallSpacing
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm),
        contentPadding = contentPadding
    ) {
        item {
            MessageDetailHeader(
                uiModel = messageMetadataState.messageDetailHeader,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar
            )
            MailDivider()
            when (messageBodyState) {
                is MessageBodyState.Loading -> ProtonCenteredProgress()
                is MessageBodyState.Data -> MessageBody(
                    messageBodyUiModel = messageBodyState.messageBodyUiModel,
                    actions = MessageBody.Actions(
                        onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                        onShowAllAttachments = actions.onShowAllAttachmentsClicked,
                        onAttachmentClicked = actions.onAttachmentClicked,
                        loadEmbeddedImage = { _, contentId -> actions.loadEmbeddedImage(contentId) },
                        onReply = actions.onReply,
                        onReplyAll = actions.onReplyAll,
                        onForward = actions.onForward
                    ),
                    showReplyActionsFeatureFlag = showMessageActionsFeatureFlag
                )

                is MessageBodyState.Error.Data -> MessageBodyLoadingError(
                    messageBodyState = messageBodyState,
                    onReload = actions.onReload
                )

                is MessageBodyState.Error.Decryption -> {
                    ProtonErrorMessage(errorMessage = stringResource(id = R.string.decryption_error))
                    MessageBody(
                        messageBodyUiModel = messageBodyState.encryptedMessageBody,
                        actions = MessageBody.Actions(
                            onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                            onShowAllAttachments = actions.onShowAllAttachmentsClicked,
                            onAttachmentClicked = actions.onAttachmentClicked,
                            loadEmbeddedImage = { _, contentId -> actions.loadEmbeddedImage(contentId) },
                            onReply = { Timber.d("Message: Reply to message $it") },
                            onReplyAll = { Timber.d("Message: Reply All to message $it") },
                            onForward = { Timber.d("Message: Forward message $it") }
                        ),
                        showReplyActionsFeatureFlag = showMessageActionsFeatureFlag
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
        val loadEmbeddedImage: (contentId: String) -> GetEmbeddedImageResult?
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
                loadEmbeddedImage = { null }
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
        val onForward: (MessageId) -> Unit
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
