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
     * list. Callers that would otherwise serve the cache instead of querying the scroller (see
     * `RustConversationsQueryImpl.reloadConversations`) should go to the scroller while it is set, so a
     * drift cannot outlive the update that caused it. Cleared by the next full replacement of the list.
     */
    var needsResync: Boolean = false
        private set

    fun itemCount(): Int = items.size

    fun applyUpdate(update: ScrollerUpdate<T>): List<T> {

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

    private fun ignoreUpdate(updateName: String, reason: String) {
        Timber.w("$updateName ignored: $reason (size=${items.size})")
        needsResync = true
    }
}
