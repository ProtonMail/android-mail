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

package ch.protonmail.android.mailcategory.domain.model

import ch.protonmail.android.maillabel.domain.model.LabelId

enum class CategorySystemLabelId(val labelId: LabelId) {
    Social(LabelId("20")),
    Promotions(LabelId("21")),
    Updates(LabelId("22")),
    Forums(LabelId("23")),
    Primary(LabelId("24")),
    Newsletter(LabelId("25")),
    Transactions(LabelId("26"));

    companion object {
        private val map = entries.associateBy { it.labelId.id }

        fun enumOf(value: String?): CategorySystemLabelId? = map[value]
    }
}
