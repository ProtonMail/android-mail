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

package ch.protonmail.android.mailbyoe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.design.compose.theme.ProtonTheme

/**
 * Colors of the full-screen BYOE pages that have no design system token in at least one theme.
 */
internal data class ByoeColors(
    val backgroundTint: Brush,
    val badgeBackground: Color,
    val badgeText: Color,
    val cardBackground: Color,
    val cardBorder: Brush,
    val cardDivider: Color
)

@Composable
@ReadOnlyComposable
internal fun byoeColors(): ByoeColors {
    val colors = ProtonTheme.colors
    return if (colors.isDark) {
        ByoeColors(
            backgroundTint = tint(DarkGradientTop, DarkGradientUpper, DarkGradientLower, DARK_GRADIENT_END_STOP),
            badgeBackground = DarkBadgeBackground,
            badgeText = DarkBadgeText,
            cardBackground = Color.Transparent,
            cardBorder = cardBorder(DarkCardBorder, DarkCardBorder),
            cardDivider = colors.borderNorm
        )
    } else {
        ByoeColors(
            backgroundTint = tint(LightGradientTop, LightGradientUpper, LightGradientLower, LIGHT_GRADIENT_END_STOP),
            badgeBackground = LightBadgeBackground,
            badgeText = LightBadgeText,
            cardBackground = colors.backgroundNorm,
            cardBorder = cardBorder(LightCardBorderTop, LightCardBorderBottom),
            cardDivider = colors.separatorNorm
        )
    }
}

/** The card border fades out from [top] towards the bottom of the card. */
private fun cardBorder(top: Color, bottom: Color): Brush =
    Brush.verticalGradient(colors = listOf(top, bottom.copy(alpha = 0f)))

/** The purple tint at the top of the page, drawn over the normal background. */
private fun tint(
    top: Color,
    upper: Color,
    lower: Color,
    endStop: Float
): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to top,
        GRADIENT_UPPER_STOP to upper,
        GRADIENT_LOWER_STOP to lower,
        endStop to Color.Transparent
    )
)

private const val GRADIENT_UPPER_STOP = 0.238f
private const val GRADIENT_LOWER_STOP = 0.335f
private const val LIGHT_GRADIENT_END_STOP = 0.462f
private const val DARK_GRADIENT_END_STOP = 0.591f
private const val GRADIENT_ALPHA = 0.3f

// The design's purple and iris tints have no design system token.
private val LightGradientTop = Color(0xFFB767F4).copy(alpha = GRADIENT_ALPHA)
private val LightGradientUpper = Color(0xFFB9BEFA).copy(alpha = GRADIENT_ALPHA)
private val LightGradientLower = Color(0xFFD4D9FB).copy(alpha = GRADIENT_ALPHA)
private val DarkGradientTop = Color(0xFF8112A9).copy(alpha = GRADIENT_ALPHA)
private val DarkGradientUpper = Color(0xFF5117C1).copy(alpha = GRADIENT_ALPHA)
private val DarkGradientLower = Color(0xFF370F83).copy(alpha = GRADIENT_ALPHA)
private val DarkBadgeBackground = Color(0xFF370F83)
private val DarkBadgeText = Color(0xFFEDEFFD)
private val LightBadgeBackground = Color(0xFFDDDEFB)
private val LightBadgeText = Color(0xFF311E71)
private val LightCardBorderTop = Color(0xFFDDDEFB)
private val LightCardBorderBottom = Color(0xFFF5F6FE)
private val DarkCardBorder = Color(0xFF50505B)
