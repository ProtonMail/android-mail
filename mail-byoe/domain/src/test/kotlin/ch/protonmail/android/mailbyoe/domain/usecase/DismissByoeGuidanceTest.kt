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
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class DismissByoeGuidanceTest {

    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository>()
    private val dismissByoeGuidance = DismissByoeGuidance(byoeGuidanceRepository)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given the guidance, when it is dismissed, then the guidance records it`() = runTest {
        // Given
        coEvery { byoeGuidanceRepository.markGuidanceDismissed(userId, ByoeSurface.Spotlight) } returns Unit.right()

        // When
        dismissByoeGuidance(userId, ByoeSurface.Spotlight)

        // Then
        coVerify(exactly = 1) { byoeGuidanceRepository.markGuidanceDismissed(userId, ByoeSurface.Spotlight) }
    }

    private companion object {

        val userId = UserId("user")
    }
}
