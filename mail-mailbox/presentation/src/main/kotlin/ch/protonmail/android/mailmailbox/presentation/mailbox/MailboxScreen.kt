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
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.viewbinding.BuildConfig
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreview
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreviewProvider
import ch.protonmail.android.mailpagination.presentation.paging.rememberLazyListState
import ch.protonmail.android.mailpagination.presentation.paging.verticalScrollbar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

object MailboxScreenTestTags {

    const val LIST = "MailboxList"
    const val LIST_PROGRESS = "MailboxListProgress"
    const val MAILBOX_EMPTY = "MailboxEmpty"
    const val MAILBOX_ERROR = "MailboxError"
    const val ROOT = "MailboxScreen"
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MailboxScreen(
    modifier: Modifier = Modifier,
    actions: MailboxScreen.Actions,
    viewModel: MailboxViewModel = hiltViewModel()
) {
    val mailboxState = rememberAsState(viewModel.state, MailboxViewModel.initialState).value
    val mailboxListItems = viewModel.items.collectAsLazyPagingItems()

    val completeActions = actions.copy(
        onDisableUnreadFilter = { viewModel.submit(MailboxViewAction.DisableUnreadFilter) },
        onEnableUnreadFilter = { viewModel.submit(MailboxViewAction.EnableUnreadFilter) },
        onExitSelectionMode = { viewModel.submit(MailboxViewAction.ExitSelectionMode) },
        onNavigateToMailboxItem = { item -> viewModel.submit(MailboxViewAction.OpenItemDetails(item)) },
        onOpenSelectionMode = {
            viewModel.submit(MailboxViewAction.EnterSelectionMode)
            actions.showFeatureMissingSnackbar()
        },
        onRefreshList = { viewModel.submit(MailboxViewAction.Refresh) }
    )

    MailboxScreen(
        mailboxState = mailboxState,
        mailboxListItems = mailboxListItems,
        actions = completeActions,
        modifier = modifier.semantics { testTagsAsResourceId = true }
    )

}

@Composable
fun MailboxScreen(
    mailboxState: MailboxState,
    mailboxListItems: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val lazyListState = mailboxListItems.rememberLazyListState()

    ConsumableLaunchedEffect(mailboxState.networkStatusEffect) {
        if (it == NetworkStatus.Disconnected) {
            actions.showOfflineSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.testTag(MailboxScreenTestTags.ROOT),
        scaffoldState = scaffoldState,
        topBar = {
            Column {
                MailboxTopAppBar(
                    state = mailboxState.topAppBarState,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = actions.openDrawerMenu,
                        onExitSelectionMode = actions.onExitSelectionMode,
                        onExitSearchMode = {},
                        onTitleClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                        onEnterSearchMode = {
                            actions.showFeatureMissingSnackbar()
                        },
                        onSearch = {},
                        onOpenCompose = {
                            actions.showFeatureMissingSnackbar()
                        }
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

            MailboxListState.Loading -> ProtonCenteredProgress(
                modifier = Modifier
                    .testTag(MailboxScreenTestTags.LIST_PROGRESS)
                    .padding(paddingValues)
            )
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
            .padding(horizontal = ProtonDimens.DefaultSpacing),
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

    val isError = when (items.loadState.refresh) {
        is LoadState.Error -> true
        else -> false
    }

    val isLoading = when {
        items.loadState.refresh is LoadState.Loading -> true
        items.loadState.append is LoadState.Loading -> true
        items.loadState.prepend is LoadState.Loading -> true
        else -> false
    }

    Timber.v("Is loading: $isLoading, items count: ${items.itemCount}")

    SwipeRefresh(
        modifier = modifier,
        state = rememberSwipeRefreshState(isLoading),
        onRefresh = {
            actions.onRefreshList()
            items.refresh()
        }
    ) {

        if (isError && items.itemCount == 0) {
            MailboxError(
                modifier = Modifier.scrollable(
                    rememberScrollableState(consumeScrollDelta = { 0f }),
                    orientation = Orientation.Vertical
                )
            )
        } else if (isLoading.not() && items.itemCount == 0) {
            MailboxEmpty(
                modifier = Modifier.scrollable(
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
        modifier = Modifier.testTag(MailboxScreenTestTags.LIST)
            .fillMaxSize()
            .let { if (BuildConfig.DEBUG) it.verticalScrollbar(listState) else it }
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            item?.let {
                MailboxItem(
                    modifier = Modifier.animateItemPlacement(),
                    item = item,
                    onItemClicked = actions.onNavigateToMailboxItem,
                    onOpenSelectionMode = actions.onOpenSelectionMode
                )
            }
            Divider(color = ProtonTheme.colors.separatorNorm, thickness = MailDimens.SeparatorHeight)
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

@Composable
private fun MailboxEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MAILBOX_EMPTY)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Empty mailbox")
    }
}

@Composable
private fun MailboxError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MAILBOX_ERROR)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("You are offline, unable to retrieve items")
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
        val openDrawerMenu: () -> Unit,
        val showOfflineSnackbar: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit
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
                openDrawerMenu = {},
                showOfflineSnackbar = {},
                showFeatureMissingSnackbar = {}
            )
        }
    }
}

/**
 * Note: The preview won't show mailbox items because this: https://issuetracker.google.com/issues/194544557
 *  Start preview in Interactive Mode to correctly see the mailbox items
 */
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun MailboxScreenPreview(
    @PreviewParameter(MailboxPreviewProvider::class) mailboxPreview: MailboxPreview
) {
    ProtonTheme {
        MailboxScreen(
            mailboxListItems = mailboxPreview.items.collectAsLazyPagingItems(),
            mailboxState = mailboxPreview.state,
            actions = MailboxScreen.Actions.Empty
        )
    }
}
