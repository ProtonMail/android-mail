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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow

fun Modifier.protonFloatingButtonShadow(): Modifier = this
    .dropShadow(
        shape = RoundedCornerShape(percent = 50),
        shadow = Shadow(
            radius = FabPrimaryShadow.Radius,
            spread = FabPrimaryShadow.Spread,
            offset = DpOffset(
                FabPrimaryShadow.OffsetX,
                FabPrimaryShadow.OffsetY
            ),
            color = FabPrimaryShadow.Color
        )
    )
    .dropShadow(
        shape = RoundedCornerShape(percent = 50),
        shadow = Shadow(
            radius = FabSecondaryShadow.Radius,
            spread = FabSecondaryShadow.Spread,
            offset = DpOffset(
                FabSecondaryShadow.OffsetX,
                FabSecondaryShadow.OffsetY
            ),
            color = FabSecondaryShadow.Color
        )
    )

private object FabPrimaryShadow {
    val Radius = 3.dp
    val Spread = 0.dp

    val OffsetX = 0.dp
    val OffsetY = 1.dp

    val Color = Color(0x4D000000)
}

private object FabSecondaryShadow {
    val Radius = 8.dp
    val Spread = 3.dp

    val OffsetX = 0.dp
    val OffsetY = 4.dp

    val Color = Color(0x26000000)
}
