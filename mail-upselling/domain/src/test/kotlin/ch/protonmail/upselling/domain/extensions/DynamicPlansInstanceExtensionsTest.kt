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

package ch.protonmail.upselling.domain.extensions

import java.time.Instant
import ch.protonmail.android.mailupselling.domain.extensions.currentPrice
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DynamicPlansInstanceExtensionsTest {

    @Test
    fun `should return the current price as a float`() {
        // Given
        val instance = DynamicPlanInstance(
            cycle = 12,
            description = "",
            periodEnd = Instant.now(),
            price = mapOf("EUR" to DynamicPlanPrice(id = "id", currency = "EUR", current = 100, default = null))
        )
        val expected = 100f

        // When
        val actual = instance.currentPrice

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return null if the price map is empty`() {
        // Given
        val instance = DynamicPlanInstance(
            cycle = 12,
            description = "",
            periodEnd = Instant.now(),
            price = mapOf()
        )

        // When
        val actual = instance.currentPrice

        // Then
        assertNull(actual)
    }
}
