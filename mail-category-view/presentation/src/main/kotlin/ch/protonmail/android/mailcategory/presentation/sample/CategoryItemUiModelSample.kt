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

package ch.protonmail.android.mailcategory.presentation.sample

import ch.protonmail.android.mailcategory.presentation.R
import ch.protonmail.android.mailcategory.presentation.design.CategoryPillColors
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategoryLabelIdUiModel

object CategoryItemUiModelSample {

    val primary =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("primary"),
            titleRes = R.string.category_title_primary,
            iconRes = R.drawable.ic_category_primary,
            hasUnseen = true,
            isActive = true,
            activeColor = CategoryPillColors.Primary
        )

    val social =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("social"),
            titleRes = R.string.category_title_social,
            iconRes = R.drawable.ic_category_social,
            hasUnseen = true,
            isActive = false,
            activeColor = CategoryPillColors.Social
        )

    val promotions =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("promotions"),
            titleRes = R.string.category_title_promotions,
            iconRes = R.drawable.ic_category_promotions,
            isActive = false,
            activeColor = CategoryPillColors.Promotions
        )

    val newsletters =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("newsletters"),
            titleRes = R.string.category_title_newsletters,
            iconRes = R.drawable.ic_category_newsletter,
            isActive = false,
            activeColor = CategoryPillColors.Newsletters
        )

    val transactions =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("transactions"),
            titleRes = R.string.category_title_transactions,
            iconRes = R.drawable.ic_category_transactions,
            isActive = false,
            activeColor = CategoryPillColors.Transactions
        )

    val updates =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("updates"),
            titleRes = R.string.category_title_updates,
            iconRes = R.drawable.ic_category_updates,
            isActive = false,
            activeColor = CategoryPillColors.Updates
        )

    val forums =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("forums"),
            titleRes = R.string.category_title_forums,
            iconRes = R.drawable.ic_category_forums,
            isActive = false,
            activeColor = CategoryPillColors.Forums
        )

    val all = listOf(
        primary,
        social,
        promotions,
        newsletters,
        transactions,
        updates,
        forums
    )
}
