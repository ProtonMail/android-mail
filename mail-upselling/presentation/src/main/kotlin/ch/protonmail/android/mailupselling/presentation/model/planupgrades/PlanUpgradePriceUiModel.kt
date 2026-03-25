/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.model.planupgrades

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class PlanUpgradePriceUiModel(
    val rawAmount: BigDecimal,
    val currencyCode: String
) {

    fun getShorthandFormat(): String {
        val symbol = getCurrencySymbol()
        val isOriginal = currencyCode == symbol
        val formattedAmount = formatAmount(rawAmount)

        // Not all currencies have symbols, fallback to the full currencyCode in that case
        return if (symbol == null || isOriginal) {
            "$currencyCode $formattedAmount" // Space for codes: "XYZ 54" or "XYZ 54.99"
        } else {
            "$symbol$formattedAmount" // No space for symbols: "$54" or "$54.99"
        }
    }

    fun getFullFormat(): String = "$currencyCode ${formatAmount(rawAmount)}"

    private fun formatAmount(amount: BigDecimal): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2

        return format.format(amount)
    }

    private fun getCurrencySymbol(): String? {
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (_: Exception) {
            null
        }
    }
}
