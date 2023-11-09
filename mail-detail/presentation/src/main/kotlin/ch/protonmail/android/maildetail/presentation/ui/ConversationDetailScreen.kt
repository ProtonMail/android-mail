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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
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
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.scrollOffsetDp
import ch.protonmail.android.maildetail.presentation.viewmodel.ConversationDetailViewModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import kotlinx.collections.immutable.ImmutableList
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
import timber.log.Timber

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    actions: ConversationDetail.Actions,
    viewModel: ConversationDetailViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = ConversationDetailViewModel.initialState)
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
        DisposableEffect(Unit) { onDispose { viewModel.submit(ConversationDetailViewAction.DismissBottomSheet) } }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(ConversationDetailViewAction.DismissBottomSheet)
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    onFolderSelected = { viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(it)) },
                    onDoneClick = { viewModel.submit(ConversationDetailViewAction.MoveToDestinationConfirmed(it)) }
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { viewModel.submit(ConversationDetailViewAction.LabelAsConfirmed(it)) }
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
        ConversationDetailScreen(
            modifier = modifier,
            state = state,
            actions = ConversationDetailScreen.Actions(
                onExit = actions.onExit,
                onStarClick = { viewModel.submit(ConversationDetailViewAction.Star) },
                onTrashClick = { viewModel.submit(ConversationDetailViewAction.Trash) },
                onUnStarClick = { viewModel.submit(ConversationDetailViewAction.UnStar) },
                onUnreadClick = { viewModel.submit(ConversationDetailViewAction.MarkUnread) },
                onMoveToClick = { viewModel.submit(ConversationDetailViewAction.RequestMoveToBottomSheet) },
                onLabelAsClick = { viewModel.submit(ConversationDetailViewAction.RequestLabelAsBottomSheet) },
                onExpandMessage = { viewModel.submit(ConversationDetailViewAction.ExpandMessage(it)) },
                onCollapseMessage = { viewModel.submit(ConversationDetailViewAction.CollapseMessage(it)) },
                onMessageBodyLinkClicked = {
                    viewModel.submit(ConversationDetailViewAction.MessageBodyLinkClicked(it))
                },
                onOpenMessageBodyLink = actions.openMessageBodyLink,
                onRequestScrollTo = { viewModel.submit(ConversationDetailViewAction.RequestScrollTo(it)) },
                onShowAllAttachmentsForMessage = {
                    viewModel.submit(ConversationDetailViewAction.ShowAllAttachmentsForMessage(it))
                },
                onAttachmentClicked = { messageId, attachmentId ->
                    viewModel.submit(
                        ConversationDetailViewAction.OnAttachmentClicked(messageId, attachmentId)
                    )
                },
                openAttachment = actions.openAttachment,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                loadEmbeddedImage = { messageId, contentId -> viewModel.loadEmbeddedImage(messageId, contentId) },
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward,
                onScrollRequestCompleted = { viewModel.submit(ConversationDetailViewAction.ScrollRequestCompleted) }
            ),
            scrollToMessageId = state.scrollToMessage?.id
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ConversationDetailScreen(
    state: ConversationDetailState,
    actions: ConversationDetailScreen.Actions,
    modifier: Modifier = Modifier,
    scrollToMessageId: String?
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = ProtonSnackbarHostState()

    ConsumableLaunchedEffect(state.exitScreenEffect) { actions.onExit(null) }
    ConsumableTextEffect(state.exitScreenWithMessageEffect) { message ->
        actions.onExit(message)
    }
    ConsumableTextEffect(state.error) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = string)
    }
    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) {
        actions.onOpenMessageBodyLink(it)
    }
    ConsumableLaunchedEffect(effect = state.openAttachmentEffect) {
        actions.openAttachment(it)
    }

    if (state.conversationState is ConversationDetailMetadataState.Error) {
        val message = state.conversationState.message.string()
        LaunchedEffect(state.conversationState) {
            snackbarHostState.showSnackbar(
                type = ProtonSnackbarType.ERROR,
                message = message
            )
        }
    }

    Scaffold(
        modifier = modifier
            .testTag(ConversationDetailScreenTestTags.RootItem)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ProtonTheme.colors.backgroundDeep,
        snackbarHost = {
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostState
            )
        },
        topBar = {
            val uiModel = (state.conversationState as? ConversationDetailMetadataState.Data)?.conversationUiModel
            DetailScreenTopBar(
                title = uiModel?.subject ?: DetailScreenTopBar.NoTitle,
                isStarred = uiModel?.isStarred,
                messageCount = uiModel?.messageCount,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = { actions.onExit(null) },
                    onStarClick = actions.onStarClick,
                    onUnStarClick = actions.onUnStarClick
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomActionBar(
                state = state.bottomBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMarkRead = { Timber.d("conversation onMarkRead clicked") },
                    onMarkUnread = actions.onUnreadClick,
                    onStar = { Timber.d("conversation onStar clicked") },
                    onUnstar = { Timber.d("conversation onUnstar clicked") },
                    onMove = actions.onMoveToClick,
                    onLabel = actions.onLabelAsClick,
                    onTrash = actions.onTrashClick,
                    onDelete = actions.showFeatureMissingSnackbar,
                    onArchive = { Timber.d("conversation onArchive clicked") },
                    onSpam = { Timber.d("conversation onSpam clicked") },
                    onViewInLightMode = { Timber.d("conversation onViewInLightMode clicked") },
                    onViewInDarkMode = { Timber.d("conversation onViewInDarkMode clicked") },
                    onPrint = { Timber.d("conversation onPrint clicked") },
                    onViewHeaders = { Timber.d("conversation onViewHeaders clicked") },
                    onViewHtml = { Timber.d("conversation onViewHtml clicked") },
                    onReportPhishing = { Timber.d("conversation onReportPhishing clicked") },
                    onRemind = { Timber.d("conversation onRemind clicked") },
                    onSavePdf = { Timber.d("conversation onSavePdf clicked") },
                    onSenderEmail = { Timber.d("conversation onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("conversation onSaveAttachments clicked") },
                    onMore = { Timber.d("conversation onMore clicked") }
                )
            )
        }
    ) { innerPadding ->
        when (state.messagesState) {
            is ConversationDetailsMessagesState.Data -> {
                val conversationDetailItemActions = ConversationDetailItem.Actions(
                    onExpand = actions.onExpandMessage,
                    onCollapse = actions.onCollapseMessage,
                    onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                    onOpenMessageBodyLink = actions.onOpenMessageBodyLink,
                    onShowAllAttachmentsForMessage = actions.onShowAllAttachmentsForMessage,
                    onAttachmentClicked = actions.onAttachmentClicked,
                    showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                    loadEmbeddedImage = actions.loadEmbeddedImage,
                    onReply = actions.onReply,
                    onReplyAll = actions.onReplyAll,
                    onForward = actions.onForward,
                    onScrollRequestCompleted = actions.onScrollRequestCompleted
                )
                MessagesContent(
                    uiModels = state.messagesState.messages,
                    padding = innerPadding,
                    scrollToMessageId = scrollToMessageId,
                    actions = conversationDetailItemActions,
                    showReplyActionsFeatureFlag = state.showReplyActionsFeatureFlag
                )
            }

            is ConversationDetailsMessagesState.Error -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = state.messagesState.message.string()
            )

            is ConversationDetailsMessagesState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )

            is ConversationDetailsMessagesState.Offline -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = stringResource(id = R.string.please_go_back_online_to_load_messages)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongParameterList", "ComplexMethod")
private fun MessagesContent(
    uiModels: ImmutableList<ConversationDetailMessageUiModel>,
    padding: PaddingValues,
    scrollToMessageId: String?,
    modifier: Modifier = Modifier,
    actions: ConversationDetailItem.Actions,
    showReplyActionsFeatureFlag: Boolean
) {
    val listState = rememberLazyListState()
    var loadedItemsChanged by remember { mutableStateOf(0) }
    val loadedItemsHeight = remember { mutableStateMapOf<String, Int>() }
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = remember(padding) {
        PaddingValues(
            start = padding.calculateStartPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            end = padding.calculateEndPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            top = padding.calculateTopPadding() + ProtonDimens.SmallSpacing,
            bottom = padding.calculateBottomPadding() + ProtonDimens.SmallSpacing
        )
    }

    // Map of item heights in LazyColumn (Row index -> height)
    // We will use this map to calculate total height of first non-draft message + any draft messages below it
    val itemsHeight = remember { mutableStateMapOf<Int, Int>() }
    var initialPlaceholderHeightCalculated by remember { mutableStateOf(false) }
    var scrollCount by remember { mutableStateOf(0) }

    var scrollToIndex = remember(scrollToMessageId) {
        if (scrollToMessageId == null) return@remember -1
        else uiModels.indexOfFirst { uiModel -> uiModel.messageId.id == scrollToMessageId }
    }

    // Insert some offset to scrolling to make sure the message above will also be visible partially
    val scrollOffsetPx = scrollOffsetDp.dpToPx()

    LaunchedEffect(key1 = scrollToIndex, key2 = loadedItemsChanged, key3 = initialPlaceholderHeightCalculated) {
        if (scrollToIndex >= 0) {

            // We are having frequent state updates at the beginning which are causing recompositions and
            // animateScrollToItem to be cancelled or delayed. Therefore we use scrollToItem for
            // the first scroll action.
            if (loadedItemsChanged == 0) {

                listState.scrollToItem(scrollToIndex, scrollOffsetPx)

                // When try to perform both scrolling and expanding at the same time, the above scrollToItem
                // suspend function is paused during WebView initialization. Therefore we notify the view model
                // after the completion of the first scrolling to start expanding the message.
                if (scrollCount == 0) {
                    scrollToMessageId?.let { actions.onExpand(MessageIdUiModel(it)) }
                }

                scrollCount++

            } else {
                // Scrolled message expanded, so we can conclude that scrolling is completed
                actions.onScrollRequestCompleted()
                scrollToIndex = -1
            }
        }
    }

    // We will insert a placeholder after the last item to move it to the top when scrolled
    val lazyColumnHeight = remember { mutableStateOf(0) }
    var placeholderHeight by remember { mutableStateOf(0) }
    var userScrolled by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier
            .testTag(ConversationDetailScreenTestTags.MessagesList)
            .pointerInteropFilter { event ->
                if (!userScrolled && event.action == android.view.MotionEvent.ACTION_DOWN) {
                    userScrolled = true
                }

                false // Allow the event to propagate
            }
            .onGloballyPositioned {
                lazyColumnHeight.value = it.size.height
            },
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        state = listState
    ) {

        itemsIndexed(uiModels) { index, uiModel ->
            val isLastItem = index == uiModels.size - 1

            ConversationDetailItem(
                uiModel = uiModel,
                actions = actions,
                modifier = when (uiModel) {
                    is ConversationDetailMessageUiModel.Collapsed,
                    is ConversationDetailMessageUiModel.Expanding -> Modifier.animateItemPlacement()

                    else -> Modifier
                }.onSizeChanged {
                    itemsHeight[index] = it.height
                },
                webViewHeight = loadedItemsHeight[uiModel.messageId.id],
                onMessageBodyLoadFinished = { messageId, height ->
                    Timber.d("onMessageBodyLoadFinished: $messageId, $height. loadedItemsChanged: $loadedItemsChanged")
                    loadedItemsHeight[messageId.id] = height
                    loadedItemsChanged += 1
                },
                showReplyActionsFeatureFlag = showReplyActionsFeatureFlag
            )

            if (!userScrolled) {
                // We will insert placeholder after the last item to move it to the top when scrolled
                // Make this calculation until we get sum of all items heights before the completion of scrolling
                // We assume scroll operation is completed when the scrolled item is expanded
                if (isLastItem && scrollToIndex >= 0 && !initialPlaceholderHeightCalculated) {
                    val sumOfHeights = itemsHeight.entries.filter { it.key >= scrollToIndex }.sumOf { it.value }
                    placeholderHeight = lazyColumnHeight.value - sumOfHeights

                    // We need to check if we got all items heights, in that case we need to trigger scroll to the
                    // message again by changing initialPlaceholderHeightCalculated to true. We need this scrolling
                    // only when sum of all item heights is less than the LazyColumn height (which means we have
                    // few messages in the conversation)
                    if (itemsHeight.size == uiModels.size) {
                        initialPlaceholderHeightCalculated = true
                    }
                }
            } else {
                // After user scrolled, we need to reset the placeholder height to 0
                placeholderHeight = 0
            }

            if (isLastItem && placeholderHeight > 0) {
                Spacer(
                    modifier = Modifier
                        .height(placeholderHeight.dp)
                )
            }
        }
    }
}


object ConversationDetail {

    data class Actions(
        val onExit: (notifyUserMessage: String?) -> Unit,
        val openMessageBodyLink: (url: String) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val onAddLabel: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
    )
}

object ConversationDetailScreen {

    const val ConversationIdKey = "conversation id"
    val scrollOffsetDp: Dp = (-30).dp

    data class Actions(
        val onExit: (notifyUserMessage: String?) -> Unit,
        val onStarClick: () -> Unit,
        val onTrashClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onUnreadClick: () -> Unit,
        val onMoveToClick: () -> Unit,
        val onLabelAsClick: () -> Unit,
        val onExpandMessage: (MessageIdUiModel) -> Unit,
        val onCollapseMessage: (MessageIdUiModel) -> Unit,
        val onMessageBodyLinkClicked: (url: String) -> Unit,
        val onOpenMessageBodyLink: (url: String) -> Unit,
        val onRequestScrollTo: (MessageIdUiModel) -> Unit,
        val onScrollRequestCompleted: () -> Unit,
        val onShowAllAttachmentsForMessage: (MessageIdUiModel) -> Unit,
        val onAttachmentClicked: (MessageIdUiModel, AttachmentId) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (messageId: MessageId?, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExit = {},
                onStarClick = {},
                onTrashClick = {},
                onUnStarClick = {},
                onUnreadClick = {},
                onMoveToClick = {},
                onLabelAsClick = {},
                onExpandMessage = {},
                onCollapseMessage = {},
                onMessageBodyLinkClicked = {},
                onOpenMessageBodyLink = {},
                onRequestScrollTo = {},
                onScrollRequestCompleted = {},
                onShowAllAttachmentsForMessage = {},
                onAttachmentClicked = { _, _ -> },
                openAttachment = {},
                showFeatureMissingSnackbar = {},
                loadEmbeddedImage = { _, _ -> null },
                onReply = {},
                onReplyAll = {},
                onForward = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
private fun ConversationDetailScreenPreview(
    @PreviewParameter(ConversationDetailsPreviewProvider::class) state: ConversationDetailState
) {
    ProtonTheme3 {
        ProtonTheme {
            ConversationDetailScreen(
                state = state,
                actions = ConversationDetailScreen.Actions.Empty,
                scrollToMessageId = null
            )
        }
    }
}

object ConversationDetailScreenTestTags {

    const val RootItem = "ConversationDetailScreenRootItem"
    const val MessagesList = "ConversationDetailMessagesList"
}
