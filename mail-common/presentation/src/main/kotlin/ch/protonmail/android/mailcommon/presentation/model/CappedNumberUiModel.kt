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

package ch.protonmail.android.mailcommon.presentation.model

import androidx.compose.runtime.Stable

private const val DEFAULT_CAP = 999

@Stable
sealed interface CappedNumberUiModel {

    data object Empty : CappedNumberUiModel

    data class Exact(val value: Int) : CappedNumberUiModel

    data class Capped(val cap: Int = DEFAULT_CAP) : CappedNumberUiModel

    companion object {

        val Zero: CappedNumberUiModel = Exact(0)
    }
}

enum class NullCountPolicy { Empty, Zero }
enum class ZeroCountPolicy { KeepZero, Empty }

fun Int?.toCappedNumberUiModel(
    cap: Int = DEFAULT_CAP,
    nullPolicy: NullCountPolicy = NullCountPolicy.Zero,
    zeroPolicy: ZeroCountPolicy = ZeroCountPolicy.KeepZero
): CappedNumberUiModel {
    val value = this ?: return when (nullPolicy) {
        NullCountPolicy.Empty -> CappedNumberUiModel.Empty
        NullCountPolicy.Zero -> CappedNumberUiModel.Zero
    }

    if (value == 0 && zeroPolicy == ZeroCountPolicy.Empty) {
        return CappedNumberUiModel.Empty
    }

    return if (value <= cap) {
        CappedNumberUiModel.Exact(value)
    } else {
        CappedNumberUiModel.Capped(cap)
    }
}

fun CappedNumberUiModel.asDisplayText(): String = when (this) {
    CappedNumberUiModel.Empty -> ""
    is CappedNumberUiModel.Exact -> "$value"
    is CappedNumberUiModel.Capped -> "$cap+"
}

fun CappedNumberUiModel.isEmpty(): Boolean = this is CappedNumberUiModel.Empty
