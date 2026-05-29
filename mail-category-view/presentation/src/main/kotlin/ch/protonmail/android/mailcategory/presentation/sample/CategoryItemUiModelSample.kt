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
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategoryLabelIdUiModel

object CategoryItemUiModelSample {

    val primary =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("primary"),
            titleRes = R.string.category_title_primary,
            iconRes = R.drawable.ic_category_primary,
            systemLabel = CategorySystemLabelId.Primary,
            hasUnseen = true,
            isActive = true
        )

    val social =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("social"),
            titleRes = R.string.category_title_social,
            iconRes = R.drawable.ic_category_social,
            systemLabel = CategorySystemLabelId.Social,
            hasUnseen = true,
            isActive = false
        )

    val promotions =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("promotions"),
            titleRes = R.string.category_title_promotions,
            iconRes = R.drawable.ic_category_promotions,
            systemLabel = CategorySystemLabelId.Promotions,
            isActive = false
        )

    val newsletters =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("newsletters"),
            titleRes = R.string.category_title_newsletters,
            iconRes = R.drawable.ic_category_newsletter,
            systemLabel = CategorySystemLabelId.Newsletter,
            isActive = false
        )

    val transactions =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("transactions"),
            titleRes = R.string.category_title_transactions,
            iconRes = R.drawable.ic_category_transactions,
            systemLabel = CategorySystemLabelId.Transactions,
            isActive = false
        )

    val updates =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("updates"),
            titleRes = R.string.category_title_updates,
            iconRes = R.drawable.ic_category_updates,
            systemLabel = CategorySystemLabelId.Updates,
            isActive = false
        )

    val forums =
        CategoryItemUiModel(
            id = CategoryLabelIdUiModel("forums"),
            titleRes = R.string.category_title_forums,
            iconRes = R.drawable.ic_category_forums,
            systemLabel = CategorySystemLabelId.Forums,
            isActive = false
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
