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
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.design.compose.theme.ProtonTheme

@Composable
internal fun SpotlightGradientBackground(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val gradientBrush = if (ProtonTheme.colors.isDark) darkGradient() else lightGradient()

    Column(
        modifier = modifier
            .background(ProtonTheme.colors.backgroundNorm)
            .background(gradientBrush),
        content = content
    )
}

@Composable
private fun lightGradient() = Brush.verticalGradient(
    colorStops = arrayOf(
        GRADIENT_STOP_BRAND_NORM to ProtonTheme.colors.brandNorm.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_BRAND_MINUS_10 to ProtonTheme.colors.brandMinus10.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_BRAND_MINUS_20 to ProtonTheme.colors.brandMinus20.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_BACKGROUND_NORM to ProtonTheme.colors.backgroundNorm.copy(alpha = GRADIENT_ALPHA)
    )
)

@Composable
private fun darkGradient() = Brush.verticalGradient(
    colorStops = arrayOf(
        DARK_GRADIENT_STOP_MAGENTA to DarkGradientMagenta.copy(alpha = DARK_GRADIENT_ALPHA),
        DARK_GRADIENT_STOP_VIOLET to DarkGradientViolet.copy(alpha = DARK_GRADIENT_ALPHA),
        DARK_GRADIENT_STOP_IRIS to DarkGradientIris.copy(alpha = DARK_GRADIENT_ALPHA),
        DARK_GRADIENT_STOP_BACKGROUND_NORM to ProtonTheme.colors.backgroundNorm.copy(alpha = DARK_GRADIENT_ALPHA)
    )
)

private const val GRADIENT_STOP_BRAND_NORM = 0f
private const val GRADIENT_STOP_BRAND_MINUS_10 = 0.16f
private const val GRADIENT_STOP_BRAND_MINUS_20 = 0.48f
private const val GRADIENT_STOP_BACKGROUND_NORM = 0.72f
private const val GRADIENT_ALPHA = 0.24f

private val DarkGradientMagenta = Color(0xFF8112A9)
private val DarkGradientViolet = Color(0xFF5117C1)
private val DarkGradientIris = Color(0xFF370F83)

private const val DARK_GRADIENT_STOP_MAGENTA = 0f
private const val DARK_GRADIENT_STOP_VIOLET = 0.2383f
private const val DARK_GRADIENT_STOP_IRIS = 0.3348f
private const val DARK_GRADIENT_STOP_BACKGROUND_NORM = 0.5913f
private const val DARK_GRADIENT_ALPHA = 0.3f
