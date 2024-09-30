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

package ch.protonmail.android.mailupselling.presentation.extension

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import me.proton.core.plan.domain.entity.DynamicPlanPrice

internal fun DynamicPlanPrice.normalizedPrice(cycle: Int): TextUiModel {
    val actualPrice = (current / 100f / cycle).takeIf {
        it != Float.POSITIVE_INFINITY && it != Float.NEGATIVE_INFINITY
    } ?: 0f
    return TextUiModel.Text(actualPrice.toDecimalString())
}

internal fun DynamicPlanPrice.totalPrice(): String {
    val actualPrice = (current / 100f).takeIf {
        it != Float.POSITIVE_INFINITY && it != Float.NEGATIVE_INFINITY
    } ?: 0f
    return actualPrice.toDecimalString()
}
