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

package ch.protonmail.android.mailpagination.data.scroller

import timber.log.Timber

class ScrollerCache<T> {

    private val items = mutableListOf<T>()
    val snapshot: List<T> get() = items.toList()

    /**
     * Set when an update could not be applied, which means this cache no longer mirrors the scroller's
     * list. Cleared by the next full replacement of the list.
     *
     * Clearing it is not guaranteed to be prompt. While it is set, callers query the scroller instead of
     * this cache, and get_items() answering with a full replacement — the response [ScrollerOnUpdateHandler]
     * expects of it — is what puts the two back in step. Should that answer never come, the cost is that
     * every refresh queries the scroller, which is the behaviour that predates serving refreshes from here.
     */
    var needsResync: Boolean = false
        private set

    /** Set once the scroller has told us what its list holds, even if the answer was "nothing". */
    private var hasReceivedListUpdate: Boolean = false

    /**
     * Whether this cache can answer for the scroller's list: it has been told what the list holds and
     * has applied every update since. When false a caller must query the scroller instead — either
     * nothing has been loaded yet, or an update was dropped and this cache no longer mirrors it.
     *
     * Note that [snapshot] being empty is not the same thing: a location the scroller has reported as
     * empty is something this cache can answer with, and an empty answer served straight away beats
     * waiting on a scroller that may be busy fetching. See `RustConversationsQueryImpl.reloadConversations`.
     */
    val mirrorsScroller: Boolean get() = hasReceivedListUpdate && !needsResync

    fun itemCount(): Int = items.size

    fun applyUpdate(update: ScrollerUpdate<T>): List<T> {

        hasReceivedListUpdate = hasReceivedListUpdate || update.reportsListContent()

        when (update) {
            is ScrollerUpdate.Append -> {
                items.addAll(update.items)
            }

            is ScrollerUpdate.ReplaceFrom -> {
                val idx = update.idx
                when (idx) {
                    0 -> {
                        items.clear()
                        items.addAll(update.items)
                        needsResync = false
                    }

                    in 1 until items.size -> {
                        items.subList(idx, items.size).clear()
                        items.addAll(update.items)
                    }
                    items.size -> items.addAll(update.items)
                    else -> ignoreUpdate("ReplaceFrom", "idx=$idx")
                }
            }

            is ScrollerUpdate.ReplaceBefore -> {
                val idx = update.idx
                when (idx) {
                    in 0 until items.size -> {
                        items.subList(0, idx).clear()
                        items.addAll(0, update.items)
                    }
                    items.size -> {
                        items.clear()
                        items.addAll(update.items)
                        needsResync = false
                    }

                    else -> ignoreUpdate("ReplaceBefore", "idx=$idx")
                }
            }

            is ScrollerUpdate.ReplaceRange -> {
                val fromIdx = update.fromIdx
                val toIdx = update.toIdx
                when {
                    fromIdx !in 0..items.size ->
                        ignoreUpdate("ReplaceRange", "invalid fromIdx=$fromIdx")
                    toIdx !in 0..items.size ->
                        ignoreUpdate("ReplaceRange", "invalid toIdx=$toIdx")
                    fromIdx > toIdx ->
                        ignoreUpdate("ReplaceRange", "requires fromIdx <= toIdx (from=$fromIdx, to=$toIdx)")
                    else -> {
                        items.subList(fromIdx, toIdx).clear()
                        items.addAll(fromIdx, update.items)
                    }
                }
            }

            is ScrollerUpdate.None,
            is ScrollerUpdate.Error -> Unit
        }

        return snapshot
    }

    // Whether the update states what the scroller's list holds. An empty list does; None (no more items
    // to add) and Error say nothing about it, so they leave a fresh cache unable to answer for the list.
    private fun ScrollerUpdate<T>.reportsListContent(): Boolean = when (this) {
        is ScrollerUpdate.Append,
        is ScrollerUpdate.ReplaceFrom,
        is ScrollerUpdate.ReplaceBefore,
        is ScrollerUpdate.ReplaceRange -> true

        is ScrollerUpdate.None,
        is ScrollerUpdate.Error -> false
    }

    private fun ignoreUpdate(updateName: String, reason: String) {
        Timber.w("$updateName ignored: $reason (size=${items.size})")
        needsResync = true
    }
}
