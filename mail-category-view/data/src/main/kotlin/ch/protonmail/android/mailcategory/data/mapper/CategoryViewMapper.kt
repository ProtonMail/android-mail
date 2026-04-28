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

package ch.protonmail.android.mailcategory.data.mapper

import ch.protonmail.android.mailcategory.domain.model.CategoryLabel
import ch.protonmail.android.mailcategory.domain.model.CategoryLabelId
import ch.protonmail.android.mailcategory.domain.model.CategorySystemLabelId
import ch.protonmail.android.mailcategory.domain.model.CategoryViewStatus
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabel
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalCategoryView
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.mailpagination.data.mapper.toPaginationError
import timber.log.Timber
import uniffi.mail_uniffi.ConversationScrollerCategoryViewResult
import uniffi.mail_uniffi.MessageScrollerCategoryViewResult

fun LocalCategoryView.toCategoryViewStatus(): CategoryViewStatus {
    val categories = available.map { it.toCategoryLabel() }

    return if (categories.isEmpty()) {
        CategoryViewStatus.NotAvailable
    } else {
        CategoryViewStatus.Available(categories)
    }
}

fun LocalCategoryLabel.toCategoryLabel(): CategoryLabel {
    return CategoryLabel(
        id = id.toCategoryLabelId(),
        isActive = enabled,
        systemLabel = systemLabel.toCategorySystemLabel()
    )
}

fun LocalCategoryLabelId.toCategoryLabelId(): CategoryLabelId = CategoryLabelId(this.value.toString())

fun ConversationScrollerCategoryViewResult.toCategoryViewStatus(): CategoryViewStatus = when (this) {
    is ConversationScrollerCategoryViewResult.Ok -> {
        v1.toCategoryViewStatus()
    }

    is ConversationScrollerCategoryViewResult.Error -> {
        CategoryViewStatus.Error(v1.toPaginationError())
    }
}

fun MessageScrollerCategoryViewResult.toCategoryViewStatus(): CategoryViewStatus = when (this) {
    is MessageScrollerCategoryViewResult.Ok -> {
        v1.toCategoryViewStatus()
    }

    is MessageScrollerCategoryViewResult.Error -> {
        CategoryViewStatus.Error(v1.toPaginationError())
    }
}


fun LocalSystemLabel.toCategorySystemLabel() = when (this) {
    LocalSystemLabel.CATEGORY_SOCIAL -> CategorySystemLabelId.Social
    LocalSystemLabel.CATEGORY_PROMOTIONS -> CategorySystemLabelId.Promotions
    LocalSystemLabel.CATEGORY_FORUMS -> CategorySystemLabelId.Forums
    LocalSystemLabel.CATEGORY_UPDATES -> CategorySystemLabelId.Updates
    LocalSystemLabel.CATEGORY_NEWSLETTER -> CategorySystemLabelId.Newsletter
    LocalSystemLabel.CATEGORY_TRANSACTIONS -> CategorySystemLabelId.Transactions
    LocalSystemLabel.CATEGORY_DEFAULT -> CategorySystemLabelId.Primary
    else -> {
        Timber.w("category-view: Mapping from non-category system label ID $this. Fallback to default category.")
        CategorySystemLabelId.Primary
    }
}
