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
import ch.protonmail.android.mailcategory.domain.model.CategorySystemLabelId

private sealed interface CategoryPillPalette {
    val primary: Color
    val social: Color
    val promotions: Color
    val updates: Color
    val forums: Color
    val newsletters: Color
    val transactions: Color

    fun colorOf(labelId: CategorySystemLabelId): Color = when (labelId) {
        CategorySystemLabelId.Primary -> primary
        CategorySystemLabelId.Social -> social
        CategorySystemLabelId.Promotions -> promotions
        CategorySystemLabelId.Updates -> updates
        CategorySystemLabelId.Forums -> forums
        CategorySystemLabelId.Newsletter -> newsletters
        CategorySystemLabelId.Transactions -> transactions
    }

    data class Colors(
        override val primary: Color,
        override val social: Color,
        override val promotions: Color,
        override val updates: Color,
        override val forums: Color,
        override val newsletters: Color,
        override val transactions: Color
    ) : CategoryPillPalette

    companion object {
        val Light: CategoryPillPalette = Colors(
            primary = CategoryPillColorsLight.Primary,
            social = CategoryPillColorsLight.Social,
            promotions = CategoryPillColorsLight.Promotions,
            updates = CategoryPillColorsLight.Updates,
            forums = CategoryPillColorsLight.Forums,
            newsletters = CategoryPillColorsLight.Newsletters,
            transactions = CategoryPillColorsLight.Transactions
        )

        val Dark: CategoryPillPalette = Colors(
            primary = CategoryPillColorsDark.Primary,
            social = CategoryPillColorsDark.Social,
            promotions = CategoryPillColorsDark.Promotions,
            updates = CategoryPillColorsDark.Updates,
            forums = CategoryPillColorsDark.Forums,
            newsletters = CategoryPillColorsDark.Newsletters,
            transactions = CategoryPillColorsDark.Transactions
        )
    }
}

object CategoryPillColorsLight {
    val Primary = CategoryBaseColors.Iris500
    val Social = CategoryBaseColors.Cyan600
    val Promotions = CategoryBaseColors.Green600
    val Newsletters = CategoryBaseColors.Orange600
    val Updates = CategoryBaseColors.Pink500
    val Forums = CategoryBaseColors.Blue500
    val Transactions = CategoryBaseColors.Red500
}

object CategoryPillColorsDark {
    val Primary = Color(0xFF9A97F7)
    val Social = Color(0xFF2FBFDD)
    val Promotions = Color(0xFF52C67A)
    val Newsletters = Color(0xFFF78647)
    val Updates = Color(0xFFF4559B)
    val Forums = CategoryBaseColors.Blue500
    val Transactions = CategoryBaseColors.Red500
}

private object CategoryBaseColors {

    val Iris500 = Color(0xFF6D4AFF)
    val Cyan600 = Color(0xFF1C9DBB)
    val Green600 = Color(0xFF3BA36B)
    val Orange600 = Color(0xFFE15922)
    val Red500 = Color(0xFFF2364E)
    val Pink500 = Color(0xFFF22287)
    val Blue500 = Color(0xFF2C96F5)
}

fun CategorySystemLabelId.activeCategoryColor(isDarkMode: Boolean = false): Color =
    (if (isDarkMode) CategoryPillPalette.Dark else CategoryPillPalette.Light).colorOf(this)
