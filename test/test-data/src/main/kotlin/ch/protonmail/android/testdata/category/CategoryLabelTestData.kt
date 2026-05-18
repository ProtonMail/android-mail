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

package ch.protonmail.android.testdata.category

import ch.protonmail.android.mailcategory.domain.model.CategoryLabel
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.mailcategory.domain.model.CategorySystemLabelId

object CategoryLabelTestData {

    val primary = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Primary.labelId.id),
        hasUnseen = true,
        isActive = true,
        systemLabel = CategorySystemLabelId.Primary
    )

    val social = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Social.labelId.id),
        hasUnseen = true,
        isActive = false,
        systemLabel = CategorySystemLabelId.Social
    )

    val promotions = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Promotions.labelId.id),
        isActive = false,
        systemLabel = CategorySystemLabelId.Promotions
    )

    val updates = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Updates.labelId.id),
        isActive = false,
        systemLabel = CategorySystemLabelId.Updates
    )

    val forums = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Forums.labelId.id),
        isActive = false,
        systemLabel = CategorySystemLabelId.Forums
    )

    val newsletter = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Newsletter.labelId.id),
        isActive = false,
        systemLabel = CategorySystemLabelId.Newsletter
    )

    val transactions = CategoryLabel(
        id = CategoryLabelId(CategorySystemLabelId.Transactions.labelId.id),
        isActive = false,
        systemLabel = CategorySystemLabelId.Transactions
    )

    val all = listOf(
        primary,
        social,
        promotions,
        updates,
        forums,
        newsletter,
        transactions
    )
}
