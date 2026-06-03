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

package ch.protonmail.android.mailupselling.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpsellRatingTriggerRepositoryImplTest {

    private val repository = UpsellRatingTriggerRepositoryImpl()

    @Test
    fun `should emit unit to a subscribed observer when upsell success is emitted`() = runTest {
        repository.observeUpsellSuccess().test {
            // When
            repository.emitUpsellSuccess()

            // Then
            assertEquals(Unit, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `should not replay a previous emission to a late subscriber`() = runTest {
        // Given
        repository.emitUpsellSuccess()

        // When
        repository.observeUpsellSuccess().test {
            // Then
            expectNoEvents()
        }
    }
}
