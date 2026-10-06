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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
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
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.ContentSearchSetUpPromptViewModel
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.ContentSearchViewModel
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.IndexingProgressViewModel
import ch.protonmail.android.mailcontentsearch.presentation.viewmodel.RecentSearchesViewModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxSkeletonLoading
import ch.protonmail.android.mailmailbox.presentation.paging.exception.PaginationErrorException
import ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata.MailboxItemUiModelPreviewData
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import kotlin.math.roundToInt

// Below this length, results may be incomplete, so the results list shows a hint about it.
private const val MIN_QUERY_LENGTH_FOR_FULL_RESULTS = 3

/**
 * The search screen.
 *
 * [turnOnContentSearchRequested] carries the set-up page's request back in, as the view model cannot
 * read it itself. Its own [androidx.lifecycle.SavedStateHandle] is a different instance from the one
 * on the navigation entry the set-up page writes to. See [TURN_ON_CONTENT_SEARCH_KEY].
 */
@Composable
fun ContentSearchScreen(actions: ContentSearchScreenActions, turnOnContentSearchRequested: Boolean = false) {
    val onClose = actions.onClose
    val onOpenItem = actions.onOpenItem
    val showSnackbar = actions.showSnackbar
    val snackbarHeight = actions.snackbarHeight
    val viewModel: ContentSearchViewModel = hiltViewModel()
    val recentsViewModel: RecentSearchesViewModel = hiltViewModel()
    val indexingViewModel: IndexingProgressViewModel = hiltViewModel()
    val setUpPromptViewModel: ContentSearchSetUpPromptViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recentsState by recentsViewModel.state.collectAsStateWithLifecycle()
    val indexingState by indexingViewModel.state.collectAsStateWithLifecycle()
    val canSetUpContentSearch by setUpPromptViewModel.canSetUpContentSearch.collectAsStateWithLifecycle()
    val showSetUpBanner by setUpPromptViewModel.showSetUpBanner.collectAsStateWithLifecycle()
    val showIndexingComplete by indexingViewModel.showIndexingComplete.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()

    LaunchedEffect(turnOnContentSearchRequested) {
        if (turnOnContentSearchRequested) {
            viewModel.submit(ContentSearchViewAction.TurnOnContentSearchRequested)
        }
    }

    LaunchedEffect(items.itemCount, items.loadState.refresh) {
        when (val refresh = items.loadState.refresh) {
            is LoadState.NotLoading -> viewModel.submit(ContentSearchViewAction.ResultsLoaded(items.itemCount))
            is LoadState.Error -> {
                Timber.w(refresh.error, "Content search: results failed to load")
                val error = (refresh.error as? PaginationErrorException)?.error
                viewModel.submit(ContentSearchViewAction.SearchFailed(error))
            }

            is LoadState.Loading -> Unit
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

    val uriHandler = LocalUriHandler.current
    val learnMoreLink = stringResource(id = R.string.content_search_learn_more_link)

    // A banner the page had no room for. It is laid out at no size, so it is not on screen and must
    // not hold the keyboard back. Reported from the layout, which is where the room is known.
    var isBannerDropped by remember { mutableStateOf(false) }

    // Both banners report here, and the one that reported last may already be gone. Cleared when
    // either changes, so the next banner is not read as dropped before it has been laid out once.
    LaunchedEffect(showSetUpBanner, showIndexingComplete) { isBannerDropped = false }

    // Whether a banner is on screen, or null until every answer is in. The set-up offer and the note
    // about indexing each have one to give, and the set-up offer also needs the history to be read:
    // it is only shown over an empty page.
    val isBannerShown: Boolean? = when {
        showSetUpBanner == null || showIndexingComplete == null -> null
        recentsState is RecentSearchesState.Loading -> null
        isBannerDropped -> false
        showIndexingComplete == true -> true
        else -> showSetUpBanner == true && recentsState is RecentSearchesState.Empty
    }

    val searchFieldFocusRequests = remember { SearchFieldFocusRequests() }
    val focusSearchFieldOnce = {
        if (viewModel.searchFieldFocus.consumeAutoFocus()) searchFieldFocusRequests.request()
    }

    // Searching answers the offer as much as the banner's own buttons do: the user has seen it and
    // moved on. It spends the focus hand-over too, unused. The field is already the user's, and
    // leaving it unspent would raise the keyboard over the results once the answer lands.
    val answerOfferBySearching = {
        if (showSetUpBanner == true) setUpPromptViewModel.dismissSetUpBanner()
        viewModel.searchFieldFocus.consumeAutoFocus()
    }

    // The set-up page can come back with the content search sheet to show. A focused field would raise
    // the keyboard on the way back, and the sheet would push it straight down again. So the field lets
    // go of its focus before leaving, and takes it back once nothing is in the way.
    val focusManager = LocalFocusManager.current
    val leaveForSetUp = {
        viewModel.searchFieldFocus.leaveForSetUp()
        focusManager.clearFocus()
        actions.onSetUpContentSearch()
    }

    // Runs when the screen comes back from the set-up page, and again when the sheet closes. The
    // turn-on request is already set on the first frame back, so the keyboard never comes up under
    // the sheet. Coming back with Not now or the back button focuses the field straight away.
    LaunchedEffect(turnOnContentSearchRequested) {
        if (!turnOnContentSearchRequested && viewModel.searchFieldFocus.consumeSetUpReturn()) {
            searchFieldFocusRequests.request()
        }
    }

    // The keyboard waits for the banners: one on screen would end up behind it. Whoever leaves last
    // hands the field its focus, here when there was nothing to show and from the banner's own
    // dismissal otherwise.
    LaunchedEffect(isBannerShown) {
        if (isBannerShown == false) focusSearchFieldOnce()
    }

    ContentSearchScreen(
        state = state,
        recentsState = recentsState,
        indexingState = indexingState,
        canSetUpContentSearch = canSetUpContentSearch,
        showSetUpBanner = showSetUpBanner == true,
        showIndexingComplete = showIndexingComplete == true,
        onBannerDropped = { isBannerDropped = it },
        items = items,
        queryState = viewModel.queryState,
        snackbarHeight = snackbarHeight(),
        searchFieldFocusRequests = searchFieldFocusRequests,
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
            onClearFoundItemsClicked = { recentsViewModel.submit(RecentSearchesViewAction.ClearFoundItems) },
            onAvatarImageLoadRequested = {
                viewModel.submit(ContentSearchViewAction.AvatarImageLoadRequested(it))
            },
            onAvatarImageLoadFailed = { viewModel.submit(ContentSearchViewAction.AvatarImageLoadFailed(it)) },
            onAttachmentClicked = { viewModel.submit(ContentSearchViewAction.RequestAttachment(it)) }
        ),
        actions = ContentSearchActions(
            onClose = onClose,
            onSearch = {
                answerOfferBySearching()
                viewModel.submit(ContentSearchViewAction.Search)
            },
            onSuggestionSelected = {
                answerOfferBySearching()
                viewModel.submit(ContentSearchViewAction.SuggestionSelected(it))
            },
            onClearQuery = { viewModel.submit(ContentSearchViewAction.ClearQuery) },
            onToggleIncludeSpamTrash = { viewModel.submit(ContentSearchViewAction.ToggleIncludeSpamTrash) },
            onSeeIndexingDetails = actions.onSeeIndexingDetails,
            onLearnMoreAboutContentSearch = { uriHandler.openUri(learnMoreLink) },
            onSetUpContentSearch = leaveForSetUp,
            onSetUpFromBanner = {
                setUpPromptViewModel.setAsideSetUpBanner()
                leaveForSetUp()
            },
            // Taking a banner away is the moment the user is done reading and ready to type, so the
            // field takes the focus the banner was holding back.
            onDismissSetUpBanner = {
                setUpPromptViewModel.dismissSetUpBanner()
                focusSearchFieldOnce()
            },
            onDismissIndexingComplete = {
                indexingViewModel.dismissIndexingComplete()
                focusSearchFieldOnce()
            },
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
    canSetUpContentSearch: Boolean = false,
    showSetUpBanner: Boolean = false,
    showIndexingComplete: Boolean = false,
    onBannerDropped: (Boolean) -> Unit = {},
    snackbarHeight: Dp = 0.dp,
    searchFieldFocusRequests: SearchFieldFocusRequests = remember { SearchFieldFocusRequests() }
) {
    // Derived so only a change in emptiness, not every keystroke, reaches the body below.
    val isQueryEmpty by remember(queryState) { derivedStateOf { queryState.text.isBlank() } }

    // The list currently under the top bar, if there is one. Each page keeps its own scroll position,
    // and the skeleton and the empty states don't scroll at all.
    val resultsListState = rememberLazyListState()
    val recentsListState = rememberLazyListState()
    val scrollingList = when (val phase = state.phase) {
        ContentSearchState.Phase.Idle -> recentsListState.takeIf { recentsState is RecentSearchesState.Data }
        ContentSearchState.Phase.Loading,
        ContentSearchState.Phase.Failed -> null
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
                    focusRequests = searchFieldFocusRequests
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Small))

                ContentSearchBody(
                    state = state,
                    recentsState = recentsState,
                    indexingState = indexingState,
                    canSetUpContentSearch = canSetUpContentSearch,
                    showSetUpBanner = showSetUpBanner,
                    showIndexingComplete = showIndexingComplete,
                    onBannerDropped = onBannerDropped,
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
    canSetUpContentSearch: Boolean,
    showSetUpBanner: Boolean,
    showIndexingComplete: Boolean,
    onBannerDropped: (Boolean) -> Unit,
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
            // The history is the only idle page that scrolls, and there the banner is the list's
            // first row. Pinned above a landscape list it would hold a good part of the page for
            // the whole scroll.
            isBannerPinned = recentsState !is RecentSearchesState.Data,
            modifier = modifier
        ) { pageModifier, bannerHeight ->
            val imeInsets = WindowInsets.ime

            Box(modifier = pageModifier) {
                ContentSearchIdlePage(
                    state = state,
                    recentsState = recentsState,
                    isQueryEmpty = isQueryEmpty,
                    actions = actions,
                    recentsActions = recentsActions,
                    listState = recentsListState,
                    modifier = Modifier.fillMaxSize(),
                    bannerHeight = bannerHeight,
                    indexingBanner = {
                        IndexingBanner(
                            indexingState = indexingState,
                            onSeeDetailsClick = actions.onSeeIndexingDetails
                        )
                    }
                )

                // An overlay just under the search bar, so the first-run state stays centred in
                // the same place whether or not the offer is showing. Only over an empty page: an
                // overlay covers the search history, which is what the user came to tap.
                if (showSetUpBanner && recentsState is RecentSearchesState.Empty) {
                    ContentSearchSetUpBanner(
                        actions = ContentSearchSetUpBannerActions(
                            onTurnOn = actions.onSetUpFromBanner,
                            onNotNow = actions.onDismissSetUpBanner,
                            onLearnMore = actions.onLearnMoreAboutContentSearch
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .droppedWhenItDoesNotFit(imeInsets, onBannerDropped)
                    )
                }

                // The note about indexing being done sits over the history too. It is one button,
                // and tapping it is what stops the note coming back, so it is not in the way for long.
                if (showIndexingComplete) {
                    ContentSearchIndexingCompleteBanner(
                        onGotIt = actions.onDismissIndexingComplete,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .droppedWhenItDoesNotFit(imeInsets, onBannerDropped)
                    )
                }
            }
        }

        ContentSearchState.Phase.Loading -> Column(
            modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        ) {
            MailboxSkeletonLoading()
        }

        ContentSearchState.Phase.Failed -> PageWithIndexingBanner(
            indexingState = indexingState,
            onSeeDetailsClick = actions.onSeeIndexingDetails,
            modifier = modifier
        ) { pageModifier, bannerHeight ->
            // Searching the same query again reloads it, so the retry is the search action.
            ContentSearchFailedState(
                onRetry = actions.onSearch,
                bannerHeight = bannerHeight,
                modifier = pageModifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            )
        }

        is ContentSearchState.Phase.Results -> if (phase.count == 0) {
            PageWithIndexingBanner(
                indexingState = indexingState,
                onSeeDetailsClick = actions.onSeeIndexingDetails,
                modifier = modifier
            ) { pageModifier, bannerHeight ->
                ContentSearchNoResultsState(
                    query = state.query,
                    canSetUpContentSearch = canSetUpContentSearch,
                    actions = ContentSearchNoResultsActions(
                        onLearnMore = actions.onLearnMoreAboutContentSearch,
                        onSetUpContentSearch = actions.onSetUpContentSearch
                    ),
                    bannerHeight = bannerHeight,
                    modifier = pageModifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                )
            }
        } else {
            ResultsList(
                items = items,
                actions = actions,
                query = state.query,
                selectionMode = state.inSelectionMode,
                selectedItemIds = remember(state.selectionState.selectedItems) {
                    state.selectionState.selectedItems.mapTo(HashSet()) { it.id }
                },
                avatarImages = state.avatarImages,
                downloadingAttachmentId = state.downloadingAttachmentId,
                showMinQueryLengthNotice = state.query.trim().length < MIN_QUERY_LENGTH_FOR_FULL_RESULTS,
                totalCount = phase.totalCount,
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
 * should pass that height on to it, so the content stays on the middle of the screen instead of
 * being pushed down by the banner. A page that fills its space should ignore it.
 *
 * [isBannerPinned] is false for a page that scrolls the banner itself. The slot above the page is
 * then left empty and the reported height stays zero.
 */
@Composable
private fun PageWithIndexingBanner(
    indexingState: IndexingProgressState,
    onSeeDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBannerPinned: Boolean = true,
    page: @Composable (pageModifier: Modifier, bannerHeight: Dp) -> Unit
) {
    var bannerHeight by remember { mutableStateOf(0.dp) }

    Column(modifier = modifier) {
        if (isBannerPinned) {
            IndexingBanner(
                indexingState = indexingState,
                onSeeDetailsClick = onSeeDetailsClick,
                onHeightChanged = { bannerHeight = it }
            )
        }

        page(Modifier.weight(1f), if (isBannerPinned) bannerHeight else 0.dp)
    }
}

/**
 * The indexing banner, or nothing at all while there is no progress to report.
 *
 * It fades in and slides out from behind whatever sits above it. Brought up at once it reads as a
 * flash.
 *
 * One fraction drives the room it takes, its opacity and the height it reports. Animating the
 * height and a page's padding separately gives that page two springs pulling on opposite edges, and
 * centred content stutters as they drift apart.
 *
 * [onHeightChanged] reports the room the banner takes right now, mid-animation included.
 */
@Composable
private fun IndexingBanner(
    indexingState: IndexingProgressState,
    onSeeDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit = {}
) {
    val density = LocalDensity.current

    // Held on to so the banner still has a percentage to show while it animates out.
    var lastVisibleState by remember { mutableStateOf<IndexingProgressState.Ongoing?>(null) }
    if (indexingState is IndexingProgressState.Ongoing) lastVisibleState = indexingState

    val revealed by animateFloatAsState(
        targetValue = if (indexingState is IndexingProgressState.Ongoing) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "indexingBannerReveal"
    )

    Box(
        modifier = modifier
            .clipToBounds()
            // Takes a fraction of the banner's height, keeping the banner itself at full size and
            // anchored to the bottom, so it slides down from behind the row above it.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * revealed).roundToInt()
                onHeightChanged(with(density) { height.toDp() })
                layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
            }
    ) {
        lastVisibleState?.let { visibleState ->
            IndexingProgressBanner(
                state = visibleState,
                onSeeDetailsClick = onSeeDetailsClick,
                modifier = Modifier
                    .graphicsLayer { alpha = revealed }
                    .padding(top = ProtonDimens.Spacing.ExtraLarge, bottom = ProtonDimens.Spacing.Large)
            )
        }
    }
}

/**
 * Drops the banner when the page has no room for it, instead of letting it run off the page.
 *
 * The keyboard and a large font scale both take room away, and the banner grows with the font scale
 * at the same time. Measuring it against what the page actually has beats guessing a minimum
 * height. The window is edge to edge, so the keyboard's inset comes off the page height by hand.
 *
 * [onDropped] reports the outcome. A dropped banner is not on screen, so the screen must not treat
 * it as one that is still holding the keyboard back.
 */
private fun Modifier.droppedWhenItDoesNotFit(imeInsets: WindowInsets, onDropped: (Boolean) -> Unit) =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val uncovered = (constraints.maxHeight - imeInsets.getBottom(this)).coerceAtLeast(0)
        val room = (uncovered * MAX_BANNER_PAGE_FRACTION).roundToInt()
        val fits = placeable.height <= room

        layout(width = if (fits) placeable.width else 0, height = if (fits) placeable.height else 0) {
            onDropped(!fits)
            if (fits) placeable.place(0, 0)
        }
    }

/**
 * Drops [this] when the room left is shorter than its content needs.
 *
 * The first-run state is centred artwork that cannot shrink. A centred child that does not fit is
 * laid out past the top of its box, over the indexing banner.
 */
private fun Modifier.droppedWhenThereIsNoRoom() = layout { measurable, constraints ->
    val needed = measurable.minIntrinsicHeight(constraints.maxWidth)
    if (needed > constraints.maxHeight) return@layout layout(0, 0) {}

    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

// How much of the page a banner may take before it is dropped. It is an overlay, so anything it
// covers is out of reach while it is up.
private const val MAX_BANNER_PAGE_FRACTION = 0.75f

/**
 * The page shown while no search is running: the history, or the first-run state when there is none.
 *
 * Once the user starts typing the history narrows down to the queries that match what is in the field —
 * the view model reads it filtered, dropping the previously found items, which belong to the queries that
 * found them rather than to the one being typed.
 *
 * [indexingBanner] rides the history list as its first row. The pages without a list don't show it,
 * the screen pins it above them instead.
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
    bannerHeight: Dp = 0.dp,
    indexingBanner: (@Composable () -> Unit)? = null
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
                bannerHeight = bannerHeight,
                // Keeps clear of the keyboard, which is up as soon as the screen opens with no
                // banner to read. Left under it, the artwork all but touches its top edge.
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .droppedWhenThereIsNoRoom()
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
            header = indexingBanner,
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
