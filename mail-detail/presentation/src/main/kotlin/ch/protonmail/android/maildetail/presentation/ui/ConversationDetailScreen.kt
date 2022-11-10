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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.ConversationDetailViewModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.default
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber

@Composable
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = ConversationDetailViewModel.initialState)
    ConversationDetailScreen(
        modifier = modifier,
        state = state,
        actions = ConversationDetailScreen.Actions(
            onBackClick = onBackClick,
            onStarClick = { viewModel.submit(ConversationDetailViewAction.Star) },
            onUnStarClick = { viewModel.submit(ConversationDetailViewAction.UnStar) },
            onUnreadClick = { viewModel.submit(ConversationDetailViewAction.MarkUnread) }
        )
    )
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
                    onBackClick = actions.onBackClick,
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
                    onMove = { Timber.d("conversation onMove clicked") },
                    onLabel = { Timber.d("conversation onLabel clicked") },
                    onTrash = { Timber.d("conversation onTrash clicked") },
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
                messages = state.messagesState.messages,
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
private fun MessagesContent(
    messages: List<ConversationDetailMessageUiModel>,
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
        items(messages) { message ->
            ElevatedCard(modifier = Modifier.background(ProtonTheme.colors.backgroundNorm)) {
                Row(
                    modifier = Modifier.padding(ProtonDimens.SmallSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(avatarUiModel = message.avatar)
                    Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
                    Text(
                        modifier = Modifier.weight(1f),
                        text = message.subject,
                        fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.Normal,
                        style = ProtonTheme.typography.default
                    )
                }
            }
        }
    }
}

object ConversationDetailScreen {

    const val CONVERSATION_ID_KEY = "conversation id"

    data class Actions(
        val onBackClick: () -> Unit,
        val onStarClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onUnreadClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onStarClick = {},
                onUnStarClick = {},
                onUnreadClick = {}
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
