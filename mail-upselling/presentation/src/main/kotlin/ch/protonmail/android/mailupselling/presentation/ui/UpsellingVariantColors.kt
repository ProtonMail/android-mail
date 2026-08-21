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

package ch.protonmail.android.mailupselling.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.isNightMode
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.BlackFriday
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.FallPromo
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.SpringPromo
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.SummerCampaign
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.coloredBorderBrush

@Immutable
internal data class UpsellingVariantColors(
    // Comparison table
    val checkmarkTint: Color,
    val checkmarkBackground: Color,
    val plusBadgeBorderBrush: Brush,
    val plusBadgeBackground: Color,
    val plusBadgeTextColor: Color,
    val tableTextColor: Color,
    val tableDividerColor: Color,
    val tableHighlightColor: Color,
    // Cycle card
    val cycleCardContainerColor: Color,
    val cycleCardBorderColor: Color,
    val cycleCardTextColor: Color,
    val cycleCardDiscountBadgeBackground: Color,
    val cycleCardDiscountBadgeTextColor: Color,
    val cycleCardDiscountBadgeHasShadow: Boolean,
    // Footer
    val autoRenewalColor: Color
)

@Composable
internal fun planUpgradeVariantColors(variant: PlanUpgradeVariant): UpsellingVariantColors = when {
    variant is PlanUpgradeVariant.BlackFriday -> UpsellingVariantColors(
        checkmarkTint = Color.Black,
        checkmarkBackground = Color.White,
        plusBadgeBorderBrush = BlackFriday.borderBrush,
        plusBadgeBackground = Color.Black.copy(alpha = 0.20f),
        plusBadgeTextColor = Color.White,
        tableTextColor = Color.White,
        tableDividerColor = Color.White.copy(alpha = 0.12f),
        tableHighlightColor = UpsellingLayoutValues.ComparisonTable.highlightBarColor,
        cycleCardContainerColor = Color.Black.copy(alpha = 0.2f),
        cycleCardBorderColor = Color.White,
        cycleCardTextColor = Color.White,
        cycleCardDiscountBadgeBackground = Color.White.copy(alpha = 0.12f),
        cycleCardDiscountBadgeTextColor = Color.White,
        cycleCardDiscountBadgeHasShadow = false,
        autoRenewalColor = Color.White
    )

    variant is PlanUpgradeVariant.SpringPromo -> {
        val nightMode = isNightMode()
        val accentColor = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30
        UpsellingVariantColors(
            checkmarkTint = if (nightMode) Color.Black else Color.White,
            checkmarkBackground = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30,
            plusBadgeBorderBrush = SpringPromo.borderBrush,
            plusBadgeBackground = Color.Transparent,
            plusBadgeTextColor = accentColor,
            tableTextColor = accentColor,
            tableDividerColor = accentColor.copy(alpha = 0.12f),
            tableHighlightColor = UpsellingLayoutValues.ComparisonTable.highlightBarColor,
            cycleCardContainerColor = Color.Transparent,
            cycleCardBorderColor = accentColor,
            cycleCardTextColor = accentColor,
            cycleCardDiscountBadgeBackground = if (nightMode) Color.White.copy(alpha = 0.20f) else Color.White,
            cycleCardDiscountBadgeTextColor = accentColor,
            cycleCardDiscountBadgeHasShadow = true,
            autoRenewalColor = accentColor
        )
    }

    variant is PlanUpgradeVariant.FallPromo -> {
        val nightMode = isNightMode()
        val textColor = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30
        UpsellingVariantColors(
            checkmarkTint = if (nightMode) Color.Black else Color.White,
            checkmarkBackground = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30,
            plusBadgeBorderBrush = FallPromo.borderBrush,
            plusBadgeBackground = FallPromo.backgroundColor(),
            plusBadgeTextColor = textColor,
            tableTextColor = textColor,
            tableDividerColor = textColor.copy(alpha = 0.12f),
            tableHighlightColor = textColor.copy(alpha = 0.08f),
            cycleCardContainerColor = Color.Transparent,
            cycleCardBorderColor = textColor,
            cycleCardTextColor = textColor,
            cycleCardDiscountBadgeBackground = FallPromo.accentColor,
            cycleCardDiscountBadgeTextColor = Color.White,
            cycleCardDiscountBadgeHasShadow = false,
            autoRenewalColor = textColor
        )
    }

    variant is PlanUpgradeVariant.SummerCampaign -> {
        val nightMode = isNightMode()
        val textColor = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30
        UpsellingVariantColors(
            checkmarkTint = if (nightMode) Color.Black else Color.White,
            checkmarkBackground = if (nightMode) Color.White else ProtonTheme.colors.brandPlus30,
            plusBadgeBorderBrush = SummerCampaign.borderBrush,
            plusBadgeBackground = Color.Transparent,
            plusBadgeTextColor = textColor,
            tableTextColor = textColor,
            tableDividerColor = textColor.copy(alpha = 0.12f),
            tableHighlightColor = UpsellingLayoutValues.ComparisonTable.highlightBarColor,
            cycleCardContainerColor = Color.Transparent,
            cycleCardBorderColor = textColor,
            cycleCardTextColor = textColor,
            cycleCardDiscountBadgeBackground = SummerCampaign.accentColor,
            cycleCardDiscountBadgeTextColor = Color.White,
            cycleCardDiscountBadgeHasShadow = false,
            autoRenewalColor = textColor
        )
    }

    else -> UpsellingVariantColors(
        checkmarkTint = Color.Black,
        checkmarkBackground = Color.White,
        plusBadgeBorderBrush = coloredBorderBrush,
        plusBadgeBackground = Color.Black.copy(alpha = 0.20f),
        plusBadgeTextColor = Color.White,
        tableTextColor = Color.White,
        tableDividerColor = Color.White.copy(alpha = 0.12f),
        tableHighlightColor = UpsellingLayoutValues.ComparisonTable.highlightBarColor,
        cycleCardContainerColor = Color.Black.copy(alpha = 0.2f),
        cycleCardBorderColor = Color.White,
        cycleCardTextColor = Color.White,
        cycleCardDiscountBadgeBackground = Color.White.copy(alpha = 0.12f),
        cycleCardDiscountBadgeTextColor = Color.White,
        cycleCardDiscountBadgeHasShadow = false,
        autoRenewalColor = Color.White
    )
}
