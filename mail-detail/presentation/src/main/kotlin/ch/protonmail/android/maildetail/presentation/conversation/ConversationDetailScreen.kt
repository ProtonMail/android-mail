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
package ch.protonmail.android.maildetail.presentation.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.maildetail.presentation.DetailScreenTopBar
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailAction
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationUiModel
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.util.kotlin.exhaustive

@Composable
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = ConversationDetailState.Loading
        ).value
    ) {
        is ConversationDetailState.Data -> ConversationDetailScreen(
            conversationUiModel = state.conversationUiModel,
            actions = ConversationDetailScreen.Actions(
                onBackClick = onBackClick,
                onStarClick = { viewModel.submit(ConversationDetailAction.Star) },
                onUnStarClick = { viewModel.submit(ConversationDetailAction.UnStar) }
            )
        )
        ConversationDetailState.Error.NoConversationIdProvided ->
            throw IllegalStateException("No Conversation id given")
        ConversationDetailState.Error.NotLoggedIn -> Text(
            modifier = modifier,
            text = "No user logged in"
        )
        ConversationDetailState.Loading -> ProtonCenteredProgress()
        ConversationDetailState.Error.FailedLoadingData -> Text("Failed loading conversation")
    }.exhaustive
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    conversationUiModel: ConversationUiModel,
    actions: ConversationDetailScreen.Actions
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailScreenTopBar(
                title = conversationUiModel.subject,
                isStarred = conversationUiModel.isStarred,
                messageCount = conversationUiModel.messageCount,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = actions.onBackClick,
                    onStarClick = actions.onStarClick,
                    onUnStarClick = actions.onUnStarClick
                ),
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier.background(ProtonTheme.colors.backgroundSecondary),
                contentPadding = innerPadding,
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
    )
}

object ConversationDetailScreen {

    const val CONVERSATION_ID_KEY = "conversation id"

    data class Actions(
        val onBackClick: () -> Unit,
        val onStarClick: () -> Unit,
        val onUnStarClick: () -> Unit
    )
}
