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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.usecase.UserHasPendingPurchases
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingOnboardingVisibility
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.PurchaseManager
import me.proton.core.payment.domain.entity.Purchase
import me.proton.core.plan.domain.usecase.CanUpgradeFromMobile
import me.proton.core.user.domain.entity.User
import javax.inject.Provider
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveUpsellingOnboardingVisibilityTest {

    private val observePrimaryUser = mockk<ObservePrimaryUser>()
    private val purchaseManager = mockk<PurchaseManager>()
    private val userHasPendingPurchases = mockk<UserHasPendingPurchases>()
    private val canUpgradeFromMobile = mockk<CanUpgradeFromMobile>()
    private val isOnboardingUpsellFeatureEnabled = mockk<Provider<Boolean>>()
    private val getOnboardingUpsellingPlans = mockk<GetOnboardingUpsellingPlans>()

    private val sut: ObserveUpsellingOnboardingVisibility
        get() = ObserveUpsellingOnboardingVisibility(
            observePrimaryUser,
            purchaseManager,
            canUpgradeFromMobile,
            userHasPendingPurchases,
            isOnboardingUpsellFeatureEnabled.get(),
            getOnboardingUpsellingPlans
        )

    @Test
    fun `should return false if observed user is null`() = runTest {
        // Given
        expectedUser(null)
        expectPurchases(emptyList())
        expectFeatureFlagEnabled(true)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if the FF is disabled`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(emptyList())
        expectFeatureFlagEnabled(false)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if user can not upgrade from mobile`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(emptyList())
        expectFeatureFlagEnabled(true)
        expectCanUpgradeFromMobile(UserSample.Primary.userId, false)

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
        expectFeatureFlagEnabled(true)
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectPurchases(listOf(mockk<Purchase>()))
        expectPendingPurchasesValue(UserSample.Primary.userId, true)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return false if onboarding upselling plans are not fetched with success`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(emptyList())
        expectFeatureFlagEnabled(false)
        expectOnboardingUpsellingPlansError(UserSample.Primary.userId)

        // When + Then
        sut().test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return true if onboarding upselling plans are fetched correctly`() = runTest {
        // Given
        expectedUser(UserSample.Primary)
        expectPurchases(emptyList())
        expectCanUpgradeFromMobile(UserSample.Primary.userId, true)
        expectFeatureFlagEnabled(true)
        expectPendingPurchasesValue(UserSample.Primary.userId, false)
        expectOnboardingUpsellingPlans(UserSample.Primary.userId)

        // When + Then
        sut().test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    private fun expectFeatureFlagEnabled(value: Boolean) {
        every { isOnboardingUpsellFeatureEnabled.get() } returns value
    }

    private fun expectedUser(user: User?) {
        every { observePrimaryUser() } returns flowOf(user)
    }

    private fun expectPurchases(list: List<Purchase>) {
        every { purchaseManager.observePurchases() } returns flowOf(list)
    }

    private fun expectPendingPurchasesValue(userId: UserId, value: Boolean) {
        coEvery { userHasPendingPurchases(any(), userId) } returns value
    }

    private fun expectCanUpgradeFromMobile(userId: UserId, value: Boolean) {
        coEvery { canUpgradeFromMobile(userId) } returns value
    }

    private fun expectOnboardingUpsellingPlans(userId: UserId) {
        coEvery { getOnboardingUpsellingPlans(userId) } returns UpsellingTestData.DynamicPlans.right()
    }

    private fun expectOnboardingUpsellingPlansError(userId: UserId) {
        coEvery {
            getOnboardingUpsellingPlans(userId)
        } returns GetOnboardingUpsellingPlans.GetOnboardingPlansError.NoPlans.left()
    }
}
