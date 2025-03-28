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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin

@JvmInline
value class InsertedPin(val digits: List<Int>) {

    override fun toString() = digits.joinToString(separator = "")

    fun appendDigit(value: Int) = InsertedPin(digits.append(value))
    fun deleteLastDigit() = InsertedPin(digits.deleteLast())
    fun hasValidLength() = digits.size in PinDigitLengthMinimum..PinDigitLengthLimit
    fun isMaxLength() = digits.size == PinDigitLengthLimit
    fun isNotEmpty() = digits.isNotEmpty()

    private fun List<Int>.append(value: Int): List<Int> = toMutableList().apply { add(value) }
    private fun List<Int>.deleteLast(): List<Int> = toMutableList().apply { removeAt(lastIndex) }

    companion object {

        private const val PinDigitLengthMinimum = 4
        private const val PinDigitLengthLimit = 8

        val Empty = InsertedPin(emptyList())
    }
}
