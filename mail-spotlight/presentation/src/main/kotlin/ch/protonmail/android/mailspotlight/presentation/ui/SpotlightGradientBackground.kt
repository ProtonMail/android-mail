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

package ch.protonmail.android.mailspotlight.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import ch.protonmail.android.design.compose.theme.ProtonTheme

@Composable
internal fun SpotlightGradientBackground(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val gradientBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            GRADIENT_STOP_BRAND_NORM to ProtonTheme.colors.brandNorm.copy(alpha = GRADIENT_ALPHA),
            GRADIENT_STOP_BRAND_MINUS_10 to ProtonTheme.colors.brandMinus10.copy(alpha = GRADIENT_ALPHA),
            GRADIENT_STOP_BRAND_MINUS_20 to ProtonTheme.colors.brandMinus20.copy(alpha = GRADIENT_ALPHA),
            GRADIENT_STOP_BACKGROUND_NORM to ProtonTheme.colors.backgroundNorm.copy(alpha = GRADIENT_ALPHA)
        )
    )

    Column(
        modifier = modifier.background(gradientBrush),
        content = content
    )
}

private const val GRADIENT_STOP_BRAND_NORM = 0f
private const val GRADIENT_STOP_BRAND_MINUS_10 = 0.16f
private const val GRADIENT_STOP_BRAND_MINUS_20 = 0.48f
private const val GRADIENT_STOP_BACKGROUND_NORM = 0.72f
private const val GRADIENT_ALPHA = 0.24f
