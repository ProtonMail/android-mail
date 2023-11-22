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

import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors.LabelColors
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetLabelColorsTest {

    private val getLabelColors = GetLabelColors()

    @Test
    fun `should return label colors`() = runTest {
        val expectedResult = listOf(
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

        // When
        val result = getLabelColors()

        // Then
        assertEquals(expectedResult, result)
    }
}
