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

package ch.protonmail.android.mailcategory.presentation.mapper

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.mailcategory.domain.model.CategorySystemLabelId
import ch.protonmail.android.mailcategory.presentation.R
import ch.protonmail.android.mailcategory.presentation.design.CategoryPillColors

@StringRes
fun CategorySystemLabelId.categoryTextRes() = when (this) {
    CategorySystemLabelId.Social -> R.string.category_title_social
    CategorySystemLabelId.Promotions -> R.string.category_title_promotions
    CategorySystemLabelId.Updates -> R.string.category_title_updates
    CategorySystemLabelId.Forums -> R.string.category_title_forums
    CategorySystemLabelId.Primary -> R.string.category_title_primary
    CategorySystemLabelId.Newsletter -> R.string.category_title_newsletters
    CategorySystemLabelId.Transactions -> R.string.category_title_transactions
}

@DrawableRes
fun CategorySystemLabelId.categoryIconRes() = when (this) {
    CategorySystemLabelId.Social -> R.drawable.ic_category_social
    CategorySystemLabelId.Promotions -> R.drawable.ic_category_promotions
    CategorySystemLabelId.Updates -> R.drawable.ic_category_updates
    CategorySystemLabelId.Forums -> R.drawable.ic_category_forums
    CategorySystemLabelId.Primary -> R.drawable.ic_category_primary
    CategorySystemLabelId.Newsletter -> R.drawable.ic_category_newsletter
    CategorySystemLabelId.Transactions -> R.drawable.ic_category_transactions
}

fun CategorySystemLabelId.categoryColor(isActive: Boolean) = if (isActive) {
    when (this) {
        CategorySystemLabelId.Primary -> CategoryPillColors.Primary
        CategorySystemLabelId.Social -> CategoryPillColors.Social
        CategorySystemLabelId.Promotions -> CategoryPillColors.Promotions
        CategorySystemLabelId.Updates -> CategoryPillColors.Updates
        CategorySystemLabelId.Forums -> CategoryPillColors.Forums
        CategorySystemLabelId.Newsletter -> CategoryPillColors.Newsletters
        CategorySystemLabelId.Transactions -> CategoryPillColors.Transactions
    }
} else {
    CategoryPillColors.InactiveBackground
}
