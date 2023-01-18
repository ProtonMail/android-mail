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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.maildetail.presentation.model.BottomSheetEffect
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.ConversationDetailViewModel
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
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
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    onExit: (notifyUserMessage: String?) -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = ConversationDetailViewModel.initialState)
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    state.bottomSheetState?.let {
        ConsumableLaunchedEffect(effect = it.bottomSheetEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) { onDispose { viewModel.submit(ConversationDetailViewAction.DismissBottomSheet) } }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetShape = ProtonTheme.shapes.bottomSheet,
        sheetBackgroundColor = ProtonTheme.colors.backgroundNorm,
        sheetContent = {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    onFolderSelected = { viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(it)) },
                    onDoneClick = { viewModel.submit(ConversationDetailViewAction.MoveToDestinationConfirmed(it)) }
                )
                null -> ProtonCenteredProgress()
            }
        }
    ) {
        ConversationDetailScreen(
            modifier = modifier,
            state = state,
            actions = ConversationDetailScreen.Actions(
                onExit = onExit,
                onStarClick = { viewModel.submit(ConversationDetailViewAction.Star) },
                onTrashClick = { viewModel.submit(ConversationDetailViewAction.Trash) },
                onUnStarClick = { viewModel.submit(ConversationDetailViewAction.UnStar) },
                onUnreadClick = { viewModel.submit(ConversationDetailViewAction.MarkUnread) },
                onMoveToClick = {
                    viewModel.submit(
                        ConversationDetailViewAction.RequestBottomSheet(
                            MoveToBottomSheetState.MoveToBottomSheetAction.Requested
                        )
                    )
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    state: ConversationDetailState,
    actions: ConversationDetailScreen.Actions,
    modifier: Modifier = Modifier
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
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ProtonTheme.colors.backgroundDeep,
        snackbarHost = {
            ProtonSnackbarHost(hostState = snackbarHostState)
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
                    onReply = { Timber.d("conversation onReply clicked") },
                    onReplyAll = { Timber.d("conversation onReplyAll clicked") },
                    onForward = { Timber.d("conversation onForward clicked") },
                    onMarkRead = { Timber.d("conversation onMarkRead clicked") },
                    onMarkUnread = actions.onUnreadClick,
                    onStar = { Timber.d("conversation onStar clicked") },
                    onUnstar = { Timber.d("conversation onUnstar clicked") },
                    onMove = actions.onMoveToClick,
                    onLabel = { Timber.d("conversation onLabel clicked") },
                    onTrash = actions.onTrashClick,
                    onDelete = { Timber.d("conversation onDelete clicked") },
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
                    onSaveAttachments = { Timber.d("conversation onSaveAttachments clicked") }
                )
            )
        }
    ) { innerPadding ->
        when (state.messagesState) {
            is ConversationDetailsMessagesState.Data -> MessagesContent(
                uiModels = state.messagesState.messages,
                padding = innerPadding
            )
            is ConversationDetailsMessagesState.Error -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = state.messagesState.message.string()
            )
            ConversationDetailsMessagesState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )
        }.exhaustive
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessagesContent(
    uiModels: List<ConversationDetailMessageUiModel>,
    padding: PaddingValues,
    modifier: Modifier = Modifier
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
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing)
    ) {
        items(uiModels) { uiModel ->
            when (uiModel) {
                is ConversationDetailMessageUiModel.Collapsed -> ConversationDetailCollapsedMessageHeader(
                    modifier = Modifier.animateItemPlacement(),
                    uiModel = uiModel
                )
                is ConversationDetailMessageUiModel.Expanded -> Text(text = "Expanded")
            }
        }
    }
}

object ConversationDetailScreen {

    const val ConversationIdKey = "conversation id"

    data class Actions(
        val onExit: (notifyUserMessage: String?) -> Unit,
        val onStarClick: () -> Unit,
        val onTrashClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onUnreadClick: () -> Unit,
        val onMoveToClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExit = {},
                onStarClick = {},
                onTrashClick = {},
                onUnStarClick = {},
                onUnreadClick = {},
                onMoveToClick = {}
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
            ConversationDetailScreen(state = state, actions = ConversationDetailScreen.Actions.Empty)
        }
    }
}
