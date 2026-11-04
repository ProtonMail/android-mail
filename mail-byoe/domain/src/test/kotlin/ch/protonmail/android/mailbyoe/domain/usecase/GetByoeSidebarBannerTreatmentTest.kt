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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GetByoeSidebarBannerTreatmentTest {

    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository>()
    private val getByoeSidebarBannerTreatment = GetByoeSidebarBannerTreatment(byoeGuidanceRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the guidance shows the banner, when asking, then its treatment is returned`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.getSidebarBannerTreatment(userId) } returns ByoeTreatment.Treatment1.right()

        // When
        val result = getByoeSidebarBannerTreatment(userId)

        // Then
        assertEquals(ByoeTreatment.Treatment1, result)
    }

    @Test
    fun `given the guidance hides the banner, when asking, then there is none`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.getSidebarBannerTreatment(userId) } returns null.right()

        // When
        val result = getByoeSidebarBannerTreatment(userId)

        // Then
        assertNull(result)
    }

    @Test
    fun `given the guidance fails, when asking, then there is none`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.getSidebarBannerTreatment(userId) } returns DataError.Local.NoUserSession.left()

        // When
        val result = getByoeSidebarBannerTreatment(userId)

        // Then
        assertNull(result)
    }

    private companion object {

        val userId = UserId("user")
    }
}
