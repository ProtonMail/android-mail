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

package ch.protonmail.android.mailupselling.domain.cache

import app.cash.turbine.test
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferList
import ch.protonmail.android.mailupselling.domain.sdk.SdkBackedGetAvailableUpgrades
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

internal class AvailableUpgradesCacheTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAvailableUpgrades = mockk<SdkBackedGetAvailableUpgrades>()
    private lateinit var cache: AvailableUpgradesCache

    private val userId = UserId("test-user-123")
    private val mockProductDetails = listOf<ProductOfferList>(mockk(), mockk())

    @BeforeTest
    fun setup() {
        cache = AvailableUpgradesCache(
            getAvailableUpgrades = getAvailableUpgrades,
            scope = TestScope(mainDispatcherRule.testDispatcher)
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `observe - returns cached data when available`() = runTest {
        // Given
        coEvery { getAvailableUpgrades() } returns mockProductDetails

        // When
        cache.observe(userId).test {
            assertEquals(mockProductDetails, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        // Then
        cache.observe(userId).test {
            assertEquals(mockProductDetails, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        coVerify(exactly = 1) { getAvailableUpgrades() }
    }

    @Test
    fun `observe - reloads data when cache is expired`() = runTest {
        // Given
        val updatedProducts = listOf(mockk<ProductOfferList>())
        coEvery { getAvailableUpgrades() } returns mockProductDetails

        // When (first load)
        cache.observe(userId).test {
            assertEquals(mockProductDetails, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        // Cache expiration
        mockCacheExpiration()
        coEvery { getAvailableUpgrades() } returns updatedProducts

        // Then
        cache.observe(userId).test {
            assertEquals(updatedProducts, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        coVerify(exactly = 2) { getAvailableUpgrades() }
    }

    @Test
    fun `get - returns data immediately when cache is fresh`() = runTest {
        // Given
        coEvery { getAvailableUpgrades() } returns mockProductDetails

        // Pre-populate cache
        cache.observe(userId).first()
        advanceUntilIdle()

        // When
        val result = cache.get(userId)

        // Then
        assertEquals(mockProductDetails, result)
        coVerify(exactly = 1) { getAvailableUpgrades() }
    }

    @Test
    fun `get - triggers reload and returns old data when cache is expired`() = runTest {
        // Given
        val updatedProducts = listOf(mockk<ProductOfferList>())
        coEvery { getAvailableUpgrades() } returnsMany listOf(mockProductDetails, updatedProducts)

        // Pre-populate cache
        cache.observe(userId).first()
        advanceUntilIdle()

        // Cache expiration
        mockCacheExpiration()
        coEvery { getAvailableUpgrades() } returns updatedProducts

        // When
        val result = cache.get(userId)

        // Then
        assertEquals(updatedProducts, result)
        advanceUntilIdle()
        coVerify(exactly = 2) { getAvailableUpgrades() }
    }

    @Test
    fun `get - returns empty list on error`() = runTest {
        // Given
        coEvery { getAvailableUpgrades() } throws Exception("Network error")

        // When
        val result = cache.get(userId)

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `invalidateAll - reloads all cached users`() = runTest {
        // Given
        val userId2 = UserId("test-user-456")
        coEvery { getAvailableUpgrades() } returns mockProductDetails

        // Pre-populate cache for multiple users
        cache.observe(userId).first()
        cache.observe(userId2).first()
        advanceUntilIdle()

        coVerify(exactly = 2) { getAvailableUpgrades() }

        // When
        cache.invalidateAll()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 4) { getAvailableUpgrades() }
    }

    @Test
    fun `multiple concurrent calls for same user share same cache`() = runTest {
        // Given
        coEvery { getAvailableUpgrades() } returns mockProductDetails

        // When - Launch multiple concurrent observers
        val results = mutableListOf<List<ProductOfferList>>()
        val jobs = List(5) {
            launch {
                cache.observe(userId).test {
                    results.add(awaitItem())
                    cancelAndConsumeRemainingEvents()
                }
            }
        }

        advanceUntilIdle()
        jobs.forEach { it.join() }

        // Then
        assertEquals(5, results.size)
        results.forEach { assertEquals(mockProductDetails, it) }

        coVerify(exactly = 1) { getAvailableUpgrades() }
    }

    @Test
    fun `cache maintains separate state for different users`() = runTest {
        // Given
        val userId2 = UserId("test-user-456")
        val user1Products = listOf(mockk<ProductOfferList>())
        val user2Products = listOf(mockk<ProductOfferList>())

        coEvery { getAvailableUpgrades() } returnsMany listOf(user1Products, user2Products)

        // When
        val user1Result = async { cache.get(userId) }
        val user2Result = async { cache.get(userId2) }

        advanceUntilIdle()

        // Then
        assertEquals(user1Products, user1Result.await())
        assertEquals(user2Products, user2Result.await())
        coVerify(exactly = 2) { getAvailableUpgrades() }
    }

    private fun mockCacheExpiration() {
        mockkObject(Clock.System)
        every {
            Clock.System.now().toEpochMilliseconds()
        } returns System.currentTimeMillis() + CACHE_EXPIRATION_MS + 1000

    }

    private companion object {

        const val CACHE_EXPIRATION_MS = 30 * 60 * 1000L // 5 minutes
    }
}
