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

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawn
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DrawerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.UndoableOperationSnackbar
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.AutoDeleteBanner
import ch.protonmail.android.mailcommon.presentation.ui.AutoDeleteBannerActions
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.delete.AutoDeleteDialog
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreview
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreviewProvider
import ch.protonmail.android.mailmailbox.presentation.paging.mapToUiStates
import ch.protonmail.android.mailmailbox.presentation.paging.search.mapToUiStatesInSearch
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxUpsellingEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.UpsellingBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.LabelAsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MailboxMoreActionBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MailboxUpsellingBottomSheet
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoreActionBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetContent
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.upselling.AutoDeleteUpsellingBottomSheet
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.component.protonOutlinedButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.compose.theme.headlineNorm
import me.proton.core.compose.theme.headlineSmallNorm
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun MailboxScreen(
    modifier: Modifier = Modifier,
    actions: MailboxScreen.Actions,
    drawerState: DrawerState,
    viewModel: MailboxViewModel = hiltViewModel()
) {
    val mailboxState by viewModel.state.collectAsStateWithLifecycle()
    val mailboxListItems = viewModel.items.collectAsLazyPagingItems()
    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(mailboxState.mailboxListState is MailboxListState.Data.SelectionMode) {
        viewModel.submit(MailboxViewAction.ExitSelectionMode)
    }

    BackHandler((mailboxState.mailboxListState as? MailboxListState.Data.ViewMode)?.isInInboxLabel()?.not() ?: false) {
        viewModel.submit(MailboxViewAction.NavigateToInboxLabel)
    }

    BackHandler(drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(MailboxViewAction.DismissBottomSheet)
    }

    LaunchedEffect(key1 = mailboxListItems.itemSnapshotList) {
        Timber.d("Paging items: ${mailboxListItems.itemSnapshotList.size}")
        viewModel.submit(MailboxViewAction.MailboxItemsChanged(mailboxListItems.itemSnapshotList.items.map { it.id }))
    }

    val completeActions = actions.copy(
        onDisableUnreadFilter = { viewModel.submit(MailboxViewAction.DisableUnreadFilter) },
        onEnableUnreadFilter = { viewModel.submit(MailboxViewAction.EnableUnreadFilter) },
        onExitSelectionMode = { viewModel.submit(MailboxViewAction.ExitSelectionMode) },
        onOfflineWithData = { viewModel.submit(MailboxViewAction.OnOfflineWithData) },
        onErrorWithData = { viewModel.submit(MailboxViewAction.OnErrorWithData) },
        onAvatarClicked = { viewModel.submit(MailboxViewAction.OnItemAvatarClicked(it)) },
        onItemClicked = { item -> viewModel.submit(MailboxViewAction.ItemClicked(item)) },
        onItemLongClicked = { viewModel.submit(MailboxViewAction.OnItemLongClicked(it)) },
        onRefreshList = { viewModel.submit(MailboxViewAction.Refresh) },
        onRefreshListCompleted = { viewModel.submit(MailboxViewAction.RefreshCompleted) },
        markAsRead = { viewModel.submit(MailboxViewAction.MarkAsRead) },
        markAsUnread = { viewModel.submit(MailboxViewAction.MarkAsUnread) },
        star = { viewModel.submit(MailboxViewAction.Star) },
        unstar = { viewModel.submit(MailboxViewAction.UnStar) },
        archive = { viewModel.submit(MailboxViewAction.MoveToArchive) },
        spam = { viewModel.submit(MailboxViewAction.MoveToSpam) },
        trash = { viewModel.submit(MailboxViewAction.Trash) },
        delete = { viewModel.submit(MailboxViewAction.Delete) },
        deleteConfirmed = { viewModel.submit(MailboxViewAction.DeleteConfirmed) },
        deleteDialogDismissed = { viewModel.submit(MailboxViewAction.DeleteDialogDismissed) },
        deleteAll = { viewModel.submit(MailboxViewAction.DeleteAll) },
        deleteAllConfirmed = { viewModel.submit(MailboxViewAction.DeleteAllConfirmed) },
        deleteAllDismissed = { viewModel.submit(MailboxViewAction.DeleteAllDialogDismissed) },
        onLabelAsClicked = { viewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet) },
        onMoveToClicked = { viewModel.submit(MailboxViewAction.RequestMoveToBottomSheet) },
        onMoreClicked = { viewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet) },
        onSwipeRead = { userId, itemId, isRead ->
            viewModel.submit(MailboxViewAction.SwipeReadAction(userId, itemId, isRead))
        },
        onSwipeArchive = { userId, itemId ->
            viewModel.submit(MailboxViewAction.SwipeArchiveAction(userId, itemId))
        },
        onSwipeSpam = { userId, itemId -> viewModel.submit(MailboxViewAction.SwipeSpamAction(userId, itemId)) },
        onSwipeTrash = { userId, itemId -> viewModel.submit(MailboxViewAction.SwipeTrashAction(userId, itemId)) },
        onSwipeStar = { userId, itemId, isStarred ->
            viewModel.submit(MailboxViewAction.SwipeStarAction(userId, itemId, isStarred))
        },
        onSwipeLabelAs = { userId, itemId -> viewModel.submit(MailboxViewAction.SwipeLabelAsAction(userId, itemId)) },
        onSwipeMoveTo = { userId, itemId -> viewModel.submit(MailboxViewAction.SwipeMoveToAction(userId, itemId)) },
        onEnterSearchMode = { viewModel.submit(MailboxViewAction.EnterSearchMode) },
        onSearchQuery = { query -> viewModel.submit(MailboxViewAction.SearchQuery(query)) },
        onSearchResult = { viewModel.submit(MailboxViewAction.SearchResult) },
        onExitSearchMode = { viewModel.submit(MailboxViewAction.ExitSearchMode) },
        onOpenUpsellingPage = {
            viewModel.submit(MailboxViewAction.RequestUpsellingBottomSheet(MailboxUpsellingEntryPoint.Mailbox))
        },
        onCloseUpsellingPage = { viewModel.submit(MailboxViewAction.DismissBottomSheet) },
        onShowRatingBooster = { viewModel.submit(MailboxViewAction.ShowRatingBooster(context)) },
        onAutoDeletePaidDismiss = { viewModel.submit(MailboxViewAction.DismissAutoDelete) },
        onAutoDeleteShowUpselling = {
            viewModel.submit(MailboxViewAction.RequestUpsellingBottomSheet(MailboxUpsellingEntryPoint.AutoDelete))
        },
        onAutoDeleteDialogAction = { viewModel.submit(MailboxViewAction.AutoDeleteDialogActionSubmitted(it)) },
        onAutoDeleteDialogShow = { viewModel.submit(MailboxViewAction.ShowAutoDeleteDialog) },
        openDrawerMenu = { scope.launch { drawerState.open() } },
        includeSpamTrashClicked = { viewModel.submit(MailboxViewAction.IncludeAllClicked) }
    )

    mailboxState.bottomSheetState?.let {
        // Avoids a "jumping" of the bottom sheet
        if (it.isShowEffectWithoutContent()) return@let

        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
                viewModel.submit(MailboxViewAction.DismissBottomSheet)
            }
        }
    }

    StorageLimitDialogs(
        storageLimitState = mailboxState.storageLimitState,
        actions = StorageLimitDialogs.Actions(
            dialogConfirmed = { viewModel.submit(MailboxViewAction.StorageLimitConfirmed) }
        )
    )

    val autoDeleteState = mailboxState.autoDeleteSettingState as? AutoDeleteSettingState.Data
    autoDeleteState?.let {
        AutoDeleteDialog(
            state = it.enablingDialogState,
            confirm = { viewModel.submit(MailboxViewAction.AutoDeleteDialogActionSubmitted(true)) },
            dismiss = { viewModel.submit(MailboxViewAction.AutoDeleteDialogActionSubmitted(false)) }
        )
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            when (val bottomSheetContentState = mailboxState.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = MoveToBottomSheetContent.Actions(
                        onAddFolderClick = actions.onAddFolder,
                        onFolderSelected = { viewModel.submit(MailboxViewAction.MoveToDestinationSelected(it)) },
                        onDoneClick = { _, entryPoint ->
                            viewModel.submit(MailboxViewAction.MoveToConfirmed(entryPoint))
                        },
                        onDismiss = { viewModel.submit(MailboxViewAction.DismissBottomSheet) }
                    )
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(MailboxViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { archiveSelected, entryPoint ->
                            viewModel.submit(MailboxViewAction.LabelAsConfirmed(archiveSelected, entryPoint))
                        }
                    )
                )

                is MailboxMoreActionsBottomSheetState -> MailboxMoreActionBottomSheetContent(
                    state = bottomSheetContentState,
                    actionCallbacks = MoreActionBottomSheetContent.Actions(
                        onStar = { viewModel.submit(MailboxViewAction.Star) },
                        onUnStar = { viewModel.submit(MailboxViewAction.UnStar) },
                        onArchive = { viewModel.submit(MailboxViewAction.MoveToArchive) },
                        onSpam = { viewModel.submit(MailboxViewAction.MoveToSpam) },
                        onTrash = { viewModel.submit(MailboxViewAction.Trash) },
                        onDelete = { viewModel.submit(MailboxViewAction.Delete) },
                        onLabel = { viewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet) },
                        onRead = { viewModel.submit(MailboxViewAction.MarkAsRead) },
                        onUnRead = { viewModel.submit(MailboxViewAction.MarkAsUnread) },
                        onMove = { viewModel.submit(MailboxViewAction.RequestMoveToBottomSheet) },
                        onOpenCustomizeToolbar = actions.navigateToCustomizeToolbar
                    )
                )

                is UpsellingBottomSheetState -> {
                    if (bottomSheetContentState is UpsellingBottomSheetState.Requested) {
                        when (bottomSheetContentState.entryPoint) {
                            MailboxUpsellingEntryPoint.Mailbox -> {
                                MailboxUpsellingBottomSheet(
                                    actions = UpsellingScreen.Actions.Empty.copy(
                                        onDismiss = { viewModel.submit(MailboxViewAction.DismissBottomSheet) },
                                        onUpgrade = { message -> actions.showNormalSnackbar(message) },
                                        onError = { message -> actions.showErrorSnackbar(message) }
                                    )
                                )
                            }

                            MailboxUpsellingEntryPoint.AutoDelete -> {
                                AutoDeleteUpsellingBottomSheet(
                                    actions = UpsellingScreen.Actions.Empty.copy(
                                        onDismiss = { viewModel.submit(MailboxViewAction.DismissBottomSheet) },
                                        onUpgrade = { message -> actions.showNormalSnackbar(message) },
                                        onError = { message -> actions.showErrorSnackbar(message) }
                                    )
                                )
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    ) {
        MailboxScreen(
            mailboxState = mailboxState,
            mailboxListItems = mailboxListItems,
            actions = completeActions,
            modifier = modifier.semantics { testTagsAsResourceId = true }
        )
    }
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
    val snackbarHostErrorState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR) }
    val rememberTopBarHeight = remember { mutableStateOf(0.dp) }
    val refreshErrorText = stringResource(id = R.string.mailbox_error_message_generic)

    UndoableOperationSnackbar(snackbarHostState = snackbarHostState, actionEffect = mailboxState.actionResult)

    ConsumableTextEffect(effect = mailboxState.error) {
        snackbarHostErrorState.showSnackbar(message = it, type = ProtonSnackbarType.ERROR)
    }

    ConsumableLaunchedEffect(mailboxState.showRatingBooster) {
        actions.onShowRatingBooster()
    }

    ConsumableLaunchedEffect(mailboxState.showNPSFeedback) {
        actions.navigateToNPSFeedback()
    }

    DeleteDialog(state = mailboxState.deleteDialogState, actions.deleteConfirmed, actions.deleteDialogDismissed)
    DeleteDialog(state = mailboxState.deleteAllDialogState, actions.deleteAllConfirmed, actions.deleteAllDismissed)

    Scaffold(
        modifier = modifier.testTag(MailboxScreenTestTags.Root),
        scaffoldState = scaffoldState,
        topBar = {
            val localDensity = LocalDensity.current
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    rememberTopBarHeight.value = with(localDensity) { coordinates.size.height.toDp() }
                }
            ) {
                MailboxTopAppBar(
                    state = mailboxState.topAppBarState,
                    upgradeStorageState = mailboxState.upgradeStorageState,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = actions.openDrawerMenu,
                        onExitSelectionMode = { actions.onExitSelectionMode() },
                        onExitSearchMode = { actions.onExitSearchMode() },
                        onTitleClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                        onEnterSearchMode = { actions.onEnterSearchMode() },
                        onSearch = { query -> actions.onSearchQuery(query) },
                        onOpenComposer = { actions.navigateToComposer() },
                        onNavigateToStandaloneUpselling = { actions.onNavigateToStandaloneUpselling(it) },
                        onOpenUpsellingPage = actions.onOpenUpsellingPage,
                        onCloseUpsellingPage = actions.onCloseUpsellingPage
                    )
                )

                if (mailboxState.topAppBarState !is MailboxTopAppBarState.Data.SearchMode) {
                    MailboxStickyHeader(
                        modifier = Modifier,
                        state = mailboxState.unreadFilterState,
                        onFilterEnabled = actions.onEnableUnreadFilter,
                        onFilterDisabled = actions.onDisableUnreadFilter
                    )
                }
            }
        },
        bottomBar = {
            BottomActionBar(
                state = mailboxState.bottomAppBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMove = actions.onMoveToClicked,
                    onLabel = actions.onLabelAsClicked,
                    onTrash = actions.trash,
                    onDelete = actions.delete,
                    onArchive = actions.archive,
                    onSpam = actions.spam,
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
                    onMore = actions.onMoreClicked,
                    onMarkRead = actions.markAsRead,
                    onMarkUnread = actions.markAsUnread,
                    onStar = actions.star,
                    onUnstar = actions.unstar,
                    onReply = { Timber.e("mailbox onReply clicked - unhandled") },
                    onReplyAll = { Timber.e("mailbox onReplyAll clicked - unhandled") },
                    onForward = { Timber.e("mailbox onForward clicked - unhandled") },
                    onCustomizeToolbar = actions.navigateToCustomizeToolbar
                )
            )
        },
        snackbarHost = {
            DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState)
            DismissableSnackbarHost(protonSnackbarHostState = snackbarHostErrorState)
        }
    ) { paddingValues ->
        when (val mailboxListState = mailboxState.mailboxListState) {
            is MailboxListState.Data -> {

                // Report for benchmarking that the mailbox is visible and emails are loaded.
                ReportDrawn()

                if (mailboxListState is MailboxListState.Data.ViewMode) {
                    ConsumableLaunchedEffect(mailboxListState.scrollToMailboxTop) {
                        lazyListState.animateScrollToItem(0)
                    }

                    ConsumableLaunchedEffect(mailboxListState.openItemEffect) { request ->
                        actions.navigateToMailboxItem(request)
                    }

                    ConsumableLaunchedEffect(mailboxListState.offlineEffect) {
                        actions.showOfflineSnackbar()
                    }

                    ConsumableLaunchedEffect(mailboxListState.refreshErrorEffect) {
                        actions.showErrorSnackbar(refreshErrorText)
                    }
                }

                MailboxSwipeRefresh(
                    modifier = Modifier.padding(paddingValues),
                    topBarHeight = rememberTopBarHeight.value,
                    items = mailboxListItems,
                    state = mailboxListState,
                    listState = lazyListState,
                    viewState = mailboxListState,
                    autoDeleteState = mailboxState.autoDeleteSettingState,
                    unreadFilterState = mailboxState.unreadFilterState,
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
    unreadFilterState: UnreadFilterState,
    autoDeleteState: AutoDeleteSettingState,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = 0.dp
) {
    // We need to show the Pull To Refresh indicator at top at correct times, which are first time we fetch data from
    // remote and when the user pulls to refresh. We will use following flags to know when to show the indicator.
    var loadingWithDataCount by remember { mutableIntStateOf(0) }
    val searchState = (state as? MailboxListState.Data)?.searchState
    val refreshRequested = (state as? MailboxListState.Data.ViewMode)?.refreshRequested ?: false
    val searchMode = searchState?.searchMode ?: MailboxSearchMode.None
    var lastViewState by remember { mutableStateOf<MailboxScreenState>(MailboxScreenState.Loading) }

    val currentViewState = remember(items.loadState, state) {
        if (searchMode.isInSearch()) items.mapToUiStatesInSearch(searchMode, lastViewState)
        else items.mapToUiStates(refreshRequested)
    }
    lastViewState = currentViewState

    BackHandler(
        state is MailboxListState.Data.ViewMode && searchMode.isInSearch()
    ) {
        actions.onExitSearchMode()
    }

    if (searchMode.isInSearch()) {
        LaunchedEffect(currentViewState) {
            if (searchMode == MailboxSearchMode.NewSearchLoading &&
                currentViewState !is MailboxScreenState.SearchLoading
            ) {
                actions.onSearchResult()
            }
        }
    }

    val refreshing = currentViewState is MailboxScreenState.LoadingWithData ||
        currentViewState is MailboxScreenState.SearchLoadingWithData

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
        refreshing = if (searchMode.isInSearch()) refreshing
        else refreshing && (refreshRequested || loadingWithDataCount == 1),
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

            is MailboxScreenState.SearchInputInvalidError -> SearchNoResult(
                subtitleRes = R.string.mailbox_error_search_input_invalid, showIncludeAll = false
            )

            is MailboxScreenState.UnexpectedError -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_error_message_unexpected)
            )

            is MailboxScreenState.Offline -> MailboxError(
                errorMessage = stringResource(id = R.string.mailbox_error_message_offline)
            )

            is MailboxScreenState.Empty -> MailboxEmpty(
                viewState,
                unreadFilterState,
                modifier.padding(bottom = topBarHeight)
            )

            is MailboxScreenState.OfflineWithData -> {
                actions.onOfflineWithData()
                MailboxItemsList(state, listState, currentViewState, autoDeleteState, items, actions)
            }

            is MailboxScreenState.ErrorWithData -> {
                actions.onErrorWithData()
                MailboxItemsList(state, listState, currentViewState, autoDeleteState, items, actions)
            }

            is MailboxScreenState.NewSearch -> {}

            is MailboxScreenState.SearchNoData -> SearchNoResult(
                showIncludeAll = searchState?.showIncludeSpamTrashButton ?: false,
                onIncludeAllClicked = actions.includeSpamTrashClicked
            )

            is MailboxScreenState.SearchLoading -> ProtonCenteredProgress(
                modifier = Modifier.testTag(MailboxScreenTestTags.ListProgress)
            )

            is MailboxScreenState.SearchLoadingWithData,
            is MailboxScreenState.SearchData,
            is MailboxScreenState.LoadingWithData,
            is MailboxScreenState.AppendLoading,
            is MailboxScreenState.AppendError,
            is MailboxScreenState.AppendOfflineError,
            is MailboxScreenState.Data -> MailboxItemsList(
                state,
                listState,
                currentViewState,
                autoDeleteState,
                items,
                actions
            )
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MailboxItemsList(
    state: MailboxListState,
    listState: LazyListState,
    viewState: MailboxScreenState,
    autoDeleteState: AutoDeleteSettingState,
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions
) {
    val itemActions = ComposeMailboxItem.Actions(
        onItemClicked = actions.onItemClicked,
        onItemLongClicked = actions.onItemLongClicked,
        onAvatarClicked = actions.onAvatarClicked
    )

    // Detect if user manually scrolled the list
    var shouldScrollToTop by rememberSaveable { mutableStateOf(true) }
    var userTapped by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = listState.isScrollInProgress) {
        if (shouldScrollToTop && userTapped && listState.isScrollInProgress) {
            shouldScrollToTop = false
        }
        // Update the state only when the scroll action stops.
        if (!listState.isScrollInProgress && !shouldScrollToTop) {
            if (listState.firstVisibleItemIndex == 0) {
                shouldScrollToTop = true
            }
        }
    }

    // Scroll to the top of the list to make the first item always visible until the user scrolls the list
    if (shouldScrollToTop) {
        LaunchedEffect(Unit) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    if (index > 0) {
                        listState.scrollToItem(0)
                    }
                }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .testTag(MailboxScreenTestTags.List)
            .fillMaxSize()
            .pointerInteropFilter { event ->
                if (!userTapped && event.action == android.view.MotionEvent.ACTION_DOWN) {
                    userTapped = true
                }
                false // Allow the event to propagate
            }
    ) {
        if (state is MailboxListState.Data) {
            state.clearState.let { it as? MailboxListState.Data.ClearState.Visible }?.let {
                item {
                    when (it) {
                        MailboxListState.Data.ClearState.Visible.Banner -> {
                            ClearBanner()
                        }

                        is MailboxListState.Data.ClearState.Visible.Button -> {
                            ClearButton(it, actions)
                        }
                    }
                }
            }
            state.autoDeleteBannerState.let { it as? MailboxListState.Data.AutoDeleteBannerState.Visible }?.let {
                item {
                    AutoDeleteBanner(
                        modifier = Modifier,
                        uiModel = it.uiModel,
                        actions = AutoDeleteBannerActions.Empty.copy(
                            onDismissClick = actions.onAutoDeletePaidDismiss,
                            onActionClick = actions.onAutoDeleteShowUpselling,
                            onConfirmClick = actions.onAutoDeleteDialogShow
                        )
                    )
                }
            }

            if (state.searchState.showIncludeSpamTrashButton == true) {
                item {
                    IncludeSpamTrashItem(onClick = actions.includeSpamTrashClicked)
                }
            }
        }
        items(
            count = items.itemCount,
            key = items.itemKey { it.id.plus(state.stateKey()) },
            contentType = items.itemContentType { MailboxItemUiModel::class }
        ) { index ->
            items[index]?.let { item ->
                SwipeableItem(
                    modifier = Modifier.animateItem(),
                    swipeActionsUiModel = (state as MailboxListState.Data).swipeActions,
                    swipingEnabled = state.swipingEnabled,
                    swipeActionCallbacks = generateSwipeActions(items, actions, item)
                ) {
                    MailboxItem(
                        modifier = Modifier
                            .background(ProtonTheme.colors.backgroundNorm)
                            .testTag("${MailboxItemTestTags.ItemRow}$index"),
                        item = item,
                        actions = itemActions,
                        selectionMode = state is MailboxListState.Data.SelectionMode,
                        currentMailLabelId = state.currentMailLabel,
                        autoDeleteEnabled = (autoDeleteState as? AutoDeleteSettingState.Data)?.isEnabled == true,
                        // See doc 0014
                        isSelected = when (state) {
                            is MailboxListState.Data.SelectionMode ->
                                state.selectedMailboxItems.any { it.id == item.id }

                            else -> false
                        }
                    )
                }
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

                else -> {
                    Spacer(modifier = Modifier.padding(1.dp))
                }
            }
        }
    }
}

@Composable
private fun ClearBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MinHeight)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.SmallSpacing,
                bottom = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing
            )
            .background(
                color = ProtonTheme.colors.backgroundSecondary,
                shape = ProtonTheme.shapes.medium
            )
            .padding(ProtonDimens.SmallSpacing),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            color = ProtonTheme.colors.textWeak,
            fontWeight = FontWeight.Normal,
            text = stringResource(id = R.string.mailbox_action_clear_operation_scheduled)
        )
    }
}

@Composable
private fun ClearButton(
    clearButtonState: MailboxListState.Data.ClearState.Visible.Button,
    actions: MailboxScreen.Actions
) {
    ProtonButton(
        onClick = actions.deleteAll,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MinHeight)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                top = ProtonDimens.SmallSpacing,
                bottom = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing
            ),
        shape = ProtonTheme.shapes.medium,
        border = BorderStroke(
            ButtonDefaults.OutlinedBorderSize,
            ProtonTheme.colors.notificationError
        ),
        elevation = null,
        colors = ButtonDefaults.protonOutlinedButtonColors(
            contentColor = ProtonTheme.colors.notificationError
        ),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(text = clearButtonState.text.string())
    }
}

private fun generateSwipeActions(
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions,
    item: MailboxItemUiModel
): SwipeActions.Actions {
    return SwipeActions.Actions(
        onNone = {},
        onTrash = { actions.onSwipeTrash(item.userId, item.id) },
        onSpam = { actions.onSwipeSpam(item.userId, item.id) },
        onStar = {
            items.itemSnapshotList.items.firstOrNull { it.id == item.id }?.let {
                actions.onSwipeStar(it.userId, it.id, it.showStar)
            }
        },
        onArchive = { actions.onSwipeArchive(item.userId, item.id) },
        onMarkRead = {
            items.itemSnapshotList.items.firstOrNull { it.id == item.id }?.let {
                actions.onSwipeRead(it.userId, it.id, it.isRead)
            }
        },
        onLabelAs = { actions.onSwipeLabelAs(item.userId, item.id) },
        onMoveTo = { actions.onSwipeMoveTo(item.userId, item.id) }
    )
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
private fun SearchNoResult(
    modifier: Modifier = Modifier,
    showIncludeAll: Boolean,
    @StringRes subtitleRes: Int = R.string.mailbox_search_no_results_explanation,
    onIncludeAllClicked: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ProtonDimens.LargerSpacing,
                end = ProtonDimens.LargerSpacing
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showIncludeAll) {
            IncludeSpamTrashItem(onClick = onIncludeAllClicked)
        }

        Spacer(
            modifier = Modifier
                .fillMaxHeight(fraction = 0.2f)
                .fillMaxWidth()
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    bottom = ProtonDimens.SmallSpacing
                ),
            painter = painterResource(id = R.drawable.search_no_results),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    bottom = ProtonDimens.SmallSpacing
                ),
            text = stringResource(id = R.string.mailbox_search_no_results_title),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.headlineNorm,
            color = ProtonTheme.colors.textNorm
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = subtitleRes),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.defaultStrongNorm,
            color = ProtonTheme.colors.textHint
        )
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
private fun MailboxEmpty(
    listState: MailboxListState.Data,
    unreadFilterState: UnreadFilterState,
    modifier: Modifier = Modifier
) {
    val (illustration, title, description) =
        if ((unreadFilterState as? UnreadFilterState.Data)?.isFilterEnabled == true) {
            Triple(
                R.drawable.illustration_empty_mailbox_unread,
                R.string.mailbox_is_empty_no_unread_messages_title,
                R.string.mailbox_is_empty_description
            )
        } else {
            when (listState.currentMailLabel.id) {
                MailLabelId.System.Inbox -> Triple(
                    R.drawable.illustration_empty_mailbox_no_messages,
                    R.string.mailbox_is_empty_title,
                    R.string.mailbox_is_empty_description
                )

                MailLabelId.System.Spam -> Triple(
                    R.drawable.illustration_empty_mailbox_spam,
                    R.string.mailbox_is_empty_title,
                    R.string.mailbox_is_empty_spam_description
                )

                MailLabelId.System.Trash -> Triple(
                    R.drawable.illustration_empty_mailbox_trash,
                    R.string.mailbox_is_empty_title,
                    R.string.mailbox_is_empty_trash_description
                )

                else -> Triple(
                    R.drawable.illustration_empty_mailbox_folder,
                    R.string.mailbox_is_empty_title,
                    R.string.mailbox_is_empty_folder_description
                )
            }
        }
    Column(
        modifier = modifier
            .testTag(MailboxScreenTestTags.MailboxEmptyRoot)
            .fillMaxSize()
            .scrollable(
                rememberScrollableState(consumeScrollDelta = { 0f }),
                orientation = Orientation.Vertical
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptyImage),
            painter = painterResource(id = illustration),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(modifier = Modifier.height(ProtonDimens.LargeSpacing))
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptyTitle),
            text = stringResource(id = title),
            style = ProtonTheme.typography.headlineSmallNorm,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptySubtitle),
            text = stringResource(id = description),
            style = ProtonTheme.typography.defaultSmallWeak,
            textAlign = TextAlign.Center
        )
    }
}

object MailboxScreen {

    data class Actions(
        val navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
        val navigateToComposer: () -> Unit,
        val navigateToNPSFeedback: () -> Unit,
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
        val showNormalSnackbar: (String) -> Unit,
        val showErrorSnackbar: (String) -> Unit,
        val onOfflineWithData: () -> Unit,
        val onErrorWithData: () -> Unit,
        val markAsRead: () -> Unit,
        val markAsUnread: () -> Unit,
        val trash: () -> Unit,
        val delete: () -> Unit,
        val star: () -> Unit,
        val unstar: () -> Unit,
        val archive: () -> Unit,
        val spam: () -> Unit,
        val deleteConfirmed: () -> Unit,
        val deleteDialogDismissed: () -> Unit,
        val deleteAll: () -> Unit,
        val deleteAllConfirmed: () -> Unit,
        val deleteAllDismissed: () -> Unit,
        val onLabelAsClicked: () -> Unit,
        val onMoveToClicked: () -> Unit,
        val onMoreClicked: () -> Unit,
        val onAddLabel: () -> Unit,
        val onAddFolder: () -> Unit,
        val onSwipeRead: (UserId, String, Boolean) -> Unit,
        val onSwipeArchive: (UserId, String) -> Unit,
        val onSwipeSpam: (UserId, String) -> Unit,
        val onSwipeTrash: (UserId, String) -> Unit,
        val onSwipeStar: (UserId, String, Boolean) -> Unit,
        val onSwipeLabelAs: (UserId, String) -> Unit,
        val onSwipeMoveTo: (UserId, String) -> Unit,
        val onEnterSearchMode: () -> Unit,
        val onSearchQuery: (String) -> Unit,
        val onSearchResult: () -> Unit,
        val onExitSearchMode: () -> Unit,
        val onNavigateToStandaloneUpselling: (type: UpsellingVisibility) -> Unit,
        val onOpenUpsellingPage: () -> Unit,
        val onCloseUpsellingPage: () -> Unit,
        val onShowRatingBooster: () -> Unit,
        val onAutoDeletePaidDismiss: () -> Unit,
        val onAutoDeleteShowUpselling: () -> Unit,
        val onAutoDeleteDialogShow: () -> Unit,
        val onAutoDeleteDialogAction: (Boolean) -> Unit,
        val onRequestNotificationPermission: () -> Unit,
        val navigateToCustomizeToolbar: () -> Unit,
        val includeSpamTrashClicked: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                navigateToMailboxItem = {},
                navigateToComposer = {},
                navigateToNPSFeedback = {},
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
                showNormalSnackbar = {},
                showErrorSnackbar = {},
                onOfflineWithData = {},
                onErrorWithData = {},
                markAsRead = {},
                markAsUnread = {},
                trash = {},
                delete = {},
                deleteConfirmed = {},
                deleteDialogDismissed = {},
                deleteAll = {},
                deleteAllConfirmed = {},
                deleteAllDismissed = {},
                onLabelAsClicked = {},
                onMoveToClicked = {},
                onMoreClicked = {},
                onAddLabel = {},
                onAddFolder = {},
                star = {},
                unstar = {},
                archive = {},
                spam = {},
                onSwipeRead = { _, _, _ -> },
                onSwipeArchive = { _, _ -> },
                onSwipeSpam = { _, _ -> },
                onSwipeTrash = { _, _ -> },
                onSwipeStar = { _, _, _ -> },
                onSwipeLabelAs = { _, _ -> },
                onSwipeMoveTo = { _, _ -> },
                onExitSearchMode = {},
                onEnterSearchMode = {},
                onSearchQuery = {},
                onSearchResult = {},
                onNavigateToStandaloneUpselling = {},
                onOpenUpsellingPage = {},
                onCloseUpsellingPage = {},
                onShowRatingBooster = {},
                onAutoDeletePaidDismiss = {},
                onAutoDeleteDialogAction = {},
                onAutoDeleteShowUpselling = {},
                onAutoDeleteDialogShow = {},
                onRequestNotificationPermission = {},
                navigateToCustomizeToolbar = {},
                includeSpamTrashClicked = {}
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
private fun SearchNoResultPreview() {
    ProtonTheme {
        SearchNoResult(modifier = Modifier.fillMaxSize(), showIncludeAll = true, onIncludeAllClicked = {})
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

private fun MailboxListState.stateKey(): String {
    return when (this) {
        is MailboxListState.Loading -> ""
        is MailboxListState.Data.ViewMode -> this.searchState.searchMode.name
        is MailboxListState.Data.SelectionMode -> this.searchState.searchMode.name
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
    const val MailboxEmptyRoot = "MailboxEmptyRootItem"
    const val MailboxEmptyImage = "MailboxEmptyImage"
    const val MailboxEmptyTitle = "MailboxEmptyTitle"
    const val MailboxEmptySubtitle = "MailboxEmptySubtitle"
    const val Root = "MailboxScreen"
}
