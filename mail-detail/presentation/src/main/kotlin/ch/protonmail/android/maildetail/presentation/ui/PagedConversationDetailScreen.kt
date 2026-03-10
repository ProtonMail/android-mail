/*
 * Copyright (c) 2025 Proton Technologies AG
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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonHorizontallyCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonSnackbarHostState
import ch.protonmail.android.design.compose.component.ProtonSnackbarType
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.maildetail.presentation.model.ConversationTopBarState
import ch.protonmail.android.maildetail.presentation.model.DynamicViewPagerState
import ch.protonmail.android.maildetail.presentation.model.Error
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.Page
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailAction
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailState
import ch.protonmail.android.maildetail.presentation.viewmodel.PagedConversationDetailViewModel
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Composable
fun PagedConversationDetailScreen(
    modifier: Modifier = Modifier,
    actions: ConversationDetail.Actions,
    viewModel: PagedConversationDetailViewModel = hiltViewModel()
) {

    val effects = viewModel.effects.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    ConsumableLaunchedEffect(effects.error) {
        when (it) {
            Error.NETWORK -> actions.showSnackbar(
                context.getString(R.string.presentation_general_connection_error),
                ProtonSnackbarType.ERROR
            )

            Error.OTHER -> actions.showSnackbar(
                context.getString(R.string.presentation_error_general),
                ProtonSnackbarType.ERROR
            )
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val currentState = state) {
        is PagedConversationDetailState.Error,
        is PagedConversationDetailState.Loading -> ProtonHorizontallyCenteredProgress()

        is PagedConversationDetailState.Ready -> {
            ConsumableLaunchedEffect(currentState.dynamicViewPagerState.exit) {
                actions.onExit(null)
            }
            PagedConversationDetailScreen(
                modifier = modifier,
                conversationDetailActions = actions,
                state = currentState,
                showUndoableOperationSnackbar = { action -> actions.showUndoableOperationSnackbar(action) },
                onPagerAction = { viewModel.submit(it) }
            )
        }
    }
}

@Composable
private fun PagedConversationDetailScreen(
    modifier: Modifier = Modifier,
    conversationDetailActions: ConversationDetail.Actions,
    state: PagedConversationDetailState.Ready,
    showUndoableOperationSnackbar: (notifyUserMessage: ActionResult?) -> Unit,
    onPagerAction: (PagedConversationDetailAction) -> Unit
) {
    val onTopbarBackClicked = { conversationDetailActions.onExit(null) }
    val actions = state.autoAdvanceEnabled.takeIf { it }?.let {
        conversationDetailActions.copy(
            onExit = {
                if (state.autoAdvanceEnabled) {
                    showUndoableOperationSnackbar(it)
                    onPagerAction(PagedConversationDetailAction.AutoAdvance)
                } else {
                    conversationDetailActions.recordMailboxScreenView()
                    conversationDetailActions.onExit(it)
                }
            }
        )
    } ?: conversationDetailActions

    val conversationDetailScreenArgs =
        ConversationDetail.NavigationArgs(
            singleMessageMode = state.navigationArgs.singleMessageMode,
            openedFromLocation = state.navigationArgs.openedFromLocation,
            conversationEntryPoint = state.navigationArgs.conversationEntryPoint,
            initialScrollToMessageId = null
        )

    ConversationPager(
        modifier = modifier,
        conversationActions = actions,
        state = state.dynamicViewPagerState,
        swipeEnabled = state.swipeEnabled,
        conversationDetailScreenNavArgs = conversationDetailScreenArgs,
        onPagerAction = onPagerAction,
        onTopBarExit = onTopbarBackClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun ConversationPager(
    modifier: Modifier = Modifier,
    conversationActions: ConversationDetail.Actions,
    conversationDetailScreenNavArgs: ConversationDetail.NavigationArgs,
    state: DynamicViewPagerState,
    swipeEnabled: Boolean,
    onPagerAction: (PagedConversationDetailAction) -> Unit,
    onTopBarExit: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPageIndex ?: 0,
        pageCount = { state.pages.size }
    )

    var stablePagerCurrentIndex by remember { mutableStateOf(pagerState.currentPage) }

    LaunchedEffect(state.focusPageIndex) {
        pagerState.stablePositionIndexFlow().collect { stablePositionIndex ->
            if (stablePositionIndex == state.focusPageIndex) {
                Timber.d(
                    "Pager settled at $stablePositionIndex, same as focusPageIndex=${state.focusPageIndex}, no-op!"
                )
            } else {
                Timber.d(
                    "Pager settled at $stablePositionIndex " +
                        "different from focusPageIndex=${state.focusPageIndex}"
                )
                onPagerAction(PagedConversationDetailAction.SetSettledPage(stablePositionIndex))
            }

            stablePagerCurrentIndex = stablePositionIndex
        }
    }

    ConsumableLaunchedEffect(state.scrollToPage) {
        state.currentPageIndex?.let { pagerState.animateScrollToPage(it + 1) }
    }

    LaunchedEffect(state.focusPageIndex, stablePagerCurrentIndex, state.pages) {
        state.focusPageIndex?.let { focusIndex ->
            if (stablePagerCurrentIndex != state.focusPageIndex) {
                // Use non-suspend scroll function to jump to focus index.
                pagerState.requestScrollToPage(focusIndex)
                onPagerAction(PagedConversationDetailAction.ClearFocusPage)
                Timber.d("Pager did not settled at focus page $focusIndex, jump scrolling to it")
            }
        }
    }

    val snackbarHostState = remember { ProtonSnackbarHostState() }

    var currentTopBarState by remember { mutableStateOf(ConversationTopBarState()) }
    val onTopBarStateUpdated = { state: ConversationTopBarState ->
        currentTopBarState = state
    }

    Scaffold(
        modifier = modifier,
        containerColor = ProtonTheme.colors.backgroundNorm,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = {
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                protonSnackbarHostState = snackbarHostState
            )
        },
        topBar = {
            DetailScreenTopBar(
                title = currentTopBarState.title.value,
                isStarred = currentTopBarState.isStarred.value,
                messageCount = currentTopBarState.messages.value,
                subjectAlpha = currentTopBarState.subjectAlpha.floatValue,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = onTopBarExit,
                    onStarClick = { currentTopBarState.onStarClick() },
                    onUnStarClick = { currentTopBarState.onStarUnClick() }
                )
            )
        }
    ) { innerPadding ->

        Pager(
            modifier = Modifier.padding(innerPadding),
            pagerState = pagerState,
            pages = state.pages,
            conversationActions = conversationActions,
            conversationDetailNavigationArgs = conversationDetailScreenNavArgs,
            onTopBarStateUpdated = onTopBarStateUpdated,
            canScroll = state.userScrollEnabled && swipeEnabled,
            isDirectionForwards = { pagerState.lastScrolledForward }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pager(
    modifier: Modifier,
    pagerState: PagerState,
    conversationActions: ConversationDetail.Actions,
    conversationDetailNavigationArgs: ConversationDetail.NavigationArgs,
    pages: ImmutableList<Page>,
    canScroll: Boolean,
    onTopBarStateUpdated: (ConversationTopBarState) -> Unit,
    isDirectionForwards: () -> Boolean
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondViewportPageCount = 0,
        key = { index ->
            pages.getOrNull(index)?.let {
                when (it) {
                    is Page.Conversation -> "${it.cursorId.conversationId}+${it.cursorId.messageId}"
                    else -> "page_$index"
                }
            } ?: "page_$index"
        },
        // Disable user scrolling while the page is being updated through ViewModel methods.
        userScrollEnabled = canScroll
    ) { page ->
        when (val item = pages[page]) {
            is Page.Conversation -> {
                val topBarHostState = remember { ConversationTopBarState() }
                if (page == pagerState.targetPage) {
                    onTopBarStateUpdated(topBarHostState)
                }
                PageUpdated(
                    topBarHostState = topBarHostState,
                    conversationActions = conversationActions,
                    navigationArgs = conversationDetailNavigationArgs.copy(
                        initialScrollToMessageId = item.cursorId.messageId?.let { MessageIdUiModel(it) }
                    ),
                    conversationId = item.cursorId.conversationId,
                    isDirectionForwards = isDirectionForwards
                )
            }

            // should be handled by indexing to never reach the end page (view pager can't scroll further)
            Page.End -> Unit

            Page.Error -> {
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.presentation_error_general),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Page.Loading -> ProtonHorizontallyCenteredProgress()
        }
    }
}

@Composable
private fun PageUpdated(
    topBarHostState: ConversationTopBarState,
    conversationActions: ConversationDetail.Actions,
    navigationArgs: ConversationDetail.NavigationArgs,
    conversationId: ConversationId,
    isDirectionForwards: () -> Boolean
) {
    ConversationDetailScreen(
        actions = conversationActions,
        conversationId = conversationId,
        navigationArgs = navigationArgs,
        topBarState = topBarHostState,
        isDirectionForwards = isDirectionForwards
    )
}

object PagedConversationDetailScreen {

    const val LocationViewModeIsConversation = "Location View Mode"
}

internal data class PagerStateSnapshot(
    val currentPage: Int,
    val settledPage: Int,
    val targetPage: Int,
    val isScrollInProgress: Boolean
)

internal fun PagerStateSnapshot.isPositionStabled(): Boolean {
    return !isScrollInProgress &&
        currentPage == settledPage &&
        currentPage == targetPage
}

internal fun PagerState.stablePositionIndexFlow(): Flow<Int> = snapshotFlow {
    PagerStateSnapshot(
        currentPage = currentPage,
        settledPage = settledPage,
        targetPage = targetPage,
        isScrollInProgress = isScrollInProgress
    )
}
    .filter { it.isPositionStabled() }
    .map { it.currentPage }
    .distinctUntilChanged()
