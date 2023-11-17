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
import androidx.activity.compose.ReportDrawn
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreview
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreviewProvider
import ch.protonmail.android.mailmailbox.presentation.paging.mapToUiStates
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
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

    Timber.d("BottomState: ${mailboxState.bottomAppBarState}")

    val completeActions = actions.copy(
        onDisableUnreadFilter = { viewModel.submit(MailboxViewAction.DisableUnreadFilter) },
        onEnableUnreadFilter = { viewModel.submit(MailboxViewAction.EnableUnreadFilter) },
        onExitSelectionMode = { viewModel.submit(MailboxViewAction.ExitSelectionMode) },
        onOfflineWithData = { viewModel.submit(MailboxViewAction.OnOfflineWithData) },
        onErrorWithData = { viewModel.submit(MailboxViewAction.OnErrorWithData) },
        onItemClicked = { item -> viewModel.submit(MailboxViewAction.ItemClicked(item)) },
        onItemLongClicked = {
            if (mailboxState.mailboxListState.selectionModeEnabled) {
                viewModel.submit(MailboxViewAction.OnItemLongClicked(it))
            } else {
                actions.showFeatureMissingSnackbar()
            }
        },
        onAvatarClicked = {
            if (mailboxState.mailboxListState.selectionModeEnabled) {
                viewModel.submit(MailboxViewAction.OnItemAvatarClicked(it))
            } else {
                actions.showFeatureMissingSnackbar()
            }
        },
        onRefreshList = { viewModel.submit(MailboxViewAction.Refresh) },
        onRefreshListCompleted = { viewModel.submit(MailboxViewAction.RefreshCompleted) },
        markAsRead = { viewModel.submit(MailboxViewAction.MarkAsRead) },
        markAsUnread = { viewModel.submit(MailboxViewAction.MarkAsUnread) },
        trash = { viewModel.submit(MailboxViewAction.Trash) },
        delete = { viewModel.submit(MailboxViewAction.Delete) },
        deleteConfirmed = { viewModel.submit(MailboxViewAction.DeleteConfirmed) },
        deleteDialogDismissed = { viewModel.submit(MailboxViewAction.DeleteDialogDismissed) }
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
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    ConsumableTextEffect(effect = mailboxState.actionMessage) {
        snackbarHostState.showSnackbar(message = it, type = ProtonSnackbarType.NORM)
    }

    MailboxDeleteDialog(state = mailboxState.deleteDialogState, actions.deleteConfirmed, actions.deleteDialogDismissed)

    Scaffold(
        modifier = modifier.testTag(MailboxScreenTestTags.Root),
        scaffoldState = scaffoldState,
        topBar = {
            Column {
                MailboxTopAppBar(
                    state = mailboxState.topAppBarState,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = actions.openDrawerMenu,
                        onExitSelectionMode = { actions.onExitSelectionMode() },
                        onExitSearchMode = {},
                        onTitleClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                        onEnterSearchMode = { actions.showFeatureMissingSnackbar() },
                        onSearch = {},
                        onOpenComposer = { actions.navigateToComposer() }
                    )
                )

                MailboxStickyHeader(
                    modifier = Modifier,
                    state = mailboxState.unreadFilterState,
                    onFilterEnabled = actions.onEnableUnreadFilter,
                    onFilterDisabled = actions.onDisableUnreadFilter
                )
            }
        },
        bottomBar = {
            BottomActionBar(
                state = mailboxState.bottomAppBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMove = { Timber.d("mailbox onMove clicked") },
                    onLabel = { Timber.d("mailbox onLabel clicked") },
                    onTrash = actions.trash,
                    onDelete = actions.delete,
                    onArchive = { Timber.d("mailbox onArchive clicked") },
                    onSpam = { Timber.d("mailbox onSpam clicked") },
                    onViewInLightMode = { Timber.d("mailbox onViewInLightMode clicked") },
                    onViewInDarkMode = { Timber.d("mailbox onViewInDarkMode clicked") },
                    onPrint = { Timber.d("mailbox onPrint clicked") },
                    onViewHeaders = { Timber.d("mailbox onViewHeaders clicked") },
                    onViewHtml = { Timber.d("mailbox onViewHtml clicked") },
                    onReportPhishing = { Timber.d("mailbox onReportPhishing clicked") },
                    onRemind = { Timber.d("mailbox onRemind clicked") },
                    onSavePdf = { Timber.d("mailbox onSavePdf clicked") },
                    onSenderEmail = { Timber.d("mailbox onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("mailbox onSaveAttachments clicked") },
                    onMore = { Timber.d("mailbox onMore clicked") },
                    onMarkRead = actions.markAsRead,
                    onMarkUnread = actions.markAsUnread,
                    onStar = { Timber.d("mailbox onStar clicked") },
                    onUnstar = { Timber.d("mailbox onUnstar clicked") }
                )
            )
        },
        snackbarHost = { ProtonSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when (val mailboxListState = mailboxState.mailboxListState) {
            is MailboxListState.Data -> {

                // Report for benchmarking that the mailbox is visible and emails are loaded.
                ReportDrawn()

                if (mailboxListState is MailboxListState.Data.ViewMode) {
                    ConsumableLaunchedEffect(mailboxListState.scrollToMailboxTop) {
                        lazyListState.animateScrollToItem(0)
                    }

                    ConsumableLaunchedEffect(mailboxListState.openItemEffect) { itemId ->
                        actions.navigateToMailboxItem(itemId)
                    }

                    ConsumableLaunchedEffect(mailboxListState.offlineEffect) {
                        actions.showOfflineSnackbar()
                    }

                    ConsumableLaunchedEffect(mailboxListState.refreshErrorEffect) {
                        actions.showRefreshErrorSnackbar()
                    }
                }

                MailboxSwipeRefresh(
                    modifier = Modifier.padding(paddingValues),
                    items = mailboxListItems,
                    state = mailboxListState,
                    listState = lazyListState,
                    viewState = mailboxListState,
                    actions = actions
                )
            }

            is MailboxListState.Loading -> ProtonCenteredProgress(
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

@SuppressWarnings("ComplexMethod")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MailboxSwipeRefresh(
    state: MailboxListState,
    items: LazyPagingItems<MailboxItemUiModel>,
    viewState: MailboxListState.Data,
    listState: LazyListState,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier
) {
    // We need to show the Pull To Refresh indicator at top at correct times, which are first time we fetch data from
    // remote and when the user pulls to refresh. We will use following flags to know when to show the indicator.
    var loadingWithDataCount by remember { mutableStateOf(0) }
    val refreshRequested = (state as? MailboxListState.Data.ViewMode)?.refreshRequested ?: false
    val currentViewState = remember(items.loadState, refreshRequested) { items.mapToUiStates(refreshRequested) }

    val refreshing = currentViewState is MailboxScreenState.LoadingWithData

    LaunchedEffect(refreshing) {
        // first time refreshing
        if (refreshing) {
            loadingWithDataCount++
        }

        // We need to clear the refreshRequestedState after the refresh is done
        if (refreshRequested && !refreshing) {
            actions.onRefreshListCompleted()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing && (refreshRequested || loadingWithDataCount == 1),
        onRefresh = {
            actions.onRefreshList()
            items.refresh()
        }
    )

    Box(
        modifier = modifier.pullRefresh(
            state = pullRefreshState,
            enabled = viewState is MailboxListState.Data.ViewMode
        )
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

            is MailboxScreenState.Empty -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_is_empty_message)
            )

            is MailboxScreenState.OfflineWithData -> {
                actions.onOfflineWithData()
                MailboxItemsList(state, listState, currentViewState, items, actions)
            }

            is MailboxScreenState.ErrorWithData -> {
                actions.onErrorWithData()
                MailboxItemsList(state, listState, currentViewState, items, actions)
            }

            is MailboxScreenState.LoadingWithData,
            is MailboxScreenState.AppendLoading,
            is MailboxScreenState.AppendError,
            is MailboxScreenState.AppendOfflineError,
            is MailboxScreenState.Data -> MailboxItemsList(state, listState, currentViewState, items, actions)
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MailboxItemsList(
    state: MailboxListState,
    listState: LazyListState,
    viewState: MailboxScreenState,
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions
) {
    val itemActions = ComposeMailboxItem.Actions(
        onItemClicked = actions.onItemClicked,
        onItemLongClicked = actions.onItemLongClicked,
        onAvatarClicked = actions.onAvatarClicked
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .testTag(MailboxScreenTestTags.List)
            .fillMaxSize()
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.id },
            contentType = items.itemContentType { MailboxItemUiModel::class }
        ) { index ->
            items[index]?.let { item ->
                MailboxItem(
                    modifier = Modifier
                        .testTag("${MailboxItemTestTags.ItemRow}$index")
                        .animateItemPlacement(),
                    item = item,
                    actions = itemActions,
                    selectionMode = state is MailboxListState.Data.SelectionMode,
                    // See doc 0014
                    isSelected = when (state) {
                        is MailboxListState.Data.SelectionMode -> state.selectedMailboxItems.any { it.id == item.id }
                        else -> false
                    }
                )
                Divider(color = ProtonTheme.colors.separatorNorm, thickness = MailDimens.SeparatorHeight)
            }
        }
        item {

            when (viewState) {
                is MailboxScreenState.AppendLoading -> ProtonCenteredProgress(
                    modifier = Modifier
                        .testTag(MailboxScreenTestTags.MailboxAppendLoader)
                        .padding(ProtonDimens.DefaultSpacing)
                )

                is MailboxScreenState.AppendOfflineError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_offline),
                    onClick = { items.retry() }
                )

                is MailboxScreenState.AppendError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_generic),
                    onClick = { items.retry() }
                )
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
    Column(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MailboxAppendError)
            .fillMaxWidth()
            .padding(
                top = ProtonDimens.DefaultSpacing,
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxAppendErrorText),
            text = message,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onClick,
            modifier = Modifier
                .testTag(MailboxScreenTestTags.MailboxAppendErrorButton)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = commonString.retry))
        }
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
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxErrorMessage),
            text = errorMessage,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MailboxDeleteDialog(
    state: DeleteDialogState,
    confirm: () -> Unit,
    dismiss: () -> Unit
) {
    if (state is DeleteDialogState.Shown) {
        ProtonAlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                ProtonAlertDialogButton(R.string.mailbox_action_delete_dialog_button_delete) {
                    confirm()
                }
            },
            dismissButton = {
                ProtonAlertDialogButton(R.string.mailbox_action_delete_dialog_button_cancel) {
                    dismiss()
                }
            },
            title = state.title.string(),
            text = { ProtonAlertDialogText(state.message.string()) }
        )
    }
}

object MailboxScreen {

    data class Actions(
        val navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
        val navigateToComposer: () -> Unit,
        val onDisableUnreadFilter: () -> Unit,
        val onEnableUnreadFilter: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onItemClicked: (MailboxItemUiModel) -> Unit,
        val onItemLongClicked: (MailboxItemUiModel) -> Unit,
        val onAvatarClicked: (MailboxItemUiModel) -> Unit,
        val onRefreshList: () -> Unit,
        val onRefreshListCompleted: () -> Unit,
        val openDrawerMenu: () -> Unit,
        val showOfflineSnackbar: () -> Unit,
        val showRefreshErrorSnackbar: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val onOfflineWithData: () -> Unit,
        val onErrorWithData: () -> Unit,
        val markAsRead: () -> Unit,
        val markAsUnread: () -> Unit,
        val trash: () -> Unit,
        val delete: () -> Unit,
        val deleteConfirmed: () -> Unit,
        val deleteDialogDismissed: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                navigateToMailboxItem = {},
                navigateToComposer = {},
                onDisableUnreadFilter = {},
                onEnableUnreadFilter = {},
                onExitSelectionMode = {},
                onItemClicked = {},
                onItemLongClicked = {},
                onAvatarClicked = {},
                onRefreshList = {},
                onRefreshListCompleted = {},
                openDrawerMenu = {},
                showOfflineSnackbar = {},
                showRefreshErrorSnackbar = {},
                showFeatureMissingSnackbar = {},
                onOfflineWithData = {},
                onErrorWithData = {},
                markAsRead = {},
                markAsUnread = {},
                trash = {},
                delete = {},
                deleteConfirmed = {},
                deleteDialogDismissed = {}
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun MailboxErrorPreview() {
    ProtonTheme {
        MailboxError(errorMessage = stringResource(id = R.string.mailbox_error_message_offline))
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun MailboxAppendErrorPreview() {
    ProtonTheme {
        AppendError(message = stringResource(id = R.string.mailbox_error_message_offline), onClick = {})
    }
}

object MailboxScreenTestTags {

    const val List = "MailboxList"
    const val ListProgress = "MailboxListProgress"
    const val MailboxError = "MailboxError"
    const val MailboxErrorMessage = "MailboxErrorMessage"
    const val MailboxAppendLoader = "MailboxAppendLoader"
    const val MailboxAppendError = "MailboxAppendError"
    const val MailboxAppendErrorText = "MailboxAppendErrorText"
    const val MailboxAppendErrorButton = "MailboxAppendErrorButton"
    const val Root = "MailboxScreen"
}
