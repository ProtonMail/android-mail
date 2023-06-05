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
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import androidx.viewbinding.BuildConfig
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreview
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreviewProvider
import ch.protonmail.android.mailmailbox.presentation.paging.mapToUiStates
import ch.protonmail.android.mailpagination.presentation.paging.rememberLazyListState
import ch.protonmail.android.mailpagination.presentation.paging.verticalScrollbar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import timber.log.Timber
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

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
        modifier = modifier.testTag(MailboxScreenTestTags.Root),
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
                        onOpenComposer = {
                            if (mailboxState.topAppBarState.isComposerDisabled) {
                                actions.showFeatureMissingSnackbar()
                            } else {
                                actions.navigateToComposer()
                            }
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
                    .testTag(MailboxScreenTestTags.ListProgress)
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

    val currentViewState = items.mapToUiStates()
    Timber.d("view state = $currentViewState")

    SwipeRefresh(
        modifier = modifier,
        state = rememberSwipeRefreshState(currentViewState is MailboxScreenState.LoadingWithData),
        onRefresh = {
            actions.onRefreshList()
            items.refresh()
        }
    ) {

        when (currentViewState) {
            is MailboxScreenState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.testTag(MailboxScreenTestTags.ListProgress)
            )
            is MailboxScreenState.Error -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_error_message_generic)
            )
            is MailboxScreenState.UnexpectedError -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_error_message_unexpected)
            )
            is MailboxScreenState.Offline -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_error_message_offline)
            )
            is MailboxScreenState.Empty -> MailboxEmpty()
            is MailboxScreenState.OfflineWithData,
            is MailboxScreenState.ErrorWithData,
            is MailboxScreenState.LoadingWithData,
            is MailboxScreenState.AppendLoading,
            is MailboxScreenState.AppendError,
            is MailboxScreenState.AppendOfflineError,
            is MailboxScreenState.Data -> MailboxItemsList(listState, currentViewState, items, actions)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MailboxItemsList(
    listState: LazyListState,
    viewState: MailboxScreenState,
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.testTag(MailboxScreenTestTags.List)
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
            when (viewState) {
                is MailboxScreenState.AppendLoading -> ProtonCenteredProgress(Modifier.fillMaxWidth())
                is MailboxScreenState.AppendOfflineError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_offline),
                    onClick = { items.retry() }
                )
                is MailboxScreenState.AppendError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_generic),
                    onClick = { items.retry() }
                )
                is MailboxScreenState.OfflineWithData -> actions.showOfflineSnackbar()
                is MailboxScreenState.ErrorWithData -> actions.showRefreshErrorSnackbar()
                else -> Unit
            }
        }
    }
}

@Composable
private fun AppendError(
    modifier: Modifier = Modifier,
    message: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MailboxScreenTestTags.MailboxAppendError),
        contentAlignment = Alignment.Center
    ) {
        Text(message)
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = commonString.retry))
        }
    }
}

@Composable
private fun MailboxEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MailboxEmpty)
            .fillMaxSize()
            .scrollable(
                rememberScrollableState(consumeScrollDelta = { 0f }),
                orientation = Orientation.Vertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("Empty mailbox")
    }
}

@Composable
private fun MailboxError(modifier: Modifier = Modifier, errorMessage: String) {
    Box(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MailboxError)
            .fillMaxSize()
            .scrollable(
                rememberScrollableState(consumeScrollDelta = { 0f }),
                orientation = Orientation.Vertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(errorMessage)
    }
}

object MailboxScreen {

    data class Actions(
        val navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
        val navigateToComposer: () -> Unit,
        val onDisableUnreadFilter: () -> Unit,
        val onEnableUnreadFilter: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onNavigateToMailboxItem: (MailboxItemUiModel) -> Unit,
        val onOpenSelectionMode: () -> Unit,
        val onRefreshList: () -> Unit,
        val openDrawerMenu: () -> Unit,
        val showOfflineSnackbar: () -> Unit,
        val showRefreshErrorSnackbar: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                navigateToMailboxItem = {},
                navigateToComposer = {},
                onDisableUnreadFilter = {},
                onEnableUnreadFilter = {},
                onExitSelectionMode = {},
                onNavigateToMailboxItem = {},
                onOpenSelectionMode = {},
                onRefreshList = {},
                openDrawerMenu = {},
                showOfflineSnackbar = {},
                showRefreshErrorSnackbar = {},
                showFeatureMissingSnackbar = {}
            )
        }
    }
}

/**
 * Note: The preview won't show mailbox items because this: https://issuetracker.google.com/issues/194544557
 *  Start preview in Interactive Mode to correctly see the mailbox items
 */
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MailboxScreenPreview(@PreviewParameter(MailboxPreviewProvider::class) mailboxPreview: MailboxPreview) {
    ProtonTheme {
        MailboxScreen(
            mailboxListItems = mailboxPreview.items.collectAsLazyPagingItems(),
            mailboxState = mailboxPreview.state,
            actions = MailboxScreen.Actions.Empty
        )
    }
}

object MailboxScreenTestTags {

    const val List = "MailboxList"
    const val ListProgress = "MailboxListProgress"
    const val MailboxEmpty = "MailboxEmpty"
    const val MailboxError = "MailboxError"
    const val MailboxAppendError = "MailboxAppendError"
    const val Root = "MailboxScreen"
}
