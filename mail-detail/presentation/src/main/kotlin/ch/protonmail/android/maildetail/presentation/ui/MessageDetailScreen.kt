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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataState
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.previewdata.MessageDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.MessageDetailViewModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.default
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@Composable
fun MessageDetailScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: MessageDetailViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = MessageDetailViewModel.initialState)
    MessageDetailScreen(
        modifier = modifier,
        state = state,
        actions = MessageDetailScreen.Actions(
            onBackClick = onBackClick,
            onStarClick = { viewModel.submit(MessageViewAction.Star) },
            onUnStarClick = { viewModel.submit(MessageViewAction.UnStar) }
        )
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MessageDetailScreen(
    state: MessageDetailState,
    actions: MessageDetailScreen.Actions,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val uiModel = (state.messageState as? MessageDetailMetadataState.Data)?.messageUiModel
            DetailScreenTopBar(
                title = uiModel?.subject ?: DetailScreenTopBar.NoTitle,
                isStarred = uiModel?.isStarred,
                messageCount = null,
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
                    onReply = { Timber.d("message onReply clicked") },
                    onReplyAll = { Timber.d("message onReplyAll clicked") },
                    onForward = { Timber.d("message onForward clicked") },
                    onMarkRead = { Timber.d("message onMarkRead clicked") },
                    onMarkUnread = { Timber.d("message onMarkUnread clicked") },
                    onStar = { Timber.d("message onStar clicked") },
                    onUnstar = { Timber.d("message onUnstar clicked") },
                    onMove = { Timber.d("message onMove clicked") },
                    onLabel = { Timber.d("message onLabel clicked") },
                    onTrash = { Timber.d("message onTrash clicked") },
                    onDelete = { Timber.d("message onDelete clicked") },
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
                    onSaveAttachments = { Timber.d("message onSaveAttachments clicked") }
                )
            )
        }
    ) { innerPadding ->
        when (state.messageState) {
            is MessageDetailMetadataState.Data -> MessageDetailContent(contentPadding = innerPadding)
            MessageDetailMetadataState.Error.NotLoggedIn -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = stringResource(id = commonString.x_error_not_logged_in)
            )
            MessageDetailMetadataState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )
        }.exhaustive
    }
}

@Composable
private fun MessageDetailContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val list = (0..75).map { it.toString() }
        items(count = list.size) {
            Text(
                text = list[it],
                style = ProtonTheme.typography.default,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

object MessageDetailScreen {

    const val MESSAGE_ID_KEY = "message id"

    data class Actions(
        val onBackClick: () -> Unit,
        val onStarClick: () -> Unit,
        val onUnStarClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onStarClick = {},
                onUnStarClick = {}
            )
        }
    }
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
