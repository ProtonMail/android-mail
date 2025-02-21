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

package ch.protonmail.android.mailupselling.presentation.model.dynamicplans

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R

enum class DynamicPlanCycle(val months: Int) {
    Monthly(1),
    Yearly(12);

    fun cycleStringValue(): TextUiModel = when (this) {
        Monthly -> TextUiModel.TextRes(R.string.upselling_month)
        Yearly -> TextUiModel.TextRes(R.string.upselling_year)
    }

    fun cyclePlanName(): TextUiModel = when (this) {
        Monthly -> TextUiModel.TextRes(R.string.upselling_select_plan_month)
        Yearly -> TextUiModel.TextRes(R.string.upselling_select_plan_year)
    }
}
