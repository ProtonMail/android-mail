/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.design.compose.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class ProtonShapes(
    val small: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Small),
    val medium: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Medium),
    val large: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Large),
    val mediumLarge: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.MediumLarge),
    val extraLarge: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge),
    val huge: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge),
    val jumbo: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Jumbo),
    val massive: CornerBasedShape = RoundedCornerShape(ProtonDimens.CornerRadius.Massive),
    val bottomSheet: Shape = RoundedCornerShape(
        topStart = ProtonDimens.LargeCornerRadius,
        topEnd = ProtonDimens.LargeCornerRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),

    val conversation: CornerBasedShape = RoundedCornerShape(
        topStart = ProtonDimens.ExtraLargeCornerRadius,
        topEnd = ProtonDimens.ExtraLargeCornerRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
)

val LocalShapes = staticCompositionLocalOf { ProtonShapes() }
