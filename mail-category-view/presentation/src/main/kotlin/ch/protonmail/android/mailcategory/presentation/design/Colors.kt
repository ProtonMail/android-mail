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

package ch.protonmail.android.mailcategory.presentation.design

import androidx.compose.ui.graphics.Color

object CategoryPillColors {
    val Primary = CategoryBaseColors.Iris500
    val Social = CategoryBaseColors.Cyan600
    val Promotions = CategoryBaseColors.Green600
    val Newsletters = CategoryBaseColors.Orange600
    val Transactions = CategoryBaseColors.Red500
    val Updates = CategoryBaseColors.Pink500
    val Forums = CategoryBaseColors.Blue500
    val InactiveBackground = CategoryBaseColors.InactiveBackground

    val ActiveContentLight = Color.White
    val ActiveContentDark = Color.White.copy(alpha = 0.8f)
}

private object CategoryBaseColors {

    val Iris500 = Color(0xFF6D4AFF)
    val Cyan600 = Color(0xFF1C9DBB)
    val Green600 = Color(0xFF3BA36B)
    val Orange600 = Color(0xFFE15922)
    val Red500 = Color(0xFFF2364E)
    val Pink500 = Color(0xFFF22287)
    val Blue500 = Color(0xFF2C96F5)
    val Blue400 = Color(0xFF54A5F3)
    val InactiveBackground = Color(0xFFF4F5F8)
}
