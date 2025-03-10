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

package ch.protonmail.upselling.presentation.usecase

import java.time.Duration
import java.time.Instant
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint.Feature
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibilityCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.user.domain.entity.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpsellingVisibilityCacheTest {

    private val users = MutableStateFlow<User?>(null)
    private val purchases = MutableStateFlow<List<Purchase>>(emptyList())

    private val purchaseManager = mockk<PurchaseManager> {
        every { this@mockk.observePurchases() } returns purchases
    }
    private val observePrimaryUser = mockk<ObservePrimaryUser> {
        every { this@mockk.invoke() } returns users
    }
    private val cache: UpsellingVisibilityCache
        get() = UpsellingVisibilityCache(
            observePrimaryUser,
            purchaseManager,
            appScope = TestScope(UnconfinedTestDispatcher())
        )

    @BeforeTest
    fun setUp() {
        mockkStatic(Instant::class)
        expectCurrentTime(0L)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
        unmockkStatic(Instant::class)
    }

    @Test
    fun `should return null initially`() = runTest {
        // Given
        // When + Then
        val actual = cache.retrieve(Feature.ContactGroups)
        assertEquals(null, actual)
    }

    @Test
    fun `should return stored value`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        expectedInitialState(UserSample.Primary, emptyList())
        val sut = cache
        // When + Then
        sut.store(Feature.ContactGroups, true)
        val actual = sut.retrieve(Feature.ContactGroups)
        assertEquals(true, actual)
    }

    @Test
    fun `should return cached value before the expiration`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        expectedInitialState(UserSample.Primary, emptyList())
        val sut = cache
        // When + Then
        sut.store(Feature.ContactGroups, false)
        val initial = sut.retrieve(Feature.ContactGroups)
        assertEquals(false, initial)
        expectCurrentTime(Duration.ofMinutes(1).toMillis())
        val actual = sut.retrieve(Feature.ContactGroups)
        assertEquals(false, actual)
    }

    @Test
    fun `should return null after stored value expires`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        expectedInitialState(UserSample.Primary, emptyList())
        val sut = cache
        // When + Then
        sut.store(Feature.ContactGroups, true)
        val initial = sut.retrieve(Feature.ContactGroups)
        assertEquals(true, initial)
        expectCurrentTime(Duration.ofHours(1).toMillis())
        val actual = sut.retrieve(Feature.ContactGroups)
        assertEquals(null, actual)
    }

    @Test
    fun `should reset stored upon a new user emission`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        expectedInitialState(UserSample.Primary, emptyList())
        val sut = cache
        // When + Then
        sut.store(Feature.ContactGroups, true)
        val initial = sut.retrieve(Feature.ContactGroups)
        assertEquals(true, initial)
        users.emit(UserSample.UserWithKeys)
        val actual = sut.retrieve(Feature.ContactGroups)
        assertEquals(null, actual)
    }

    @Test
    fun `should reset stored upon a new purchase`() = runTest(UnconfinedTestDispatcher()) {
        // Given
        expectedInitialState(UserSample.Primary, emptyList())
        val sut = cache
        // When + Then
        sut.store(Feature.ContactGroups, true)
        val initial = sut.retrieve(Feature.ContactGroups)
        assertEquals(true, initial)
        purchases.emit(listOf(mockk()))
        val actual = sut.retrieve(Feature.ContactGroups)
        assertEquals(null, actual)
    }

    private fun expectedInitialState(initialUser: User?, initialPurchases: List<Purchase>) {
        users.value = initialUser
        purchases.value = initialPurchases
    }

    private fun expectCurrentTime(timeMs: Long) {
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns timeMs }
    }
}
