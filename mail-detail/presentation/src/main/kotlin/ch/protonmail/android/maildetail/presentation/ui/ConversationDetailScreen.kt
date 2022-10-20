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
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.viewmodel.ConversationDetailViewModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.default
import me.proton.core.util.kotlin.exhaustive
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

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
            onStarClick = { viewModel.submit(ConversationDetailAction.Star) },
            onUnStarClick = { viewModel.submit(ConversationDetailAction.UnStar) }
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val uiModel = (state.conversationState as? ConversationState.Data)?.conversationUiModel
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
        }
    ) { innerPadding ->
        when (state.conversationState) {
            is ConversationState.Data -> ConversationDetailContent(contentPadding = innerPadding)
            ConversationState.Error.NotLoggedIn -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = stringResource(id = commonString.x_error_not_logged_in)
            )
            ConversationState.Error.FailedLoadingData -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = stringResource(id = string.details_error_loading_conversation)
            )
            ConversationState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )
        }.exhaustive
    }
}

@Composable
private fun ConversationDetailContent(
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

object ConversationDetailScreen {

    const val CONVERSATION_ID_KEY = "conversation id"

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
private fun ConversationDetailScreenPreview(
    @PreviewParameter(ConversationDetailsPreviewProvider::class) state: ConversationDetailState
) {
    ProtonTheme3 {
        ProtonTheme {
            ConversationDetailScreen(state = state, actions = ConversationDetailScreen.Actions.Empty)
        }
    }
}
