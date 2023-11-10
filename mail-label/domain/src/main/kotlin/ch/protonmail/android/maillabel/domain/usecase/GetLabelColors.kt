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

package ch.protonmail.android.maillabel.domain.usecase

import javax.inject.Inject

class GetLabelColors @Inject constructor() {

    operator fun invoke(): List<String> {
        return listOf(
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

    object LabelColors {
        const val PurpleBase = "#FF8080FF"
        const val EnzianBase = "#FF5252CC"
        const val PinkBase = "#FFDB60D6"
        const val PlumBase = "#FFA839A4"
        const val StrawberryBase = "#FFEC3E7C"
        const val CeriseBase = "#FFBA1E55"
        const val CarrotBase = "#FFF78400"
        const val CopperBase = "#FFC44800"
        const val SaharaBase = "#FF936D58"
        const val SoilBase = "#FF54473F"
        const val SlateBlueBase = "#FF415DF0"
        const val CobaltBase = "#FF273EB2"
        const val PacificBase = "#FF179FD9"
        const val OceanBase = "#FF0A77A6"
        const val ReefBase = "#FF1DA583"
        const val PineBase = "#FF0F735A"
        const val FernBase = "#FF3CBB3A"
        const val ForestBase = "#FF258723"
        const val OliveBase = "#FFB4A40E"
        const val PickleBase = "#FF807304"
    }
}
