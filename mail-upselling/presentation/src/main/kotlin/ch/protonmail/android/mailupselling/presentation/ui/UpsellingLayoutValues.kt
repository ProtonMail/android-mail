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

package ch.protonmail.android.mailupselling.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import me.proton.core.compose.theme.ProtonDimens

internal object UpsellingLayoutValues {

    val titleColor = Color.White
    val subtitleColor = Color.White
    val closeButtonSize = 32.dp
    val closeButtonColor = Color.White
    val closeButtonBackgroundColor = Color.White.copy(alpha = 0.08f)

    val SocialProofDescColor = Color(0xFFDFCEFC)
    val CheckMarkEntitlementColor = Color.White.copy(alpha = 0.9f)

    val BlueInteractionNorm = Color(0xFF6D4AFF)

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0C0233),
            Color(0xFF522580)
        )
    )

    val backgroundGradientVariantB = Brush.verticalGradient(
        listOf(
            Color(0xFF1D121D),
            BlueInteractionNorm
        )
    )

    val discountTagColorStops = arrayOf(0.0f to Color(0xFFA792FF), 0.8f to Color(0xFF27DDB1))

    val autoRenewText = Color.White
    val autoRenewTextSize = 12.sp

    const val topSpacingWeight = 0.25f
    const val bottomSpacingWeight = 0.4f

    object ComparisonTable {

        val highlightBarColor = Color.White.copy(alpha = 0.08f)
        val highlightBarShape = RoundedCornerShape(8.dp)
        val spacerHeight = 1.dp
        val spacerBackgroundColor = Color.White.copy(alpha = 0.12f)

        val plusBadgeGradient = Brush.linearGradient(
            listOf(Color(0xFFD8AAFF), BlueInteractionNorm)
        )

        val plusBadgeBackground = Color(0xFF2a1b5a)

        val plusBadgeShape = RoundedCornerShape(6.dp)

        val titleColumnSize = 16.sp
        val textColor = Color.White
        val iconColor = Color.White

        val itemTextSize = 14.sp
    }

    object EntitlementsList {

        val textColor = Color.White
        val iconColor = Color.White

        val rowDivider = Color.White.copy(alpha = 0.08f)
        val imageSize = 20.dp
    }

    object UpsellingPlanButtonsFooter {

        val backgroundColor = Color.White.copy(alpha = 0.08f)
        val spacerHeight = 1.dp
        val spacerColor = Color.White.copy(alpha = 0.24f)

        val paymentButtonBackground = android.graphics.Color.parseColor("#FFFFFF")
    }

    object UpsellingPromoButton {
        val iconColorDark = Color(0xFF8A6EFF)
        val iconColorLight = BlueInteractionNorm
        val bgColor = Color(0x4d8a6eff)
    }

    object SquarePaymentButtons {

        val buttonCornerRadius: Int
            @Composable
            get() = 25.dp.dpToPx()

        val currencyDivider = 2.dp
        val discountTagVerticalOffset = (-8).dp
        val discountTagTextColor = Color(0xFF1B1340)
        const val discountTagDefaultZIndex = 1f

        val highlightedBackgroundColor = Color(0xFF352E52)
        val nonHighlightedBackgroundColor = Color.White.copy(alpha = 0.06f)

        val highlightedBorderColor = Color(0xFFA196FC)
        val nonHighlightedBorderColor = Color.White.copy(alpha = 0.08f)

        val shape = RoundedCornerShape(ProtonDimens.DefaultSpacing)

        val textColor = Color.White
        val subtextColor = Color(0xFFA7A4B5) // CadetBlue

        val SecondaryPaymentButtonBackground = ColorUtils.setAlphaComponent(
            android.graphics.Color.parseColor("#FFFFFF"),
            (0.16f * 255).toInt()
        )
    }

    object RectangularPaymentButtons {

        val textSize = 14.sp
        val textColor = Color.White
        val subtextSize = 12.sp
        val subtextColor = Color(0xFFA7A4B5) // CadetBlue

        val mainPriceTextSize = 16.sp
        val mainPriceTextColor = Color.White

        val discountTagTextColor = Color.White
        val discountTagBackground = Color.White.copy(alpha = 0.12f)
        val discountBadgeShape = RoundedCornerShape(8.dp)
        val discountTextSize = 12.sp

        val outlinedCardContainerColor = Color.Black.copy(alpha = 0.4f)

        val outlinedCardSelectedBorderStroke = BorderStroke(2.dp, Color.White)
        val outlinedCardStandardBorderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    }

    object Onboarding {

        val payButtonCornerRadius: Int
            @Composable
            get() = ProtonDimens.LargeCornerRadius.dpToPx()
    }
}
