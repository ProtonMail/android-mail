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

package ch.protonmail.android.mailspotlight.data.repository

import arrow.core.right
import ch.protonmail.android.mailspotlight.data.local.FeatureSpotlightLocalDataSource
import ch.protonmail.android.mailspotlight.domain.model.FeatureSpotlightDisplay
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FeatureSpotlightRepositoryImplTest {

    private val userId = UserId("user-id")
    private val datasource = mockk<FeatureSpotlightLocalDataSource>()
    private lateinit var repository: FeatureSpotlightRepositoryImpl

    @BeforeTest
    fun setUp() {
        repository = FeatureSpotlightRepositoryImpl(datasource)
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `observe proxies to local data source`() {
        // Given
        val expectedFlow = flowOf(FeatureSpotlightDisplay(show = true).right())
        every { datasource.observe(userId) } returns expectedFlow

        // When
        val result = repository.observe(userId)

        // Then
        verify(exactly = 1) { datasource.observe(userId) }
        assertEquals(expectedFlow, result)
    }

    @Test
    fun `save proxies to local data source`() = runTest {
        // Given
        val expectedResult = Unit.right()
        coEvery { datasource.save(userId) } returns expectedResult

        // When
        val result = repository.save(userId)

        // Then
        coVerify(exactly = 1) { datasource.save(userId) }
        assertEquals(expectedResult, result)
    }
}
