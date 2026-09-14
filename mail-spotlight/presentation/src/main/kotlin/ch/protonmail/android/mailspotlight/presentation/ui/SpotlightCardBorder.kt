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

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonTheme

/**
 * The shape shared by every spotlight card and by [spotlightCardBorder].
 *
 * Both cards read this so the fill and the border cannot drift apart.
 */
internal val spotlightCardShape: CornerBasedShape
    @Composable get() = ProtonTheme.shapes.huge

/**
 * The hairline border on the spotlight cards, fading out towards the bottom of the card.
 *
 * The gradient runs top to bottom and reaches full transparency at 60% of the card's height, so the
 * lower half of the card has no visible outline at all.
 */
@Composable
internal fun Modifier.spotlightCardBorder(): Modifier = border(
    width = CardBorderWidth,
    brush = Brush.verticalGradient(
        colorStops = arrayOf(
            CARD_BORDER_STOP_VISIBLE to SpotlightColors.cardBorder,
            CARD_BORDER_STOP_TRANSPARENT to SpotlightColors.cardBorder.copy(alpha = 0f)
        )
    ),
    shape = spotlightCardShape
)

private val CardBorderWidth = 1.dp

private const val CARD_BORDER_STOP_VISIBLE = 0.0007f
private const val CARD_BORDER_STOP_TRANSPARENT = 0.5983f
