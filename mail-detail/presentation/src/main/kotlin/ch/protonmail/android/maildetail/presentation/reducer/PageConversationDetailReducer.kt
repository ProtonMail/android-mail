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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maildetail.presentation.model.DynamicViewPagerState
import ch.protonmail.android.maildetail.presentation.model.Page
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.PagedConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.PagerSettings
import ch.protonmail.android.maildetail.presentation.model.addPage
import ch.protonmail.android.maildetail.presentation.model.currentPage
import ch.protonmail.android.maildetail.presentation.model.exists
import ch.protonmail.android.maildetail.presentation.model.nextPage
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import javax.inject.Inject

class PagedConversationDetailReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: PagedConversationDetailState,
        event: PagedConversationDetailEvent
    ): PagedConversationDetailState {
        return when (event) {
            is PagedConversationDetailEvent.Error -> {
                PagedConversationDetailState.Error
            }

            is PagedConversationDetailEvent.Ready -> {
                PagedConversationDetailState.Ready(
                    settings = event.pagerSettings,
                    dynamicViewPagerState = DynamicViewPagerState(
                        pages = mutableListOf<Page>()
                            .addPage(event.previousItem)
                            .addPage(event.currentItem)
                            .addPage(event.nextItem)
                            .toImmutableList()
                    ).setFocusIndexes(event.previousItem.exists()),
                    navigationArgs = event.navigationArgs
                )
            }

            is PagedConversationDetailEvent.SettingsUpdated -> {
                reduceSettingsUpdated(currentState, event.pagerSettings)
            }

            is PagedConversationDetailEvent.UpdatePage -> {
                reducePagerState(currentState) {
                    reduceUpdatePage(it, event)
                }
            }

            PagedConversationDetailEvent.PageChanging -> {
                reducePagerState(currentState) {
                    it.copy(userScrollEnabled = false)
                }
            }

            PagedConversationDetailEvent.ClearFocusPage -> {
                reducePagerState(currentState) {
                    it.copy(focusPageIndex = null).apply {
                        Timber.d("conversation-pager reducer clear focus page ${it.focusPageIndex}")
                    }
                }
            }

            PagedConversationDetailEvent.AutoAdvanceRequested -> {
                reducePagerState(currentState) {
                    reduceAutoAdvanceRequested(it)
                }
            }
        }
    }
}

private fun reduceSettingsUpdated(
    currentState: PagedConversationDetailState,
    pagerSettings: PagerSettings
): PagedConversationDetailState {
    return when (currentState) {
        is PagedConversationDetailState.Ready -> {
            currentState.copy(settings = pagerSettings)
        }

        PagedConversationDetailState.Loading,
        PagedConversationDetailState.Error -> {
            currentState
        }
    }
}

private fun reduceAutoAdvanceRequested(currentState: DynamicViewPagerState): DynamicViewPagerState {
    return if (currentState.nextPage() != null) {
        currentState.copy(
            scrollToPage = Effect.of(Unit),
            pendingRemoval = currentState.currentPage(),
            userScrollEnabled = false
        )
    } else {
        return currentState.copy(exit = Effect.of(Unit))
    }
}

private fun reduceUpdatePage(
    currentState: DynamicViewPagerState,
    event: PagedConversationDetailEvent.UpdatePage
): DynamicViewPagerState = currentState.copy(
    pages = mutableListOf<Page>()
        .addPage(event.previousItem)
        .addPage(event.currentItem)
        .addPage(event.nextItem)
        .toImmutableList(),
    userScrollEnabled = true,
    pendingRemoval = null
).setFocusIndexes(event.previousItem.exists()).apply {
    Timber.d("conversation-pager pager state updated $pages")
}

private fun reducePagerState(
    currentState: PagedConversationDetailState,
    block: (currentPagerState: DynamicViewPagerState) -> DynamicViewPagerState
) = when (currentState) {
    is PagedConversationDetailState.Ready ->
        currentState.copy(dynamicViewPagerState = block(currentState.dynamicViewPagerState))

    else -> {
        PagedConversationDetailState.Error
    }
}

private fun DynamicViewPagerState.setFocusIndexes(hasPrevious: Boolean): DynamicViewPagerState {
    val newFocusIndex = if (!hasPrevious) 0 else 1
    Timber.d("conversation-pager SETTING focus index is $newFocusIndex")
    return this.copy(
        currentPageIndex = newFocusIndex,
        focusPageIndex = newFocusIndex
    )
}
