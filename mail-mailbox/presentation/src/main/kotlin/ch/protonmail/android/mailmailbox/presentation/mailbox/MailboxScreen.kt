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
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ReportDrawn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeWeak
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.presentation.IntentHelper
import ch.protonmail.android.mailattachments.presentation.model.AttachmentIdUiModel
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.ui.OpenAttachmentInput
import ch.protonmail.android.mailattachments.presentation.ui.fileOpener
import ch.protonmail.android.mailattachments.presentation.ui.fileSaver
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.SnackbarError
import ch.protonmail.android.mailcommon.presentation.SnackbarNormal
import ch.protonmail.android.mailcommon.presentation.SnackbarType
import ch.protonmail.android.mailcommon.presentation.SnackbarUndo
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.AvatarImageUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheet
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetScreen
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsItemId
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheet
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetScreen
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxEmptyUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.getHighlightText
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.hasClearableOperations
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreview
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxPreviewProvider
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxStateSampleData
import ch.protonmail.android.mailmailbox.presentation.mailbox.swipe.SwipeActions
import ch.protonmail.android.mailmailbox.presentation.mailbox.swipe.SwipeableItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.swipe.getAccessibilityActionsForTalkback
import ch.protonmail.android.mailmailbox.presentation.paging.mapToUiStates
import ch.protonmail.android.mailmailbox.presentation.paging.search.mapToUiStatesInSearch
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ManageAccountSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.SnoozeSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MailboxMoreActionBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoreActionBottomSheetContent
import ch.protonmail.android.mailsnooze.presentation.SnoozeBottomSheet
import ch.protonmail.android.mailsnooze.presentation.SnoozeBottomSheetScreen
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.uicomponents.fab.LazyFab
import ch.protonmail.android.uicomponents.fab.ProtonFabHostState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountSwitchEvent
import me.proton.android.core.accountmanager.presentation.switcher.v2.AccountsSwitcherBottomSheetScreen
import timber.log.Timber
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MailboxScreen(
    modifier: Modifier = Modifier,
    actions: MailboxScreen.Actions,
    onEvent: (AccountSwitchEvent) -> Unit,
    viewModel: MailboxViewModel = hiltViewModel(),
    fabHostState: ProtonFabHostState
) {
    val mailboxState = viewModel.state.collectAsStateWithLifecycle().value

    val mailboxListItems = viewModel.items.collectAsLazyPagingItems()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismissBottomSheet(continuation: () -> Unit = {}) {
        scope.launch { bottomSheetState.hide() }
            .invokeOnCompletion {
                if (!bottomSheetState.isVisible) {
                    showBottomSheet = false
                }
                continuation()
            }
    }

    BackHandler(mailboxState.mailboxListState is MailboxListState.Data.SelectionMode) {
        viewModel.submit(MailboxViewAction.ExitSelectionMode)
    }

    BackHandler((mailboxState.mailboxListState as? MailboxListState.Data.ViewMode)?.isInInboxLabel()?.not() ?: false) {
        viewModel.submit(MailboxViewAction.NavigateToInboxLabel)
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(MailboxViewAction.DismissBottomSheet)
    }

    LaunchedEffect(key1 = mailboxListItems.itemSnapshotList) {
        viewModel.submit(MailboxViewAction.MailboxItemsChanged(mailboxListItems.itemSnapshotList.items.map { it.id }))
    }

    (mailboxState.composerNavigationState as? MailboxComposerNavigationState.Enabled)?.navigateToCompose?.let {
        ConsumableLaunchedEffect(it) {
            actions.navigateToComposer()
        }
    }

    ConsumableLaunchedEffect(mailboxState.showRatingBooster) {
        actions.onShowRatingBooster()
    }

    val completeActions = actions.copy(
        navigateToComposer = { viewModel.submit(MailboxViewAction.NavigateToComposer) },
        onDisableUnreadFilter = { viewModel.submit(MailboxViewAction.DisableUnreadFilter) },
        onEnableUnreadFilter = { viewModel.submit(MailboxViewAction.EnableUnreadFilter) },
        onEnableSpamTrashFilter = { viewModel.submit(MailboxViewAction.EnableShowSpamTrashFilter) },
        onDisableSpamTrashFilter = { viewModel.submit(MailboxViewAction.DisableShowSpamTrashFilter) },
        onSelectAllClicked = {
            viewModel.submit(MailboxViewAction.SelectAll(mailboxListItems.itemSnapshotList.items))
        },
        onDeselectAllClicked = { viewModel.submit(MailboxViewAction.DeselectAll) },
        onExitSelectionMode = { viewModel.submit(MailboxViewAction.ExitSelectionMode) },
        onOfflineWithData = { viewModel.submit(MailboxViewAction.OnOfflineWithData) },
        onErrorWithData = { viewModel.submit(MailboxViewAction.OnErrorWithData) },
        onAccountAvatarClicked = { viewModel.submit(MailboxViewAction.RequestManageAccountsBottomSheet) },
        onAvatarClicked = { viewModel.submit(MailboxViewAction.OnItemAvatarClicked(it)) },
        onAvatarImageLoadRequested = { viewModel.submit(MailboxViewAction.OnAvatarImageLoadRequested(it)) },
        onAvatarImageLoadFailed = { viewModel.submit(MailboxViewAction.OnAvatarImageLoadFailed(it)) },
        onStarClicked = { item ->
            viewModel.submit(MailboxViewAction.StarAction(item.id, item.isStarred))
        },
        onItemClicked = { item -> viewModel.submit(MailboxViewAction.ItemClicked(item)) },
        onItemLongClicked = { viewModel.submit(MailboxViewAction.OnItemLongClicked(it)) },
        onRefreshList = { viewModel.submit(MailboxViewAction.Refresh) },
        markAsRead = { viewModel.submit(MailboxViewAction.MarkAsRead) },
        markAsUnread = { viewModel.submit(MailboxViewAction.MarkAsUnread) },
        trash = { viewModel.submit(MailboxViewAction.Trash) },
        delete = { viewModel.submit(MailboxViewAction.Delete) },
        archive = { viewModel.submit(MailboxViewAction.MoveToArchive) },
        spam = { viewModel.submit(MailboxViewAction.MoveToSpam) },
        star = { viewModel.submit(MailboxViewAction.Star) },
        unStar = { viewModel.submit(MailboxViewAction.UnStar) },
        moveToInbox = { viewModel.submit(MailboxViewAction.MoveToInbox) },
        deleteConfirmed = { viewModel.submit(MailboxViewAction.DeleteConfirmed) },
        deleteDialogDismissed = { viewModel.submit(MailboxViewAction.DeleteDialogDismissed) },
        onLabelAsClicked = { viewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet) },
        onMoveToClicked = { viewModel.submit(MailboxViewAction.RequestMoveToBottomSheet) },
        onMoreClicked = { viewModel.submit(MailboxViewAction.RequestMoreActionsBottomSheet) },
        onSwipeRead = { itemId, isRead ->
            viewModel.submit(MailboxViewAction.SwipeReadAction(itemId, isRead))
        },
        onSwipeArchive = { itemId ->
            viewModel.submit(MailboxViewAction.SwipeArchiveAction(itemId))
        },
        onSwipeSpam = { itemId -> viewModel.submit(MailboxViewAction.SwipeSpamAction(itemId)) },
        onSwipeTrash = { itemId -> viewModel.submit(MailboxViewAction.SwipeTrashAction(itemId)) },
        onSwipeStar = { itemId, isStarred ->
            viewModel.submit(MailboxViewAction.StarAction(itemId, isStarred))
        },
        onSwipeLabelAs = { itemId -> viewModel.submit(MailboxViewAction.SwipeLabelAsAction(itemId)) },
        onSwipeMoveTo = { itemId -> viewModel.submit(MailboxViewAction.SwipeMoveToAction(itemId)) },
        onEnterSearchMode = {
            actions.onEnterSearchMode()
            viewModel.submit(MailboxViewAction.EnterSearchMode)
        },
        onSearchQuery = { query -> viewModel.submit(MailboxViewAction.SearchQuery(query)) },
        onSearchResult = { viewModel.submit(MailboxViewAction.SearchResult) },
        onExitSearchMode = {
            actions.onExitSearchMode()
            viewModel.submit(MailboxViewAction.ExitSearchMode)
        },
        onAttachmentClicked = { viewModel.submit(MailboxViewAction.RequestAttachment(it)) },
        onClearAll = { viewModel.submit(MailboxViewAction.ClearAll) },
        onClearAllConfirmed = { viewModel.submit(MailboxViewAction.ClearAllConfirmed) },
        onClearAllDismissed = { viewModel.submit(MailboxViewAction.ClearAllDismissed) },
        onSnooze = { viewModel.submit(MailboxViewAction.RequestSnoozeBottomSheet) },
        validateUserSession = { viewModel.submit(MailboxViewAction.ValidateUserSession) }
    )

    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(key1 = Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.submit(MailboxViewAction.ValidateUserSession)
        }
    }

    mailboxState.bottomSheetState?.let {
        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> dismissBottomSheet()
                BottomSheetVisibilityEffect.Show -> showBottomSheet = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (bottomSheetState.currentValue != SheetValue.Hidden) {
                viewModel.submit(MailboxViewAction.DismissBottomSheet)
            }
        }
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        onDismissed = { showBottomSheet = false },
        dismissOnBack = true,
        sheetState = bottomSheetState,
        sheetContent = {
            when (val contentState = mailboxState.bottomSheetState?.contentState) {
                is MoveToBottomSheetState.Requested -> {

                    val initialData = MoveToBottomSheet.InitialData(
                        contentState.userId,
                        contentState.currentLabel,
                        contentState.itemIds,
                        entryPoint = contentState.entryPoint
                    )
                    val moveSheetActions = MoveToBottomSheet.Actions(
                        onCreateNewFolderClick = actions.onAddFolder,
                        onError = { actions.showSnackbar(SnackbarError(it)) },
                        onMoveToComplete = { label, entryPoint ->
                            val action = (entryPoint as? MoveToBottomSheetEntryPoint.Mailbox)?.let {
                                MailboxViewAction.SignalMoveToCompleted(label, it)
                            } ?: return@Actions Timber.e("Invalid entry point for MoveTo - $entryPoint")

                            viewModel.submit(action)
                        },
                        onDismiss = { viewModel.submit(MailboxViewAction.DismissBottomSheet) }
                    )

                    MoveToBottomSheetScreen(providedData = initialData, actions = moveSheetActions)
                }

                is LabelAsBottomSheetState.Requested -> {
                    val initialData = LabelAsBottomSheet.InitialData(
                        contentState.userId,
                        contentState.currentLocationLabelId,
                        contentState.itemIds,
                        entryPoint = contentState.entryPoint
                    )

                    val labelSheetActions = LabelAsBottomSheet.Actions(
                        onCreateNewLabelClick = actions.onAddLabel,
                        onError = { actions.showSnackbar(SnackbarError(it)) },
                        onLabelAsComplete = { alsoArchive, entryPoint ->
                            val action = (entryPoint as? LabelAsBottomSheetEntryPoint.Mailbox)?.let {
                                MailboxViewAction.SignalLabelAsCompleted(alsoArchive, it)
                            } ?: return@Actions Timber.e("Invalid entry point for LabelAs - $entryPoint")

                            viewModel.submit(action)
                        },
                        onDismiss = { viewModel.submit(MailboxViewAction.DismissBottomSheet) }
                    )

                    LabelAsBottomSheetScreen(providedData = initialData, actions = labelSheetActions)
                }

                is MailboxMoreActionsBottomSheetState -> MailboxMoreActionBottomSheetContent(
                    state = contentState,
                    actionCallbacks = MoreActionBottomSheetContent.Actions(
                        onStar = { viewModel.submit(MailboxViewAction.Star) },
                        onUnStar = { viewModel.submit(MailboxViewAction.UnStar) },
                        onArchive = { viewModel.submit(MailboxViewAction.MoveToArchive) },
                        onSpam = { viewModel.submit(MailboxViewAction.MoveToSpam) },
                        onLabel = { viewModel.submit(MailboxViewAction.RequestLabelAsBottomSheet) },
                        onMarkRead = { viewModel.submit(MailboxViewAction.MarkAsRead) },
                        onMarkUnread = { viewModel.submit(MailboxViewAction.MarkAsUnread) },
                        onTrash = { viewModel.submit(MailboxViewAction.Trash) },
                        onDelete = { viewModel.submit(MailboxViewAction.Delete) },
                        onInbox = { viewModel.submit(MailboxViewAction.MoveToInbox) },
                        onMoveTo = { viewModel.submit(MailboxViewAction.RequestMoveToBottomSheet) },
                        onCustomizeToolbar = {
                            showBottomSheet = false
                            actions.onCustomizeToolbar()
                        },
                        onSnooze = { viewModel.submit(MailboxViewAction.RequestSnoozeBottomSheet) }
                    )
                )

                is ManageAccountSheetState -> AccountsSwitcherBottomSheetScreen(
                    onEvent = { dismissBottomSheet { onEvent(it) } }
                )

                is SnoozeSheetState.Requested -> {
                    val initialData = SnoozeBottomSheet.InitialData(
                        contentState.userId,
                        contentState.labelId,
                        items = contentState.itemIds
                    )
                    SnoozeBottomSheetScreen(
                        initialData = initialData,
                        actions = SnoozeBottomSheet.Actions(
                            onShowSuccess = {
                                actions.showSnackbar(SnackbarNormal(it))
                                viewModel.submit(MailboxViewAction.SnoozeDismissed)
                            },
                            onShowError = {
                                actions.showSnackbar(SnackbarError(it))
                                viewModel.submit(MailboxViewAction.SnoozeDismissed)
                            },
                            onNavigateToUpsell = { type ->
                                actions.onNavigateToUpselling(UpsellingEntryPoint.Feature.Snooze, type)
                                viewModel.submit(MailboxViewAction.SnoozeDismissed)
                            }
                        )
                    )
                }

                else -> Unit
            }
        }
    ) {
        MailboxScreen(
            mailboxState = mailboxState,
            fabHostState = fabHostState,
            mailboxListItems = mailboxListItems,
            actions = completeActions,
            modifier = modifier.semantics { testTagsAsResourceId = true }
        )
    }
}

@Composable
fun MailboxScreen(
    mailboxState: MailboxState,
    fabHostState: ProtonFabHostState,
    mailboxListItems: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val rememberTopBarHeight = remember { mutableStateOf(0.dp) }
    val refreshErrorText = stringResource(id = R.string.mailbox_error_message_generic)
    val context = LocalContext.current

    val showMinimizedFab by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val stickyHeaderActions = MailboxStickyHeader.Actions(
        onUnreadFilterEnabled = actions.onEnableUnreadFilter,
        onUnreadFilterDisabled = actions.onDisableUnreadFilter,
        onSpamTrashFilterEnabled = actions.onEnableSpamTrashFilter,
        onSpamTrashFilterDisabled = actions.onDisableSpamTrashFilter,
        onSelectAllClicked = actions.onSelectAllClicked,
        onDeselectAllClicked = actions.onDeselectAllClicked
    )

    val fileSaver = fileSaver(
        onFileSaved = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )

    val openAttachment = fileOpener()

    ConsumableTextEffect(effect = mailboxState.error) {
        actions.showSnackbar(SnackbarError(it))
    }

    ConsumableLaunchedEffect(effect = mailboxState.actionResult) {
        actions.showSnackbar(SnackbarUndo(it))
    }

    DeleteDialog(state = mailboxState.deleteDialogState, actions.deleteConfirmed, actions.deleteDialogDismissed)

    DeleteDialog(state = mailboxState.clearAllDialogState, actions.onClearAllConfirmed, actions.onClearAllDismissed)

    LazyFab(fabHostState) { modifier ->
        val mailboxListState = mailboxState.mailboxListState

        if (mailboxListState is MailboxListState.Data && mailboxListState.shouldShowFab) {
            AnimatedComposeMailFab(
                modifier = modifier.windowInsetsPadding(
                    WindowInsets
                        .safeDrawing
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                ),
                showMinimized = showMinimizedFab,
                onComposeClick = actions.navigateToComposer
            )
        }
    }

    Scaffold(
        modifier = modifier.testTag(MailboxScreenTestTags.Root),
        containerColor = ProtonTheme.colors.backgroundNorm,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            val localDensity = LocalDensity.current
            var topAppBarBounds by remember { mutableStateOf<Rect?>(null) }

            Column(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        rememberTopBarHeight.value = with(localDensity) { coordinates.size.height.toDp() }
                    }
            ) {

                MailboxTopAppBar(
                    state = mailboxState.topAppBarState,
                    actions = MailboxTopAppBar.Actions(
                        onOpenMenu = actions.openDrawerMenu,
                        onExitSelectionMode = { actions.onExitSelectionMode() },
                        onExitSearchMode = { actions.onExitSearchMode() },
                        onTitleClick = { scope.launch { lazyListState.animateScrollToItem(0) } },
                        onEnterSearchMode = { actions.onEnterSearchMode() },
                        onSearch = { query -> actions.onSearchQuery(query) },
                        onAccountAvatarClicked = actions.onAccountAvatarClicked,
                        onNavigateToUpselling = actions.onNavigateToUpselling
                    )
                )

                MailboxStickyHeader(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    ),
                    state = mailboxState,
                    actions = stickyHeaderActions
                )

                val loadingBarState = (mailboxState.mailboxListState as? MailboxListState.Data)?.loadingBarState
                    ?: LoadingBarUiState.Hide
                MailboxLoadingBar(
                    state = loadingBarState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
                )
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
                    onMoveToInbox = actions.moveToInbox,
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
                    onUnstar = actions.unStar,
                    onCustomizeToolbar = { Timber.d("mailbox onCustomizeToolbar clicked") },
                    onSnooze = actions.onSnooze,
                    onActionBarVisibilityChanged = actions.onActionBarVisibilityChanged,
                    onReply = { Timber.d("mailbox onReply clicked") },
                    onReplyAll = { Timber.d("mailbox onReplyAll clicked") },
                    onForward = { Timber.d("mailbox onForward clicked") }
                )
            )
        }
    ) { paddingValues ->

        val mailboxListState = mailboxState.mailboxListState
        ReportDrawn()

        if (mailboxListState is MailboxListState.Data.ViewMode) {
            ConsumableLaunchedEffect(mailboxListState.scrollToMailboxTop) {
                lazyListState.animateScrollToItem(0)
            }

            ConsumableLaunchedEffect(mailboxListState.openItemEffect) { request ->
                actions.navigateToMailboxItem(request)
            }

            ConsumableLaunchedEffect(mailboxListState.refreshErrorEffect) {
                actions.showSnackbar(SnackbarError(refreshErrorText))
            }

            ConsumableTextEffect(mailboxListState.attachmentOpeningStarted) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }

            ConsumableLaunchedEffect(mailboxListState.displayAttachment) {
                val fileContent = FileContent(it.name, it.uri, it.mimeType)

                // Check on OpenMode here is always skipped - we don't have a download button in the preview.
                if (IntentHelper.canOpenFile(context, OpenAttachmentInput(it.uri, it.mimeType))) {
                    openAttachment(OpenAttachmentInput(it.uri, it.mimeType))
                } else {
                    fileSaver(fileContent)
                }
            }

            ConsumableTextEffect(mailboxListState.displayAttachmentError) {
                actions.showSnackbar(SnackbarError(it))
            }
        }

        MailboxSwipeRefresh(
            modifier = Modifier.padding(paddingValues),
            topBarHeight = rememberTopBarHeight.value,
            items = mailboxListItems,
            state = mailboxListState,
            listState = lazyListState,
            viewState = mailboxListState,
            unreadFilterState = mailboxState.unreadFilterState,
            actions = actions
        )
    }
}

@Composable
private fun AnimatedComposeMailFab(
    showMinimized: Boolean,
    modifier: Modifier = Modifier,
    onComposeClick: () -> Unit
) {
    AnimatedContent(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(MailDimens.MailboxFabRadius),
                clip = false,
                ambientColor = ProtonTheme.colors.shadowLifted.copy(alpha = 0.4f),
                spotColor = ProtonTheme.colors.shadowLifted.copy(alpha = 0.4f)
            ),
        targetState = showMinimized,
        transitionSpec = {
            (scaleIn(initialScale = 0.8f) + fadeIn())
                .togetherWith(scaleOut(targetScale = 0.8f) + fadeOut())
        }
    ) { minimized ->
        if (minimized) {
            IconOnlyComposeMailFab(onComposeClick)
        } else {
            ComposeMailFab(onComposeClick)
        }
    }
}

@Composable
fun ComposeMailFab(onComposeClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = { onComposeClick() },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_proton_pen_square),
                contentDescription = stringResource(id = R.string.mailbox_fab_compose_button_content_description),
                tint = ProtonTheme.colors.iconNorm
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.mailbox_fab_compose_button_title),
                color = ProtonTheme.colors.textNorm,
                style = ProtonTheme.typography.titleMedium
            )
        },
        modifier = Modifier
            .border(
                ProtonDimens.OutlinedBorderSize,
                ProtonTheme.colors.borderLight, RoundedCornerShape(MailDimens.MailboxFabRadius)
            )
            .background(
                color = ProtonTheme.colors.interactionFabNorm,
                shape = RoundedCornerShape(MailDimens.MailboxFabRadius)
            ),
        expanded = true,
        shape = RoundedCornerShape(MailDimens.MailboxFabRadius),
        containerColor = ProtonTheme.colors.interactionFabNorm,
        contentColor = ProtonTheme.colors.iconNorm,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    )
}

@Composable
fun IconOnlyComposeMailFab(onComposeClick: () -> Unit) {
    FloatingActionButton(
        onClick = { onComposeClick() },
        modifier = Modifier
            .border(
                width = 1.dp,
                color = ProtonTheme.colors.borderLight,
                shape = CircleShape
            ),
        shape = RoundedCornerShape(MailDimens.MailboxFabRadius),
        containerColor = ProtonTheme.colors.interactionFabNorm,
        contentColor = ProtonTheme.colors.iconNorm,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_pen_square),
            contentDescription = stringResource(id = R.string.mailbox_fab_compose_button_content_description),
            tint = ProtonTheme.colors.iconNorm
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressWarnings("ComplexMethod")
@Composable
private fun MailboxSwipeRefresh(
    state: MailboxListState,
    items: LazyPagingItems<MailboxItemUiModel>,
    viewState: MailboxListState,
    listState: LazyListState,
    unreadFilterState: UnreadFilterState,
    actions: MailboxScreen.Actions,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = 0.dp
) {
    // We need to show the Pull To Refresh indicator at top at correct times, which are first time we fetch data from
    // remote and when the user pulls to refresh. We will use following flags to know when to show the indicator.
    val refreshOngoing = (state as? MailboxListState.Data.ViewMode)?.refreshOngoing ?: false
    val searchMode = (state as? MailboxListState.Data)?.searchState?.searchMode ?: MailboxSearchMode.None

    var lastViewState by remember { mutableStateOf<MailboxScreenState>(MailboxScreenState.Loading) }

    val currentViewState = remember(items.loadState, state) {
        when {
            state is MailboxListState.Loading -> MailboxScreenState.Loading
            state is MailboxListState.CouldNotLoadUserSession -> MailboxScreenState.CouldNotLoadUserSession
            searchMode.isInSearch() -> items.mapToUiStatesInSearch(searchMode, lastViewState)
            else -> items.mapToUiStates(refreshOngoing)
        }
    }

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

    val pullToRefreshState = rememberPullToRefreshState()

    // Refresh action
    val onRefresh: () -> Unit = {
        actions.onRefreshList()
        items.refresh()
    }

    val listDataState = state as? MailboxListState.Data
    listDataState?.paginatorInvalidationEffect?.let { effect ->
        ConsumableLaunchedEffect(effect) { event ->
            Timber.d(
                "Paginator: UI calling items.refresh() for invalidation event id=%s",
                event.id
            )
            items.refresh()
        }
    }

    val isRefreshing = listDataState?.refreshOngoing ?: false

    MailboxPullToRefreshBox(
        modifier = modifier,
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh
    ) {
        when (currentViewState) {
            is MailboxScreenState.Loading -> MailboxSkeletonLoading(
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

            is MailboxScreenState.Empty -> MailboxEmpty(
                viewState,
                unreadFilterState,
                topBarHeight
            )

            is MailboxScreenState.OfflineWithData -> {
                actions.onOfflineWithData()
                MailboxItemsList(state, listState, currentViewState, items, actions)
            }

            is MailboxScreenState.ErrorWithData -> {
                actions.onErrorWithData()
                MailboxItemsList(state, listState, currentViewState, items, actions)
            }

            is MailboxScreenState.NewSearch -> {}

            is MailboxScreenState.SearchNoData -> SearchNoResult(topBarHeight)

            is MailboxScreenState.SearchLoading -> ProtonCenteredProgress(
                modifier = Modifier.testTag(MailboxScreenTestTags.ListProgress)
            )

            is MailboxScreenState.CouldNotLoadUserSession,
            is MailboxScreenState.SearchLoadingWithData,
            is MailboxScreenState.SearchData,
            is MailboxScreenState.LoadingWithData,
            is MailboxScreenState.AppendLoading,
            is MailboxScreenState.AppendError,
            is MailboxScreenState.AppendOfflineError,
            is MailboxScreenState.Data -> MailboxItemsList(
                state, listState, currentViewState, items, actions
            )
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
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
        onAvatarClicked = actions.onAvatarClicked,
        onAvatarImageLoadRequested = actions.onAvatarImageLoadRequested,
        onAvatarImageLoadFailed = actions.onAvatarImageLoadFailed,
        onStarClicked = actions.onStarClicked,
        onAttachmentClicked = actions.onAttachmentClicked
    )

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

    var isOpenDrawerGesture by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val edgeSwipeThreshold = with(LocalDensity.current) {
        (screenWidthDp * 0.2f).toPx() // 1/5 of screen width
    }

    val coroutineScope = rememberCoroutineScope()

    val snapshotKeys = rememberDuplicateTolerantMailboxKeys(items)

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small),
        modifier = Modifier
            .testTag(MailboxScreenTestTags.List)
            .fillMaxSize()
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isOpenDrawerGesture = event.x <= edgeSwipeThreshold

                        // We need to manually reset isOpenDrawerGesture here, as MotionEvent.ACTION_UP
                        // is not received when the corresponding MotionEvent.ACTION_DOWN returns false
                        // to pass the gesture through to the drawer.
                        if (isOpenDrawerGesture) {
                            coroutineScope.launch {
                                @Suppress("MagicNumber")
                                delay(500)
                                isOpenDrawerGesture = false
                            }
                        }
                    }
                }

                if (!userTapped && event.action == MotionEvent.ACTION_DOWN) {
                    userTapped = true
                }

                false // Allow the event to propagate
            }
    ) {
        item {
            if (state.hasClearableOperations()) {
                ClearAllOperationBanner(
                    actions = ClearAllOperationBanner.Actions(
                        onUpselling = { type ->
                            actions.onNavigateToUpselling(UpsellingEntryPoint.Feature.AutoDelete, type)
                        },
                        onClearAll = actions.onClearAll
                    )
                )
            }
        }

        items(
            count = items.itemCount,
            key = { index ->
                snapshotKeys.getOrNull(index) ?: "mailbox-placeholder-$index"
            },
            contentType = items.itemContentType { MailboxItemUiModel::class }
        ) { index ->
            items[index]?.let { item ->
                val listDataState = state as MailboxListState.Data
                val swipeActionsUiModel = listDataState.swipeActions
                val isSelectionMode = listDataState is MailboxListState.Data.SelectionMode
                val isInSearch = listDataState is MailboxListState.Data.ViewMode &&
                    listDataState.searchState.isInSearch()

                val swipingEnabled = swipeActionsUiModel?.isSwipingEnabled == true &&
                    !isSelectionMode &&
                    !isInSearch &&
                    !isOpenDrawerGesture

                val swipeActions = generateSwipeActions(items, actions, item)
                SwipeableItem(
                    modifier = Modifier.animateItem(),
                    swipeActionsUiModel = listDataState.swipeActions,
                    swipingEnabled = swipingEnabled,
                    swipeActionCallbacks = swipeActions
                ) {
                    val accessibilitySwipeActions = listDataState.swipeActions?.let {
                        getAccessibilityActionsForTalkback(
                            swipingEnabled = swipingEnabled,
                            swipeActionsUiModel = it,
                            swipeActions = swipeActions,
                            context = LocalContext.current
                        )
                    }

                    val avatarImageUiModel = (item.avatar as? AvatarUiModel.ParticipantAvatar)
                        ?.let { listDataState.avatarImagesUiModel.getStateForAddress(it.address) }
                        ?: AvatarImageUiModel.NotLoaded

                    MailboxItem(
                        modifier = Modifier
                            .background(ProtonTheme.colors.backgroundNorm)
                            .testTag("${MailboxItemTestTags.ItemRow}$index"),
                        item = item,
                        avatarImageUiModel = avatarImageUiModel,
                        actions = itemActions,
                        selectionMode = listDataState is MailboxListState.Data.SelectionMode,
                        // See doc 0014
                        isSelected = when (listDataState) {
                            is MailboxListState.Data.SelectionMode ->
                                listDataState.selectedMailboxItems.any { it.id == item.id }

                            else -> false
                        },
                        accessibilitySwipeActions = accessibilitySwipeActions.orEmpty().toImmutableList(),
                        highlightText = state.getHighlightText()
                    )
                }
            }
        }
        item {
            when (viewState) {
                is MailboxScreenState.AppendLoading -> ProtonCenteredProgress(
                    modifier = Modifier
                        .testTag(MailboxScreenTestTags.MailboxAppendLoader)
                        .padding(ProtonDimens.Spacing.Large)
                )

                is MailboxScreenState.AppendOfflineError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_offline),
                    onClick = { items.retry() }
                )

                is MailboxScreenState.AppendError -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_generic),
                    onClick = { items.retry() }
                )

                is MailboxScreenState.CouldNotLoadUserSession -> AppendError(
                    message = stringResource(id = R.string.mailbox_error_message_user_session),
                    onClick = { actions.validateUserSession() }
                )

                else -> {
                    Spacer(modifier = Modifier.padding(1.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo))
        }
    }
}

private fun generateSwipeActions(
    items: LazyPagingItems<MailboxItemUiModel>,
    actions: MailboxScreen.Actions,
    item: MailboxItemUiModel
): SwipeActions.Actions {
    return SwipeActions.Actions(
        onTrash = { actions.onSwipeTrash(item.id) },
        onSpam = { actions.onSwipeSpam(item.id) },
        onStar = {
            items.itemSnapshotList.items.firstOrNull { it.id == item.id }?.let {
                actions.onSwipeStar(it.id, it.isStarred)
            }
        },
        onArchive = { actions.onSwipeArchive(item.id) },
        onMarkRead = {
            items.itemSnapshotList.items.firstOrNull { it.id == item.id }?.let {
                actions.onSwipeRead(it.id, it.isRead)
            }
        },
        onLabelAs = { actions.onSwipeLabelAs(LabelAsItemId(item.id)) },
        onMoveTo = { actions.onSwipeMoveTo(MoveToItemId(item.id)) }
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
                top = ProtonDimens.Spacing.Large,
                start = ProtonDimens.Spacing.Large,
                end = ProtonDimens.Spacing.Large
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
private fun SearchNoResult(topBarHeight: Dp) {
    MailboxStaticContent(topBarHeight = topBarHeight) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    bottom = ProtonDimens.Spacing.Standard
                ),
            painter = painterResource(id = R.drawable.search_no_results),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(
            modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge)
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.mailbox_search_no_results_title),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.titleLargeNorm,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(
            modifier = Modifier.height(ProtonDimens.Spacing.MediumLight)
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.mailbox_search_no_results_explanation),
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.bodyLargeWeak
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
    listState: MailboxListState,
    unreadFilterState: UnreadFilterState,
    topBarHeight: Dp
) {
    val (illustration, title, description) = MailboxEmptyUiModelMapper
        .toEmptyMailboxUiModel(unreadFilterState, listState)

    MailboxStaticContent(
        modifier = Modifier
            .testTag(MailboxScreenTestTags.MailboxEmptyRoot),
        topBarHeight = topBarHeight
    ) {
        Image(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptyImage),
            painter = painterResource(id = illustration),
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptyTitle),
            text = stringResource(id = title),
            style = ProtonTheme.typography.titleLargeNorm,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))
        Text(
            modifier = Modifier.testTag(MailboxScreenTestTags.MailboxEmptySubtitle),
            text = stringResource(id = description),
            style = ProtonTheme.typography.bodyLargeWeak,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MailboxStaticContent(
    topBarHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current

    val bottomPadding by remember {
        derivedStateOf {
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else topBarHeight
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wrap content in Spacers with 1f weight to keep it centered (while allowing PTR properly)
            Spacer(modifier = Modifier.weight(1f))
            content()
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

object MailboxScreen {

    data class Actions(
        val showMissingFeature: () -> Unit,
        val navigateToMailboxItem: (OpenMailboxItemRequest) -> Unit,
        val navigateToComposer: () -> Unit,
        val onDisableUnreadFilter: () -> Unit,
        val onEnableUnreadFilter: () -> Unit,
        val onEnableSpamTrashFilter: () -> Unit,
        val onDisableSpamTrashFilter: () -> Unit,
        val onSelectAllClicked: () -> Unit,
        val onDeselectAllClicked: () -> Unit,
        val onExitSelectionMode: () -> Unit,
        val onItemClicked: (MailboxItemUiModel) -> Unit,
        val onItemLongClicked: (MailboxItemUiModel) -> Unit,
        val onAvatarClicked: (MailboxItemUiModel) -> Unit,
        val onAvatarImageLoadRequested: (MailboxItemUiModel) -> Unit,
        val onAvatarImageLoadFailed: (MailboxItemUiModel) -> Unit,
        val onAttachmentClicked: (AttachmentIdUiModel) -> Unit,
        val onAttachmentReady: (OpenAttachmentIntentValues) -> Unit,
        val onStarClicked: (MailboxItemUiModel) -> Unit,
        val onRefreshList: () -> Unit,
        val openDrawerMenu: () -> Unit,
        val showSnackbar: (SnackbarType) -> Unit,
        val onOfflineWithData: () -> Unit,
        val onErrorWithData: () -> Unit,
        val markAsRead: () -> Unit,
        val markAsUnread: () -> Unit,
        val trash: () -> Unit,
        val delete: () -> Unit,
        val archive: () -> Unit,
        val spam: () -> Unit,
        val star: () -> Unit,
        val unStar: () -> Unit,
        val moveToInbox: () -> Unit,
        val deleteConfirmed: () -> Unit,
        val deleteDialogDismissed: () -> Unit,
        val onLabelAsClicked: () -> Unit,
        val onMoveToClicked: () -> Unit,
        val onMoreClicked: () -> Unit,
        val onAddLabel: () -> Unit,
        val onAddFolder: () -> Unit,
        val onSwipeRead: (String, Boolean) -> Unit,
        val onSwipeArchive: (String) -> Unit,
        val onSwipeSpam: (String) -> Unit,
        val onSwipeTrash: (String) -> Unit,
        val onSwipeStar: (String, Boolean) -> Unit,
        val onSwipeLabelAs: (LabelAsItemId) -> Unit,
        val onSwipeMoveTo: (MoveToItemId) -> Unit,
        val onEnterSearchMode: () -> Unit,
        val onSearchQuery: (String) -> Unit,
        val onSearchResult: () -> Unit,
        val onExitSearchMode: () -> Unit,
        val onAccountAvatarClicked: () -> Unit,
        val onNavigateToUpselling: (entryPoint: UpsellingEntryPoint.Feature, type: UpsellingVisibility) -> Unit,
        val onClearAll: () -> Unit,
        val onClearAllConfirmed: () -> Unit,
        val onClearAllDismissed: () -> Unit,
        val onSnooze: () -> Unit,
        val onActionBarVisibilityChanged: (Boolean) -> Unit,
        val onCustomizeToolbar: () -> Unit,
        val validateUserSession: () -> Unit,
        val onShowRatingBooster: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                navigateToMailboxItem = {},
                navigateToComposer = {},
                onDisableUnreadFilter = {},
                onEnableUnreadFilter = {},
                onEnableSpamTrashFilter = {},
                onDisableSpamTrashFilter = {},
                onSelectAllClicked = {},
                onDeselectAllClicked = {},
                onExitSelectionMode = {},
                onItemClicked = {},
                onItemLongClicked = {},
                onAvatarClicked = {},
                onAvatarImageLoadRequested = {},
                onAvatarImageLoadFailed = {},
                onStarClicked = {},
                onRefreshList = {},
                openDrawerMenu = {},
                showSnackbar = {},
                onOfflineWithData = {},
                onErrorWithData = {},
                markAsRead = {},
                markAsUnread = {},
                trash = {},
                delete = {},
                archive = {},
                spam = {},
                star = {},
                unStar = {},
                moveToInbox = {},
                deleteConfirmed = {},
                deleteDialogDismissed = {},
                onLabelAsClicked = {},
                onMoveToClicked = {},
                onMoreClicked = {},
                onAddLabel = {},
                onAddFolder = {},
                onSwipeRead = { _, _ -> },
                onSwipeArchive = { _ -> },
                onSwipeSpam = { _ -> },
                onSwipeTrash = { _ -> },
                onSwipeStar = { _, _ -> },
                onSwipeLabelAs = { _ -> },
                onSwipeMoveTo = { _ -> },
                onExitSearchMode = {},
                onEnterSearchMode = {},
                onSearchQuery = {},
                onSearchResult = {},
                onAccountAvatarClicked = {},
                showMissingFeature = {},
                onAttachmentClicked = {},
                onAttachmentReady = {},
                onClearAll = {},
                onClearAllConfirmed = {},
                onClearAllDismissed = {},
                onNavigateToUpselling = { _, _ -> },
                onSnooze = {},
                onCustomizeToolbar = {},
                onActionBarVisibilityChanged = {},
                validateUserSession = {},
                onShowRatingBooster = {}
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
            fabHostState = ProtonFabHostState(),
            actions = MailboxScreen.Actions.Empty
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun MailboxEmptyTrashPreview() {
    ProtonTheme {
        MailboxEmpty(
            listState = MailboxStateSampleData.Trash.mailboxListState as MailboxListState.Data,
            unreadFilterState = UnreadFilterState.Data(CappedNumberUiModel.Zero, false),
            topBarHeight = 0.dp
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun MailboxUnreadFilterEmptyPreview() {
    ProtonTheme {
        MailboxEmpty(
            listState = MailboxStateSampleData.Inbox.mailboxListState as MailboxListState.Data,
            unreadFilterState = UnreadFilterState.Data(CappedNumberUiModel.Zero, true),
            topBarHeight = 0.dp
        )
    }
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@AdaptivePreviews
@Composable
private fun MailboxEmptyPreview() {
    ProtonTheme {
        MailboxEmpty(
            listState = MailboxStateSampleData.Inbox.mailboxListState as MailboxListState.Data,
            unreadFilterState = UnreadFilterState.Data(CappedNumberUiModel.Zero, false),
            topBarHeight = 0.dp
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun SearchNoResultPreview() {
    ProtonTheme {
        SearchNoResult(topBarHeight = 0.dp)
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
    const val MailboxEmptyRoot = "MailboxEmptyRootItem"
    const val MailboxEmptyImage = "MailboxEmptyImage"
    const val MailboxEmptyTitle = "MailboxEmptyTitle"
    const val MailboxEmptySubtitle = "MailboxEmptySubtitle"
    const val Root = "MailboxScreen"
}
