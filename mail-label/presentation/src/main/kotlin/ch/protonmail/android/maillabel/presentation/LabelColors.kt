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

package ch.protonmail.android.maillabel.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun getLabelColors(): Array<Color> {
    return arrayOf(
        LabelColors.PurpleBase,
        LabelColors.EnzianBase,
        LabelColors.PinkBase,
        LabelColors.PlumBase,
        LabelColors.StrawberryBase,
        LabelColors.CeriseBase,
        LabelColors.CarrotBase,
        LabelColors.CopperBase,
        LabelColors.SaharaBase,
        LabelColors.SoilBase,
        LabelColors.SlateBlueBase,
        LabelColors.CobaltBase,
        LabelColors.PacificBase,
        LabelColors.OceanBase,
        LabelColors.ReefBase,
        LabelColors.PineBase,
        LabelColors.FernBase,
        LabelColors.ForestBase,
        LabelColors.OliveBase,
        LabelColors.PickleBase
    )
}

@SuppressWarnings("ImplicitDefaultLocale", "MagicNumber")
fun Int.hexToString() = String.format("#%06X", 0xFFFFFF and this)

fun Color.getHexStringFromColor() = this.toArgb().hexToString()

fun String.getColorFromHexString() = Color(android.graphics.Color.parseColor(this))

object LabelColors {
    val PurpleBase = Color(0xFF8080FF)
    val EnzianBase = Color(0xFF5252CC)
    val PinkBase = Color(0xFFDB60D6)
    val PlumBase = Color(0xFFA839A4)
    val StrawberryBase = Color(0xFFEC3E7C)
    val CeriseBase = Color(0xFFBA1E55)
    val CarrotBase = Color(0xFFF78400)
    val CopperBase = Color(0xFFC44800)
    val SaharaBase = Color(0xFF936D58)
    val SoilBase = Color(0xFF54473F)
    val SlateBlueBase = Color(0xFF415DF0)
    val CobaltBase = Color(0xFF273EB2)
    val PacificBase = Color(0xFF179FD9)
    val OceanBase = Color(0xFF0A77A6)
    val ReefBase = Color(0xFF1DA583)
    val PineBase = Color(0xFF0F735A)
    val FernBase = Color(0xFF3CBB3A)
    val ForestBase = Color(0xFF258723)
    val OliveBase = Color(0xFFB4A40E)
    val PickleBase = Color(0xFF807304)
}
