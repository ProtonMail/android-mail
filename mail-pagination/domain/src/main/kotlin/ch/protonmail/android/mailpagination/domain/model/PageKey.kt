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

package ch.protonmail.android.mailpagination.domain.model

import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.All
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.Read
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.Unread

/**
 * Page Parameters needed to query/fetch/filter/sort/order a page.
 */
sealed interface PageKey {

    val pageToLoad: PageToLoad

    data class DefaultPageKey(
        val labelId: LabelId = LabelId("0"),
        val categoryLabelId: CategoryLabelId? = null,
        override val pageToLoad: PageToLoad = PageToLoad.First
    ) : PageKey

    data class PageKeyForSearch(
        val keyword: String,
        override val pageToLoad: PageToLoad = PageToLoad.First
    ) : PageKey
}

fun PageKey.copyWithNewPageToLoad(pageToLoad: PageToLoad): PageKey {
    return when (this) {
        is PageKey.DefaultPageKey -> this.copy(pageToLoad = pageToLoad)
        is PageKey.PageKeyForSearch -> this.copy(pageToLoad = pageToLoad)
    }
}


/**
 * Filter only [Read], [Unread] or [All] items.
 *
 */
enum class ReadStatus {
    All,
    Read,
    Unread
}

/**
 * Filters whether Spam/Trash elements should be included in the results.
 *
 * It only applies to AlmostAllMail label and Search mode.
 */
enum class ShowSpamTrash {
    Hide,
    Show
}

/**
 * Filter which page to load from rust lib, based on the current labelId.
 *
 * "First" returns the initial page (most up to date items)
 * "Next" calls keep returning the following items based on rust-internal tracking logic
 * "All" returns all the items loaded so far
 */
enum class PageToLoad {
    First,
    Next,
    All
}
