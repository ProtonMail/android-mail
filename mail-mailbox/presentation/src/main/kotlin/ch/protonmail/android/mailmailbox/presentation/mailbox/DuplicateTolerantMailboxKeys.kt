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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import kotlin.collections.set

/**
 * Builds stable unique keys (allowing duplicate items) for the currently loaded snapshot items.
 *
 * Due to a data-layer Rust scroller integration issue, duplicate mailbox items
 * (same id + user id) can temporarily appear in the list.
 *
 * We intentionally avoid using the adapter index as part of the key because that
 * breaks stable identity and causes insertion/deletion animations
 * to behave incorrectly.
 *
 * Instead, we:
 *  - Compute a baseKey from the current Paging snapshot ("$userId:$id")
 *  - Detect duplicates
 *  - Append a deterministic occurrence suffix (#0, #1, #2 etc.)
 */
@Composable
fun rememberDuplicateTolerantMailboxKeys(pagingItems: LazyPagingItems<MailboxItemUiModel>): List<String> {
    val snapshotItems = pagingItems.itemSnapshotList.items

    return remember(snapshotItems) {
        val counters = HashMap<String, Int>(snapshotItems.size)
        snapshotItems.map { item ->
            val baseKey = "${item.userId}:${item.id}"
            val occurrence = counters[baseKey] ?: 0
            counters[baseKey] = occurrence + 1
            "$baseKey#$occurrence"
        }
    }
}
