/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ch.protonmail.android.design.compose.modifier.rememberCollapsingTopBarState
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.presentation.IntentHelper
import ch.protonmail.android.mailattachments.presentation.model.FileContent
import ch.protonmail.android.mailattachments.presentation.ui.OpenAttachmentInput
import ch.protonmail.android.mailattachments.presentation.ui.fileOpener
import ch.protonmail.android.mailattachments.presentation.ui.fileSaver
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.SnackbarError
import ch.protonmail.android.mailcommon.presentation.SnackbarNormal
import ch.protonmail.android.mailcommon.presentation.SnackbarUndo
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchResultUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchViewAction
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesState
import ch.protonmail.android.mailcontentsearch.presentation.model.RecentSearchesViewAction
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.ContentSearchViewModel
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.IndexingProgressViewModel
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.RecentSearchesViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxSkeletonLoading
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.flowOf

@Composable
fun ContentSearchScreen(actions: ContentSearchScreenActions) {
    val onClose = actions.onClose
    val onOpenItem = actions.onOpenItem
    val showSnackbar = actions.showSnackbar
    val snackbarHeight = actions.snackbarHeight
    val viewModel: ContentSearchViewModel = hiltViewModel()
    val recentsViewModel: RecentSearchesViewModel = hiltViewModel()
    val indexingViewModel: IndexingProgressViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recentsState by recentsViewModel.state.collectAsStateWithLifecycle()
    val indexingState by indexingViewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(items.itemCount, items.loadState.refresh) {
        if (items.loadState.refresh is LoadState.NotLoading) {
            viewModel.submit(ContentSearchViewAction.ResultsLoaded(items.itemCount))
        }
    }

    LaunchedEffect(items.itemSnapshotList) {
        viewModel.submit(ContentSearchViewAction.ItemsChanged(items.itemSnapshotList.items.map { it.id }))
    }

    // The history has no change stream, so re-read it every time the screen falls back to the idle
    // page — on open, and again after the user clears a query (possibly having opened a result).
    val isIdle = state.phase is ContentSearchState.Phase.Idle
    LaunchedEffect(isIdle) {
        if (isIdle) recentsViewModel.submit(RecentSearchesViewAction.Refresh)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.queryState.text.toString() }
            .collect { recentsViewModel.submit(RecentSearchesViewAction.QueryChanged(it)) }
    }

    ConsumableLaunchedEffect(effect = state.closeScreen) { onClose() }

    ConsumableLaunchedEffect(effect = state.actionMessage) { showSnackbar(SnackbarUndo(it)) }

    val context = LocalContext.current
    val openAttachment = fileOpener()
    val saveAttachment = fileSaver(
        onFileSaved = { showSnackbar(SnackbarNormal(it)) },
        onError = { showSnackbar(SnackbarError(it)) }
    )
    ConsumableLaunchedEffect(effect = state.openAttachment) { intentValues ->
        val input = OpenAttachmentInput(intentValues.uri, intentValues.mimeType)
        if (IntentHelper.canOpenFile(context, input)) {
            openAttachment(input)
        } else {
            saveAttachment(
                FileContent(
                    intentValues.name,
                    intentValues.mimeType,
                    intentValues.userId,
                    intentValues.attachmentId
                )
            )
        }
    }
    ConsumableTextEffect(effect = state.errorMessage) { showSnackbar(SnackbarError(it)) }

    ConsumableLaunchedEffect(effect = state.reloadResults) {
        items.refresh()
    }

    ContentSearchScreen(
        state = state,
        recentsState = recentsState,
        indexingState = indexingState,
        items = items,
        queryState = viewModel.queryState,
        snackbarHeight = snackbarHeight(),
        consumeAutoFocus = viewModel::consumeSearchFieldAutoFocus,
        recentsActions = RecentSearchesActions(
            // Only bumps the query in the history; the search itself is re-run by the screen, which
            // owns the search field's text.
            onTermClicked = { recentsViewModel.submit(RecentSearchesViewAction.TermClicked(it)) },
            onTermDismissed = { recentsViewModel.submit(RecentSearchesViewAction.TermDismissed(it)) },
            onClearTermsClicked = { recentsViewModel.submit(RecentSearchesViewAction.ClearTerms) },
            onFoundItemClicked = { found ->
                recentsViewModel.submit(RecentSearchesViewAction.FoundItemClicked(found))
                onOpenItem(
                    OpenSearchResultRequest(
                        messageId = found.messageId,
                        conversationId = found.item.conversationId,
                        // The query recorded with the item, so the detail view highlights the same
                        // terms it was originally found by.
                        searchQuery = found.searchQuery.orEmpty(),
                        isConversationGrouping = state.isConversationGrouping,
                        // An item moved to Trash or Spam since it was found has to be opened from All
                        // Mail, the only location that still contains it. Everything else keeps the
                        // results' location, so the detail screen behaves as it does for a live row.
                        openedFromLocation = if (found.isInTrashOrSpam) {
                            state.allMailLocation
                        } else {
                            state.openedFromLocation
                        },
                        shouldOpenInComposer = found.item.shouldOpenInComposer
                    )
                )
            },
            onFoundItemStarClicked = { recentsViewModel.submit(RecentSearchesViewAction.FoundItemStarClicked(it)) },
            onAvatarImageLoadRequested = {
                viewModel.submit(ContentSearchViewAction.AvatarImageLoadRequested(it))
            },
            onAvatarImageLoadFailed = { viewModel.submit(ContentSearchViewAction.AvatarImageLoadFailed(it)) },
            onAttachmentClicked = { viewModel.submit(ContentSearchViewAction.RequestAttachment(it)) }
        ),
        actions = ContentSearchActions(
            onClose = onClose,
            onSearch = { viewModel.submit(ContentSearchViewAction.Search) },
            onSuggestionSelected = { viewModel.submit(ContentSearchViewAction.SuggestionSelected(it)) },
            onClearQuery = { viewModel.submit(ContentSearchViewAction.ClearQuery) },
            onToggleIncludeSpamTrash = { viewModel.submit(ContentSearchViewAction.ToggleIncludeSpamTrash) },
            onSeeIndexingDetails = actions.onSeeIndexingDetails,
            onItemClicked = { result ->
                val item = result.item
                if (state.inSelectionMode) {
                    viewModel.submit(ContentSearchViewAction.ToggleItemSelection(item))
                } else {
                    val messageId = MessageId(item.id)
                    // Opening a result is what creates search history: it records both the query and
                    // the item, so the idle page can offer them next time.
                    recentsViewModel.submit(
                        RecentSearchesViewAction.SearchResultOpened(
                            query = state.query,
                            messageId = messageId,
                            conversationId = item.conversationId
                        )
                    )
                    onOpenItem(
                        OpenSearchResultRequest(
                            messageId = messageId,
                            conversationId = item.conversationId,
                            searchQuery = state.query,
                            isConversationGrouping = state.isConversationGrouping,
                            // A result trashed while still listed — from these very results, or on
                            // another device — has left the searched location, so open it from All Mail.
                            openedFromLocation = if (result.isInTrashOrSpam) {
                                state.allMailLocation
                            } else {
                                state.openedFromLocation
                            },
                            shouldOpenInComposer = item.shouldOpenInComposer
                        )
                    )
                }
            },
            onStarClicked = { item ->
                viewModel.submit(ContentSearchViewAction.StarAction(item.id, item.isStarred))
            },
            onItemLongClicked = { viewModel.submit(ContentSearchViewAction.ItemLongClicked(it)) },
            onAvatarImageLoadRequested = {
                viewModel.submit(ContentSearchViewAction.AvatarImageLoadRequested(it))
            },
            onAvatarImageLoadFailed = { viewModel.submit(ContentSearchViewAction.AvatarImageLoadFailed(it)) },
            onAttachmentClicked = { viewModel.submit(ContentSearchViewAction.RequestAttachment(it)) },
            onExitSelectionMode = { viewModel.submit(ContentSearchViewAction.ExitSelectionMode) },
            onMarkRead = { viewModel.submit(ContentSearchViewAction.MarkAsRead) },
            onMarkUnread = { viewModel.submit(ContentSearchViewAction.MarkAsUnread) },
            onStarSelection = { viewModel.submit(ContentSearchViewAction.Star) },
            onUnStarSelection = { viewModel.submit(ContentSearchViewAction.UnStar) },
            onTrash = { viewModel.submit(ContentSearchViewAction.Trash) },
            onArchive = { viewModel.submit(ContentSearchViewAction.MoveToArchive) },
            onSpam = { viewModel.submit(ContentSearchViewAction.MoveToSpam) },
            onMoveToInbox = { viewModel.submit(ContentSearchViewAction.MoveToInbox) },
            onDelete = { viewModel.submit(ContentSearchViewAction.Delete) },
            onDeleteConfirmed = { viewModel.submit(ContentSearchViewAction.DeleteConfirmed) },
            onDeleteDialogDismissed = { viewModel.submit(ContentSearchViewAction.DeleteDialogDismissed) },
            onRequestMoveTo = { viewModel.submit(ContentSearchViewAction.RequestMoveToBottomSheet) },
            onRequestLabelAs = { viewModel.submit(ContentSearchViewAction.RequestLabelAsBottomSheet) },
            onRequestMore = { viewModel.submit(ContentSearchViewAction.RequestMoreActionsBottomSheet) },
            onDismissBottomSheet = { viewModel.submit(ContentSearchViewAction.DismissBottomSheet) },
            onMoveToCompleted = { destination, itemCount ->
                viewModel.submit(ContentSearchViewAction.MoveToCompleted(destination, itemCount))
            },
            onLabelAsCompleted = { archived, itemCount ->
                viewModel.submit(ContentSearchViewAction.LabelAsCompleted(archived, itemCount))
            }
        )
    )
}

@Composable
private fun ContentSearchScreen(
    state: ContentSearchState,
    recentsState: RecentSearchesState,
    indexingState: IndexingProgressState,
    items: LazyPagingItems<ContentSearchResultUiModel>,
    queryState: TextFieldState,
    actions: ContentSearchActions,
    recentsActions: RecentSearchesActions,
    snackbarHeight: Dp = 0.dp,
    consumeAutoFocus: () -> Boolean = { false }
) {
    // Derived so only a change in emptiness, not every keystroke, reaches the body below.
    val isQueryEmpty by remember(queryState) { derivedStateOf { queryState.text.isBlank() } }

    // The list currently under the top bar, if there is one. Each page keeps its own scroll position,
    // and the skeleton and the empty states don't scroll at all.
    val resultsListState = rememberLazyListState()
    val recentsListState = rememberLazyListState()
    val scrollingList = when (val phase = state.phase) {
        ContentSearchState.Phase.Idle -> recentsListState.takeIf { recentsState is RecentSearchesState.Data }
        ContentSearchState.Phase.Loading -> null
        is ContentSearchState.Phase.Results -> resultsListState.takeIf { phase.count > 0 }
    }

    val collapsingTopBarState = rememberCollapsingTopBarState()
    SideEffect {
        // Selection mode pins the bar: its back button is the way out of it.
        collapsingTopBarState.configure(enabled = !state.inSelectionMode) {
            scrollingList?.canScrollForward == true
        }
    }
    // Bring the bar back whenever what sits under it changes. A page that just appeared, or one that
    // can't scroll, has no way to scroll a hidden bar back into view.
    LaunchedEffect(scrollingList, state.inSelectionMode) {
        collapsingTopBarState.animateExpand()
    }

    val bottomPadding by animateDpAsState(
        targetValue = maxOf(
            ProtonDimens.Spacing.Large,
            snackbarHeight - MailDimens.SnackbarOuterMargin + MailDimens.SnackbarFabGap
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "contentSearchBottomPadding"
    )

    ContentSearchBottomSheetHost(
        bottomSheetState = state.bottomSheetState,
        inSelectionMode = state.inSelectionMode,
        actions = actions
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ProtonTheme.colors.backgroundNorm)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .nestedScroll(collapsingTopBarState.nestedScrollConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ContentSearchTopBar(
                    queryState = queryState,
                    includeFilter = state.includeFilter,
                    isSearchLive = state.isSearchLive,
                    actions = actions,
                    inSelectionMode = state.inSelectionMode,
                    collapsingState = collapsingTopBarState,
                    consumeAutoFocus = consumeAutoFocus
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))

                ContentSearchBody(
                    state = state,
                    recentsState = recentsState,
                    indexingState = indexingState,
                    items = items,
                    isQueryEmpty = isQueryEmpty,
                    actions = actions,
                    recentsActions = recentsActions,
                    resultsListState = resultsListState,
                    recentsListState = recentsListState,
                    modifier = Modifier.weight(1f)
                )
            }

            ContentSearchBottomToolbar(
                bottomBarState = state.bottomBarState,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = ProtonDimens.Spacing.Large)
                    .padding(top = ProtonDimens.Spacing.Large, bottom = bottomPadding)
            )
        }

        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = actions.onDeleteConfirmed,
                onDismiss = actions.onDeleteDialogDismissed
            )
        }
    }
}

@Composable
private fun ContentSearchBody(
    state: ContentSearchState,
    recentsState: RecentSearchesState,
    indexingState: IndexingProgressState,
    items: LazyPagingItems<ContentSearchResultUiModel>,
    isQueryEmpty: Boolean,
    actions: ContentSearchActions,
    recentsActions: RecentSearchesActions,
    resultsListState: LazyListState,
    recentsListState: LazyListState,
    modifier: Modifier = Modifier
) {
    when (val phase = state.phase) {
        // Loading / no-results aren't scrollable, so they keep clear of the navigation bar via bottom
        // safe padding (the lists, by contrast, scroll under it). The app bar owns the top inset.
        ContentSearchState.Phase.Idle -> PageWithIndexingBanner(
            indexingState = indexingState,
            onSeeDetailsClick = actions.onSeeIndexingDetails,
            modifier = modifier
        ) { pageModifier, bannerHeight ->
            ContentSearchIdlePage(
                state = state,
                recentsState = recentsState,
                isQueryEmpty = isQueryEmpty,
                actions = actions,
                recentsActions = recentsActions,
                listState = recentsListState,
                modifier = pageModifier,
                firstRunBottomPadding = bannerHeight
            )
        }

        ContentSearchState.Phase.Loading -> Column(
            modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            MailboxSkeletonLoading()
        }

        is ContentSearchState.Phase.Results -> if (phase.count == 0) {
            PageWithIndexingBanner(
                indexingState = indexingState,
                onSeeDetailsClick = actions.onSeeIndexingDetails,
                modifier = modifier
            ) { pageModifier, bannerHeight ->
                SearchMessageState(
                    iconRes = R.drawable.ic_search_empty_result,
                    title = stringResource(R.string.content_search_no_results_title),
                    description = stringResource(R.string.content_search_no_results_description),
                    modifier = pageModifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(bottom = bannerHeight)
                )
            }
        } else {
            ResultsList(
                items = items,
                actions = actions,
                highlightText = state.query,
                selectionMode = state.inSelectionMode,
                selectedItemIds = remember(state.selectionState.selectedItems) {
                    state.selectionState.selectedItems.mapTo(HashSet()) { it.id }
                },
                avatarImages = state.avatarImages,
                downloadingAttachmentId = state.downloadingAttachmentId,
                listState = resultsListState,
                modifier = modifier
            )
        }
    }
}

/**
 * Puts the indexing banner, when there is one to show, directly under the top bar and above [page].
 *
 * The banner belongs to the pages with nothing to read, so it never competes with a list of results.
 *
 * [page] is handed the banner's height as well as its modifier. A page that centres its content
 * should apply that height as bottom padding, so the content stays on the middle of the screen
 * instead of being pushed down by the banner. A page that fills its space should ignore it.
 */
@Composable
private fun PageWithIndexingBanner(
    indexingState: IndexingProgressState,
    onSeeDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (pageModifier: Modifier, bannerHeight: Dp) -> Unit
) {
    val density = LocalDensity.current
    var bannerHeight by remember { mutableStateOf(0.dp) }

    Column(modifier = modifier) {
        if (indexingState is IndexingProgressState.Visible) {
            IndexingProgressBanner(
                state = indexingState,
                onSeeDetailsClick = onSeeDetailsClick,
                modifier = Modifier
                    .onSizeChanged { bannerHeight = with(density) { it.height.toDp() } }
                    .padding(top = ProtonDimens.Spacing.ExtraLarge, bottom = ProtonDimens.Spacing.Large)
            )
        }

        page(Modifier.weight(1f), if (indexingState is IndexingProgressState.Visible) bannerHeight else 0.dp)
    }
}

/**
 * The page shown while no search is running: the history, or the first-run state when there is none.
 *
 * Once the user starts typing the history narrows down to the queries that match what is in the field —
 * the view model reads it filtered, dropping the previously found items, which belong to the queries that
 * found them rather than to the one being typed.
 */
@Composable
private fun ContentSearchIdlePage(
    state: ContentSearchState,
    recentsState: RecentSearchesState,
    isQueryEmpty: Boolean,
    actions: ContentSearchActions,
    recentsActions: RecentSearchesActions,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    firstRunBottomPadding: Dp = 0.dp
) {
    when (recentsState) {
        // Nothing yet: hold an empty page rather than flashing the first-run state and then replacing it
        // with the history a moment later.
        RecentSearchesState.Loading -> Box(modifier = modifier)

        // The first-run state invites the user to search, so it belongs to an empty field only: under a
        // typed query with no matching history there is nothing to say, and the page stays blank.
        RecentSearchesState.Empty -> if (isQueryEmpty) {
            SearchMessageState(
                iconRes = R.drawable.ic_search_empty_state,
                description = stringResource(R.string.content_search_first_run_description),
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(bottom = firstRunBottomPadding)
            )
        } else {
            Box(modifier = modifier)
        }

        is RecentSearchesState.Data -> RecentSearchesPage(
            state = recentsState,
            avatarImages = state.avatarImages,
            downloadingAttachmentId = state.downloadingAttachmentId,
            actions = recentsActions.copy(
                // Tapping a chip both bumps it in the history and runs its search.
                onTermClicked = { term ->
                    recentsActions.onTermClicked(term)
                    actions.onSuggestionSelected(term)
                }
            ),
            listState = listState,
            modifier = modifier
        )
    }
}

@Preview(name = "First run", showBackground = true, showSystemUi = true)
@Preview(name = "First run · dark", showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ContentSearchScreenFirstRunPreview() {
    ProtonTheme {
        ContentSearchScreen(
            state = ContentSearchState.Initial,
            recentsState = RecentSearchesState.Empty,
            indexingState = IndexingProgressState.Visible(percentage = 30),
            items = flowOf(PagingData.empty<ContentSearchResultUiModel>()).collectAsLazyPagingItems(),
            queryState = rememberTextFieldState(),
            actions = ContentSearchActions.Empty,
            recentsActions = RecentSearchesActions.Empty
        )
    }
}

@Preview(name = "Results", showBackground = true, showSystemUi = true)
@Composable
private fun ContentSearchScreenResultsPreview() {
    val results = listOf(
        MailboxItemUiModelPreviewData.Message.WeatherForecastAug,
        MailboxItemUiModelPreviewData.Message.WeatherForecastSep,
        MailboxItemUiModelPreviewData.Conversation.WeatherForecast
    ).map { ContentSearchResultUiModel(item = it, isInTrashOrSpam = false) }
    ProtonTheme {
        ContentSearchScreen(
            state = ContentSearchState.Initial.copy(
                query = "weather",
                phase = ContentSearchState.Phase.Results(count = results.size)
            ),
            recentsState = RecentSearchesState.Empty,
            indexingState = IndexingProgressState.Hidden,
            items = flowOf(PagingData.from(results)).collectAsLazyPagingItems(),
            queryState = rememberTextFieldState(initialText = "weather"),
            actions = ContentSearchActions.Empty,
            recentsActions = RecentSearchesActions.Empty
        )
    }
}
