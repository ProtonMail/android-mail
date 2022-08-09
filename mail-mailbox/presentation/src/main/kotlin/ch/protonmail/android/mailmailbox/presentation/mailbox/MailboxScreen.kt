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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.viewbinding.BuildConfig
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.UnreadItemsFilter
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailpagination.presentation.paging.rememberLazyListState
import ch.protonmail.android.mailpagination.presentation.paging.verticalScrollbar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

const val TEST_TAG_MAILBOX_SCREEN = "MailboxScreenTestTag"

@Composable
fun MailboxScreen(
    modifier: Modifier = Modifier,
    navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
    openDrawerMenu: () -> Unit,
    viewModel: MailboxViewModel = hiltViewModel(),
) {
    val mailboxState = rememberAsState(viewModel.state, MailboxViewModel.initialState).value
    val mailboxListItems = viewModel.items.collectAsLazyPagingItems()

    val actions = MailboxScreen.Actions(
        navigateToMailboxItem = navigateToMailboxItem,
        onDisableUnreadFilter = { viewModel.submit(MailboxViewModel.Action.DisableUnreadFilter) },
        onEnableUnreadFilter = { viewModel.submit(MailboxViewModel.Action.EnableUnreadFilter) },
        onExitSelectionMode = { viewModel.submit(MailboxViewModel.Action.ExitSelectionMode) },
        onNavigateToMailboxItem = { item -> viewModel.submit(MailboxViewModel.Action.OpenItemDetails(item)) },
        onOpenSelectionMode = { viewModel.submit(MailboxViewModel.Action.EnterSelectionMode) },
        onRefreshList = { viewModel.submit(MailboxViewModel.Action.Refresh) },
        openDrawerMenu = openDrawerMenu
    )

    MailboxScreen(
        mailboxState = mailboxState,
        mailboxListItems = mailboxListItems,
        actions = actions,
        modifier = modifier
    )

}

@Composable
fun MailboxScreen(
    mailboxState: MailboxState,
    mailboxListItems: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val lazyListState = mailboxListItems.rememberLazyListState()

    Scaffold(
        modifier = modifier.testTag(TEST_TAG_MAILBOX_SCREEN),
        topBar = {
            Column {
                MailboxTopAppBar(
                    state = mailboxState.topAppBarState,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = actions.openDrawerMenu,
                        onExitSelectionMode = actions.onExitSelectionMode,
                        onExitSearchMode = {},
                        onTitleClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                        onEnterSearchMode = {},
                        onSearch = {},
                        onOpenCompose = {}
                    )
                )

                MailboxStickyHeader(
                    modifier = Modifier,
                    state = mailboxState.unreadFilterState,
                    onFilterEnabled = actions.onEnableUnreadFilter,
                    onFilterDisabled = actions.onDisableUnreadFilter
                )
            }
        }
    ) { paddingValues ->
        when (val mailboxListState = mailboxState.mailboxListState) {
            is MailboxListState.Data -> {

                ConsumableLaunchedEffect(mailboxListState.scrollToMailboxTop) {
                    lazyListState.animateScrollToItem(0)
                }

                ConsumableLaunchedEffect(mailboxListState.openItemEffect) { itemId ->
                    actions.navigateToMailboxItem(itemId)
                }

                MailboxSwipeRefresh(
                    modifier = Modifier.padding(paddingValues),
                    items = mailboxListItems,
                    listState = lazyListState,
                    actions = actions
                )
            }
            MailboxListState.Loading -> ProtonCenteredProgress(Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun MailboxStickyHeader(
    modifier: Modifier = Modifier,
    state: UnreadFilterState,
    onFilterEnabled: () -> Unit,
    onFilterDisabled: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ProtonDimens.SmallSpacing),
        horizontalArrangement = Arrangement.End
    ) {
        UnreadItemsFilter(
            modifier = Modifier,
            state = state,
            onFilterEnabled = onFilterEnabled,
            onFilterDisabled = onFilterDisabled
        )
    }
}

@Composable
private fun MailboxSwipeRefresh(
    items: LazyPagingItems<MailboxItemUiModel>,
    listState: LazyListState,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier
) {

    val isRefreshing = when {
        items.loadState.refresh is LoadState.Loading -> true
        items.loadState.append is LoadState.Loading -> true
        items.loadState.prepend is LoadState.Loading -> true
        else -> false
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { actions.onRefreshList(); items.refresh() },
        modifier = modifier,
    ) {

        if (isRefreshing.not() && items.itemCount == 0) {
            MailboxEmpty(
                modifier = modifier.scrollable(
                    rememberScrollableState(consumeScrollDelta = { 0f }),
                    orientation = Orientation.Vertical
                )
            )
        } else {
            MailboxItemsList(listState, items, actions)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MailboxItemsList(
    listState: LazyListState,
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .let { if (BuildConfig.DEBUG) it.verticalScrollbar(listState) else it }
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            item?.let {
                MailboxItem(
                    item = item,
                    modifier = Modifier.animateItemPlacement(),
                    onItemClicked = actions.onNavigateToMailboxItem,
                    onOpenSelectionMode = actions.onOpenSelectionMode
                )
            }
        }
        item {
            when (items.loadState.append) {
                is LoadState.NotLoading -> Unit
                is LoadState.Loading -> ProtonCenteredProgress(Modifier.fillMaxWidth())
                is LoadState.Error -> Button(
                    onClick = { items.retry() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = commonString.retry))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MailboxItem(
    item: MailboxItemUiModel,
    modifier: Modifier = Modifier,
    onItemClicked: (MailboxItemUiModel) -> Unit,
    onOpenSelectionMode: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .combinedClickable(onClick = { onItemClicked(item) }, onLongClick = onOpenSelectionMode)
    ) {
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                val fontWeight = if (item.read) FontWeight.Normal else FontWeight.Bold
                Text(
                    text = "UserId: ${item.userId}",
                    fontWeight = fontWeight,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = "Participants: ${item.participants}",
                    fontWeight = fontWeight,
                    maxLines = 1
                )
                Text(
                    text = "Subject: ${item.subject}",
                    fontWeight = fontWeight,
                    maxLines = 1
                )
                Text(
                    text = when (item.type) {
                        MailboxItemType.Message -> "Message "
                        MailboxItemType.Conversation -> "Conversation "
                    } + "Labels: ${item.labels.map { it.name }}",
                    fontWeight = fontWeight,
                    maxLines = 1
                )
                Text(text = "Time: ${item.time}", fontWeight = fontWeight)
            }
        }
    }
}

@Composable
private fun MailboxEmpty(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Empty mailbox")
    }
}

object MailboxScreen {

    data class Actions(
        val navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
        val onDisableUnreadFilter: () -> Unit,
        val onEnableUnreadFilter: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onNavigateToMailboxItem: (MailboxItemUiModel) -> Unit,
        val onOpenSelectionMode: () -> Unit,
        val onRefreshList: () -> Unit,
        val openDrawerMenu: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                navigateToMailboxItem = {},
                onDisableUnreadFilter = {},
                onEnableUnreadFilter = {},
                onExitSelectionMode = {},
                onNavigateToMailboxItem = {},
                onOpenSelectionMode = {},
                onRefreshList = {},
                openDrawerMenu = {}
            )
        }
    }
}

@Preview(
    name = "Mailbox in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Mailbox in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun PreviewMailbox() {
    val items = flowOf(
        PagingData.from(
            listOf(
                MailboxItemUiModel(
                    type = MailboxItemType.Message,
                    id = "1",
                    conversationId = ConversationId("2"),
                    userId = UserId("0"),
                    participants = emptyList(),
                    subject = "First message",
                    time = TextUiModel.TextRes(R.string.yesterday),
                    read = false,
                    shouldShowRepliedIcon = true,
                    shouldShowRepliedAllIcon = false,
                    shouldShowForwardedIcon = false
                ),
                MailboxItemUiModel(
                    type = MailboxItemType.Message,
                    id = "2",
                    conversationId = ConversationId("2"),
                    userId = UserId("0"),
                    participants = listOf(Recipient("address", "name")),
                    subject = "Second message",
                    time = TextUiModel.Text("10:42"),
                    read = true,
                    shouldShowRepliedIcon = false,
                    shouldShowRepliedAllIcon = true,
                    shouldShowForwardedIcon = true
                )
            )
        )
    ).collectAsLazyPagingItems()

    ProtonTheme {
        MailboxSwipeRefresh(
            items = items,
            listState = items.rememberLazyListState(),
            actions = MailboxScreen.Actions.Empty
        )
    }
}
