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

package ch.protonmail.android.mailcommon.presentation.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow

fun Modifier.protonFloatingButtonShadow(alpha: Float = 1f): Modifier =
    protonTwoLayerShadow(shape = RoundedCornerShape(percent = 50), alpha = alpha)

fun Modifier.protonTwoLayerShadow(shape: Shape, alpha: Float = 1f): Modifier = this
    .dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = PrimaryShadow.Radius,
            spread = PrimaryShadow.Spread,
            offset = DpOffset(
                PrimaryShadow.OffsetX,
                PrimaryShadow.OffsetY
            ),
            color = PrimaryShadow.Color,
            alpha = alpha
        )
    )
    .dropShadow(
        shape = shape,
        shadow = Shadow(
            radius = SecondaryShadow.Radius,
            spread = SecondaryShadow.Spread,
            offset = DpOffset(
                SecondaryShadow.OffsetX,
                SecondaryShadow.OffsetY
            ),
            color = SecondaryShadow.Color,
            alpha = alpha
        )
    )

private object PrimaryShadow {
    val Radius = 3.dp
    val Spread = 0.dp

    val OffsetX = 0.dp
    val OffsetY = 1.dp

    val Color = Color(0x4D000000)
}

private object SecondaryShadow {
    val Radius = 8.dp
    val Spread = 3.dp

    val OffsetX = 0.dp
    val OffsetY = 4.dp

    val Color = Color(0x26000000)
}
