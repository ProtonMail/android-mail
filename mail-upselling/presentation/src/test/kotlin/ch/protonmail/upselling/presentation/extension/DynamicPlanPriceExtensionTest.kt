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

package ch.protonmail.upselling.presentation.extension

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.extension.normalizedPrice
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import org.junit.Test
import kotlin.test.assertEquals

internal class DynamicPlanPriceExtensionTest {

    @Test
    fun `should return the current price as float, divided by 100`() {
        // Given
        val value = 19_999
        val price = DynamicPlanPrice("id", "CHF", value)
        val expectedValue = TextUiModel.Text("199.99")

        // When
        val actual = price.normalizedPrice(cycle = 1)

        // Then
        assertEquals(expectedValue, actual)
    }

    @Test
    fun `should return 0 if the division fails`() {
        val value = 19_999
        val price = DynamicPlanPrice("id", "CHF", value)
        val expectedValue = TextUiModel.Text("0")

        // When
        val actual = price.normalizedPrice(cycle = 0)

        // Then
        assertEquals(expectedValue, actual)
    }
}
