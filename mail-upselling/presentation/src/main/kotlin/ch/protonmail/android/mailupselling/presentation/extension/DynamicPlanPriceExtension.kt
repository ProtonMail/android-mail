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
import ch.protonmail.android.mailupselling.presentation.R
import me.proton.core.plan.domain.entity.DynamicPlanPrice

internal fun DynamicPlanPrice.normalizedPrice(cycle: Int): TextUiModel {
    val actualPrice = current.normalized(cycle)
    return TextUiModel.Text(actualPrice.toDecimalString())
}

internal fun DynamicPlanPrice.promoPrice(cycle: Int): TextUiModel {
    val actualPrice = current.normalized(cycle)
    return TextUiModel
        .TextResWithArgs(R.string.upselling_get_button_promotional, listOf(currency, actualPrice.toDecimalString()))
}

internal fun DynamicPlanPrice.totalPrice(): Float = current.toActualPrice()
internal fun DynamicPlanPrice.totalDefaultPrice(): Float = (default ?: 0).toActualPrice()
internal fun DynamicPlanPrice.totalDefaultPriceNullable(): Float? = default?.toActualPrice()

@Suppress("MagicNumber")
internal fun Int.toActualPrice() = (this / 100f).takeIf {
    it != Float.POSITIVE_INFINITY && it != Float.NEGATIVE_INFINITY
} ?: 0f

@Suppress("MagicNumber")
internal fun Int.normalized(cycle: Int) = (this / 100f / cycle).takeIf {
    it != Float.POSITIVE_INFINITY && it != Float.NEGATIVE_INFINITY
} ?: 0f
