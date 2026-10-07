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

package ch.protonmail.android.mailcontentsearch.presentation.model

import androidx.compose.runtime.Immutable
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel

/**
 * A search result row: the mailbox item it renders as, plus the one thing the mailbox model cannot tell
 * us and that opening it depends on — see [isInTrashOrSpam].
 *
 * Wrapped here rather than added to [MailboxItemUiModel] because only this screen opens items against a
 * location derived from a filter, and the mailbox has no use for the flag.
 *
 * @param highlightText the words to highlight in the row. See [buildHighlightText].
 */
@Immutable
data class ContentSearchResultUiModel(
    val item: MailboxItemUiModel,
    val isInTrashOrSpam: Boolean,
    val highlightText: String
) {

    val id: String get() = item.id
}

/**
 * The query's words plus the text search matched in a result, so fuzzy matches are highlighted too.
 *
 * Built once per row rather than on every recomposition. The highlight splits it on whitespace again.
 * Matched terms are single words or addresses, so the split keeps them whole.
 */
internal fun buildHighlightText(query: String, searchMatchedTerms: List<String>): String =
    (query.split(Whitespace) + searchMatchedTerms)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(" ")

private val Whitespace = Regex("\\s+")

/**
 * Whether the item sits in Trash or Spam, the two locations Almost All Mail leaves out. A result can end
 * up in one of them while still listed — it may have been trashed from these very results, or moved on
 * another device — and it then has to be opened from All Mail or the detail screen will not find it.
 *
 * Read from the exclusive locations rather than the plain labels: Trash and Spam are folders, so an item
 * is in one of them or it isn't, whatever else it may also be labelled with.
 */
internal fun MailboxItem.isInTrashOrSpam(): Boolean = exclusiveLocations.any {
    it is ExclusiveLocation.System && it.systemLabelId in TrashAndSpam
}

private val TrashAndSpam = setOf(SystemLabelId.Trash, SystemLabelId.Spam)
