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

package ch.protonmail.android.mailbyoe.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailupselling.domain.usecase.HasInAppNotificationsEnabled
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GetByoeSpotlightTreatmentTest {

    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository> {
        coEvery { getSpotlightTreatment(userId) } returns ByoeTreatment.Treatment2.right()
    }
    private val hasInAppNotificationsEnabled = mockk<HasInAppNotificationsEnabled> {
        coEvery { this@mockk.invoke(userId) } returns true.right()
    }
    private val getByoeSpotlightTreatment =
        GetByoeSpotlightTreatment(byoeGuidanceRepository, hasInAppNotificationsEnabled)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an existing user in a treatment group, when getting the treatment, then it is returned`() = runTest {
        // When
        val result = getByoeSpotlightTreatment(userId)

        // Then
        assertEquals(ByoeTreatment.Treatment2, result)
    }

    @Test
    fun `given in-app notifications are turned off, when getting the treatment, then there is none`() = runTest {
        // Given
        coEvery { hasInAppNotificationsEnabled(userId) } returns false.right()

        // When
        val result = getByoeSpotlightTreatment(userId)

        // Then
        assertNull(result)
    }

    private companion object {

        val userId = UserId("user")
    }
}
