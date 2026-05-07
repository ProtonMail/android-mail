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

import ch.protonmail.android.mailcategory.domain.model.CategoryLabel
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.mailcategory.domain.model.CategoryViewStatus
import ch.protonmail.android.mailcategory.presentation.model.CategoryItemUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategoryLabelIdUiModel
import ch.protonmail.android.mailcategory.presentation.model.CategoryViewState
import javax.inject.Inject

class CategoryViewUiModelMapper @Inject constructor() {

    fun toUiModel(categoryViewStatus: CategoryViewStatus): CategoryViewState = when (categoryViewStatus) {
        is CategoryViewStatus.Available -> CategoryViewState.Available.Data(
            categories = categoryViewStatus.categories.map { it.toUiModel() }
        )

        CategoryViewStatus.NotAvailable -> CategoryViewState.NotAvailable
        is CategoryViewStatus.Error -> CategoryViewState.NotAvailable
    }
}

fun CategoryLabel.toUiModel(): CategoryItemUiModel {
    return CategoryItemUiModel(
        id = CategoryLabelIdUiModel(id.id),
        titleRes = systemLabel.categoryTextRes(),
        iconRes = systemLabel.categoryIconRes(),
        activeColor = systemLabel.categoryColor(isActive),
        isActive = isActive
    )
}

fun CategoryLabelId.toUiModel(): CategoryLabelIdUiModel = CategoryLabelIdUiModel(id = id)

fun CategoryLabelIdUiModel.toDomainModel(): CategoryLabelId = CategoryLabelId(id = id)
