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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.mailupselling.presentation.R
import me.proton.core.compose.theme.ProtonColors
import me.proton.core.compose.theme.ProtonTheme

internal object UpsellingDimens {

    val ButtonCornerRadius: Int
        @Composable
        get() = 25.dp.dpToPx()

    val ButtonHeight: Int
        @Composable
        get() = 36.dp.dpToPx()

    val EntitlementImageItemSize = 20.dp

    val DiscountTagVerticalOffset = (-8).dp
    const val DiscountTagDefaultZIndex = 1f

    const val UpsellingPaymentItemWeight = 1f

    val CurrencyDivider = 2.dp
}

internal object UpsellingColors {

    val BottomSheetContentColors: ProtonColors?
        @Composable
        get() = ProtonTheme.colors.sidebarColors

    val BottomSheetBackgroundColor = R.color.haiti
    val DiscountTagColorStops = arrayOf(0.0f to Color(0xFFA792FF), 0.5f to Color(0xFF27DDB1))
    val EntitlementsRowDivider = Color.White.copy(alpha = 0.08f)
    val SecondaryButtonBackground = android.graphics.Color.parseColor("#33FFFFFF")
    val PaymentDiscountedItemBackground = Color(0xFF221B42)
    val PaymentDiscountedItemBorder = Color(0xFFA196FC)
    val PaymentStandardItemBackground = Color.White.copy(alpha = 0.04f)
    val PaymentStandardItemBorder = Color.White.copy(alpha = 0.08f)
}
