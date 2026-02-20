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
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.SpringPromo
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues.coloredBorderBrush

@Immutable
internal data class UpsellingVariantColors(
    // Comparison table
    val checkmarkTint: Color,
    val checkmarkBackground: Color,
    val plusBadgeBorderBrush: Brush,
    val plusBadgeBackground: Color,
    val tableTextColor: Color,
    val tableDividerColor: Color,
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
        tableTextColor = Color.White,
        tableDividerColor = Color.White.copy(alpha = 0.12f),
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
            tableTextColor = accentColor,
            tableDividerColor = accentColor.copy(alpha = 0.12f),
            cycleCardContainerColor = Color.Transparent,
            cycleCardBorderColor = accentColor,
            cycleCardTextColor = accentColor,
            cycleCardDiscountBadgeBackground = if (nightMode) Color.White.copy(alpha = 0.20f) else Color.White,
            cycleCardDiscountBadgeTextColor = accentColor,
            cycleCardDiscountBadgeHasShadow = true,
            autoRenewalColor = accentColor
        )
    }

    else -> UpsellingVariantColors(
        checkmarkTint = Color.White,
        checkmarkBackground = Color.Black.copy(alpha = 0.2f),
        plusBadgeBorderBrush = coloredBorderBrush,
        plusBadgeBackground = Color.Black.copy(alpha = 0.20f),
        tableTextColor = Color.White,
        tableDividerColor = Color.White.copy(alpha = 0.12f),
        cycleCardContainerColor = Color.Black.copy(alpha = 0.2f),
        cycleCardBorderColor = Color.White,
        cycleCardTextColor = Color.White,
        cycleCardDiscountBadgeBackground = Color.White.copy(alpha = 0.12f),
        cycleCardDiscountBadgeTextColor = Color.White,
        cycleCardDiscountBadgeHasShadow = false,
        autoRenewalColor = Color.White
    )
}
