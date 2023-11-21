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

package ch.protonmail.android.maildetail.presentation.model

data class SubjectHeaderTransform(
    val headerHeightPx: Float,
    val yOffsetPx: Float,
    private val minOffsetPxForAlphaChange: Float
) {

    val alpha: Float
        get() = calculateAlpha()

    private fun calculateAlpha(): Float {

        val remainingHeight = headerHeightPx + yOffsetPx
        return when {
            remainingHeight > minOffsetPxForAlphaChange -> 1f
            remainingHeight <= 0 -> 0f
            else -> remainingHeight / minOffsetPxForAlphaChange
        }
    }

    fun copyWithUpdatedHeaderHeight(newHeaderHeightPx: Float): SubjectHeaderTransform =
        copy(headerHeightPx = newHeaderHeightPx)

    fun copyWithUpdatedYOffset(newYOffset: Float): SubjectHeaderTransform = copy(yOffsetPx = newYOffset)

    companion object {

        const val minOffsetForAlphaChangeDp = 48f
    }
}
