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

package ch.protonmail.android.maildetail.presentation.model

import androidx.compose.runtime.Stable
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maillabel.domain.model.LabelId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
sealed interface PagedConversationDetailState {

    object Loading : PagedConversationDetailState
    object Error : PagedConversationDetailState

    data class Ready(
        val settings: PagerSettings,
        val dynamicViewPagerState: DynamicViewPagerState,
        val navigationArgs: NavigationArgs
    ) : PagedConversationDetailState
}

@Stable
data class PagerSettings(
    val swipeEnabled: Boolean,
    val autoAdvanceEnabled: Boolean
)

@Stable
data class NavigationArgs(
    val singleMessageMode: Boolean,
    val openedFromLocation: LabelId,
    val conversationEntryPoint: ConversationDetailEntryPoint
)

enum class Error {
    NETWORK, OTHER
}

@Stable
sealed interface Page {
    data class Conversation(val cursorId: CursorId) : Page
    object Loading : Page
    object Error : Page
    object End : Page
}

fun Page?.exists() = this !is Page.End && this != null

data class PagedConversationEffects(
    val error: Effect<Error> = Effect.empty()
)

@Stable
data class DynamicViewPagerState(
    val userScrollEnabled: Boolean = true,
    val currentPageIndex: Int? = null,
    val pages: ImmutableList<Page> = emptyList<Page>().toImmutableList(),
    val focusPageIndex: Int? = null,
    val pendingRemoval: Page? = null,
    val scrollToPage: Effect<Unit> = Effect.empty(),
    val exit: Effect<Unit> = Effect.empty()
)

fun MutableList<Page>.addPage(page: Page?): MutableList<Page> {
    if (page != null && page != Page.End) {
        add(page)
    }
    return this
}

fun DynamicViewPagerState.currentPage() = currentPageIndex?.let {
    if (currentPageIndex < pages.size) {
        pages[currentPageIndex]
    } else null
}

fun DynamicViewPagerState.nextPage() = currentPageIndex?.let {
    if (currentPageIndex < pages.size - 1) {
        pages[currentPageIndex + 1]
    } else null
}

sealed interface PagedConversationDetailOperation

sealed interface PagedConversationDetailEvent : PagedConversationDetailOperation {
    data class UpdatePage(
        val currentItem: Page?,
        val nextItem: Page?,
        val previousItem: Page?
    ) :
        PagedConversationDetailEvent

    data class Error(val error: ConversationCursorError) : PagedConversationDetailEvent
    data class Ready(
        val pagerSettings: PagerSettings,
        val currentItem: Page,
        val nextItem: Page?,
        val previousItem: Page?,
        val navigationArgs: NavigationArgs
    ) : PagedConversationDetailEvent

    data class SettingsUpdated(val pagerSettings: PagerSettings) : PagedConversationDetailEvent

    object AutoAdvanceRequested : PagedConversationDetailEvent
    object ClearFocusPage : PagedConversationDetailEvent
    object PageChanging : PagedConversationDetailEvent

}

sealed interface PagedConversationDetailAction : PagedConversationDetailOperation {
    data class SetSettledPage(val value: Int) : PagedConversationDetailAction
    object AutoAdvance : PagedConversationDetailAction
    object ClearFocusPage : PagedConversationDetailAction
}

