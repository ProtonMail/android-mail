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
        GRADIENT_STOP_FIRST to LightGradientPurple400.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_SECOND to LightGradientIris200.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_THIRD to LightGradientIris150.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_BACKGROUND_NORM to ProtonTheme.colors.backgroundNorm.copy(alpha = GRADIENT_ALPHA)
    )
)

@Composable
private fun darkGradient() = Brush.verticalGradient(
    colorStops = arrayOf(
        GRADIENT_STOP_FIRST to DarkGradientMagenta.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_SECOND to DarkGradientViolet.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_THIRD to DarkGradientIris.copy(alpha = GRADIENT_ALPHA),
        GRADIENT_STOP_BACKGROUND_NORM to ProtonTheme.colors.backgroundNorm.copy(alpha = GRADIENT_ALPHA)
    )
)

private const val GRADIENT_STOP_FIRST = 0f
private const val GRADIENT_STOP_SECOND = 0.2383f
private const val GRADIENT_STOP_THIRD = 0.3348f
private const val GRADIENT_STOP_BACKGROUND_NORM = 0.5913f
private const val GRADIENT_ALPHA = 0.3f

private val LightGradientPurple400 = Color(0xFFB767F4)
private val LightGradientIris200 = Color(0xFFB9BEFA)
private val LightGradientIris150 = Color(0xFFD4D9FB)

private val DarkGradientMagenta = Color(0xFF8112A9)
private val DarkGradientViolet = Color(0xFF5117C1)
private val DarkGradientIris = Color(0xFF370F83)
