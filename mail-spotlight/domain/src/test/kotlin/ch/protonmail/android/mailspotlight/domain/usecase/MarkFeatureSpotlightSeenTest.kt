/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailspotlight.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailspotlight.domain.repository.FeatureSpotlightRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MarkFeatureSpotlightSeenTest {

    private val userId = UserId("user-id")
    private val featureSpotlightRepository = mockk<FeatureSpotlightRepository>()
    private lateinit var markFeatureSpotlightSeen: MarkFeatureSpotlightSeen

    @BeforeTest
    fun setUp() {
        markFeatureSpotlightSeen = MarkFeatureSpotlightSeen(featureSpotlightRepository)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `invoke proxies to repository save`() = runTest {
        // Given
        val expectedResult = Unit.right()
        coEvery { featureSpotlightRepository.save(userId) } returns expectedResult

        // When
        val result = markFeatureSpotlightSeen(userId)

        // Then
        coVerify(exactly = 1) { featureSpotlightRepository.save(userId) }
        assertEquals(expectedResult, result)
    }
}
