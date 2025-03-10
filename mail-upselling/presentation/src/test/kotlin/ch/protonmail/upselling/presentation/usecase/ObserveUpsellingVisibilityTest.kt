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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint.Feature
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibilityCache
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.user.domain.entity.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveUpsellingVisibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val cache = mockk<UpsellingVisibilityCache>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val resolveUpsellingVisibility = mockk<ResolveUpsellingVisibility>()
    private val sut: ObserveUpsellingVisibility
        get() = ObserveUpsellingVisibility(
            observePrimaryUser,
            purchaseManager,
            resolveUpsellingVisibility = resolveUpsellingVisibility,
            cache = cache
        )

    @BeforeTest
    fun setUp() {
        expectPurchases(emptyList())
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false if observed user is null`() = runTest {
        // Given
        expectedUser(null)
        // When + Then
        sut(Feature.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return cached value if available`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        every { cache.retrieve(Feature.ContactGroups) } returns true

        // When + Then
        sut(Feature.ContactGroups).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false from cache if available`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        every { cache.retrieve(Feature.ContactGroups) } returns false

        // When + Then
        sut(Feature.ContactGroups).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should resolve a new value if cached value not present`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        every { cache.retrieve(Feature.ContactGroups) } returns null
        every { cache.store(Feature.ContactGroups, true) } just runs
        expectedUpsellingVisibility(true)

        // When + Then
        sut(Feature.ContactGroups).test {
            assertEquals(false, awaitItem())
            assertEquals(true, awaitItem())
            verify(exactly = 1) { cache.store(Feature.ContactGroups, true) }
            awaitComplete()
        }
    }

    @Test
    fun `should emit distinct values`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        every { cache.retrieve(Feature.ContactGroups) } returns null
        every { cache.store(Feature.ContactGroups, false) } just runs
        expectedUpsellingVisibility(false)

        // When + Then
        sut(Feature.ContactGroups).test {
            assertEquals(false, awaitItem())
            verify(exactly = 1) { cache.store(Feature.ContactGroups, false) }
            awaitComplete()
        }
    }

    private fun expectedUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectPurchases(list: List<Purchase>) {
        every { purchaseManager.observePurchases() } returns flowOf(list)
    }

    private fun expectedUpsellingVisibility(value: Boolean) {
        coEvery { resolveUpsellingVisibility.invoke(any(), any(), any()) } returns value
    }
}
