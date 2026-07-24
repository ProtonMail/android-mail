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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import me.proton.core.domain.entity.UserId
import kotlin.time.Duration.Companion.milliseconds

/**
 * Decides when the mailbox loading skeleton is shown.
 *
 * The mailbox reloads for two very different reasons and only one of them warrants a skeleton:
 * - **Navigation** — a different account, label, category or search mode. The list on screen no longer
 *   belongs where the user is, so a skeleton is the honest thing to render.
 * - **In-place reload** — an unread / spam-trash filter toggle or a pull-to-refresh on the list the user
 *   is already looking at. Replacing it with a skeleton reads as a flash; the thin top loading bar
 *   already signals the activity.
 *
 * Paging cannot tell the two apart on its own, so [MailboxLocation] identifies the list and
 * [SettledViewLatch] decides which reload is which. [CategorySkeletonLatch] and
 * [rememberSkeletonVisibility] then keep the skeleton on screen as one steady frame for the whole
 * navigation instead of letting Paging's start-up oscillation blink through it.
 *
 * See ET-6553, ET-6594 and ET-6565.
 */

/**
 * Identifies the list currently on screen. Any change means the user navigated somewhere else, so the
 * items Paging is holding no longer belong to where they are.
 *
 * All four parts matter: switching account keeps the same label and category, entering search keeps
 * both as well, and either one leaves the previous list on screen until Paging catches up.
 */
internal data class MailboxLocation(
    val userId: UserId?,
    val labelId: String,
    val categoryId: String?,
    val isInSearch: Boolean
)

/**
 * Keeps the category-switch skeleton visible for the whole first-page (re)load, from the moment the
 * scroller starts loading its first page until the Paging refresh presents the new list. Without it,
 * the previous category's items flash back in between. See ET-6553.
 *
 * It arms by edge-detecting [ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.firstPageLoadingStartCount]
 * rather than reading a toggling boolean: the state arrives through a conflated StateFlow, so a fast
 * start/end pair could be conflated away and never observed, leaving the skeleton un-armed (the
 * "loading too quick → stale flash" bug). A monotonic counter's latest value always reveals that a load
 * started, so the arm is reliable. It then stays latched — bridging the gap before the Paging refresh
 * begins — and clears once that refresh has been seen loading and has settled (the new list is present).
 */
internal class CategorySkeletonLatch {

    private var initialised = false
    private var lastStartCount = 0
    private var latched = false
    private var sawRefreshLoading = false

    fun update(
        firstPageLoadingStartCount: Int,
        isRefreshLoading: Boolean,
        isInError: Boolean
    ): Boolean {
        // Seed on the first composition so a pre-existing count (e.g. a config change that recreates
        // the composable while the ViewModel survives) does not arm a spurious skeleton.
        if (!initialised) {
            lastStartCount = firstPageLoadingStartCount
            initialised = true
        }
        val started = firstPageLoadingStartCount > lastStartCount
        lastStartCount = firstPageLoadingStartCount

        if (started) {
            latched = true
            sawRefreshLoading = false
        }
        latched = when {
            isInError -> false
            !latched -> false
            isRefreshLoading -> true.also { sawRefreshLoading = true }
            sawRefreshLoading -> false // refresh loaded and settled → new list is present
            else -> true // armed, but the refresh has not started yet → bridge the gap
        }
        return latched
    }
}

/**
 * Suppresses the loading skeleton for in-place reloads, so it only ever shows while navigating to a
 * different list. See ET-6594.
 *
 * A view counts as *settled* for a location once the pager has actually reloaded there — observed as
 * `reloading → not reloading` — and produced a non-loading view. That confirmation matters: the location
 * changes (a category tab flips, an account switches) one or more frames **before** the pager reacts, so
 * in between `rawView` is still rendering the *previous* location's items. Treating that as settled is
 * what would suppress the skeleton for the very navigation it is meant to cover, re-opening ET-6553.
 *
 * Only [MailboxScreenState.Empty] and [MailboxScreenState.SearchNoData] are ever held across a reload.
 * They render no list content, so holding them is safe; every other state either wraps the live
 * [androidx.paging.compose.LazyPagingItems] — which would draw an empty list once the reload cleared it —
 * or is an error the user needs to see.
 */
internal class SettledViewLatch {

    private var initialised = false
    private var location: MailboxLocation? = null
    private var sawReload = false
    private var settled = false
    private var settledView: MailboxScreenState = MailboxScreenState.Loading

    fun update(
        rawView: MailboxScreenState,
        location: MailboxLocation?,
        isPagerReloading: Boolean
    ): MailboxScreenState {
        if (!initialised || location != this.location) {
            initialised = true
            this.location = location
            sawReload = false
            settled = false
        }
        sawReload = sawReload || isPagerReloading

        val isLoading = rawView is MailboxScreenState.Loading
        if (settled && isLoading && settledView.rendersNoListContent()) return settledView

        // Record a settled view only once the pager has reloaded for this location; until then `rawView`
        // still belongs to wherever the user came from.
        val pagerCaughtUp = sawReload && !isPagerReloading
        if (!isLoading && (settled || pagerCaughtUp)) {
            settled = true
            settledView = rawView
        }
        return rawView
    }

    private fun MailboxScreenState.rendersNoListContent() =
        this is MailboxScreenState.Empty || this is MailboxScreenState.SearchNoData
}

/**
 * Smooths the skeleton so it renders as a single steady frame instead of flashing. Paging's refresh
 * oscillates `Loading`/`NotLoading` a few times before settling, and each gap would otherwise fall
 * through to the "No messages" empty state — an `empty → skeleton → empty` blink. See ET-6594.
 *
 * The show edge and the immediate release on real items are part of the returned expression rather than
 * stored state, so both land in the same frame the inputs change; only the grace window that bridges the
 * oscillation needs to outlive a composition. Holding it in state written from composition instead would
 * invalidate the caller and cost a full extra recomposition of the mailbox on every flip.
 *
 * The window bridges the sub-frame gaps between refresh bursts, not a slow network: a genuinely ongoing
 * load keeps [rawVisible] latched for as long as it runs.
 */
@Composable
internal fun rememberSkeletonVisibility(rawVisible: Boolean, hasItems: Boolean): Boolean {
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(rawVisible, hasItems) {
        when {
            rawVisible -> holding = true
            hasItems -> holding = false
            holding -> {
                delay(SKELETON_HIDE_DELAY_MILLIS.milliseconds)
                holding = false
            }
        }
    }
    val bridgingTheGap = holding && !hasItems
    return rawVisible || bridgingTheGap
}

private const val SKELETON_HIDE_DELAY_MILLIS = 500L
