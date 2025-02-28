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
import ch.protonmail.android.mailupselling.domain.usecase.UserHasAvailablePlans
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveFreeUserClickUpsellingVisibility
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import me.proton.core.user.domain.entity.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveFreeUserClickUpsellingVisibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val userHasAvailablePlans = mockk<UserHasAvailablePlans>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val userHasPendingPurchases = mockk<UserHasPendingPurchases>()
    private val canUpgradeFromMobile = mockk<CanUpgradeFromMobile>()

    private val sut: ObserveFreeUserClickUpsellingVisibility
        get() = ObserveFreeUserClickUpsellingVisibility(
            observePrimaryUser,
            purchaseManager,
            canUpgradeFromMobile,
            userHasAvailablePlans,
            userHasPendingPurchases
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return false if observed user is null`() = runTest {
        // Given
        expectedUser(null)
        expectPurchases(emptyList())

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user can not upgrade from mobile`() = runTest {
        // Given
        expectedUser(null)
        expectPurchases(emptyList())

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user has pending purchases`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user has no available plans`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, false)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return true if user has available plans and no pending subs`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(listOf(mockk<Purchase>()))
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectUserHasAvailablePlans(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    private fun expectedUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectPurchases(list: List<Purchase>) {
        every { purchaseManager.observePurchases() } returns flowOf(list)
    }

    private fun expectUserHasAvailablePlans(userId: UserId, value: Boolean) {
        coEvery { userHasAvailablePlans(userId) } returns value
    }

    private fun expectPendingPurchasesValue(userId: UserId, value: Boolean) {
        coEvery { userHasPendingPurchases(any(), userId) } returns value
    }

    private fun expectCanUpgradeFromMobile(userId: UserId, value: Boolean) {
        coEvery { canUpgradeFromMobile(userId) } returns value
    }
}
