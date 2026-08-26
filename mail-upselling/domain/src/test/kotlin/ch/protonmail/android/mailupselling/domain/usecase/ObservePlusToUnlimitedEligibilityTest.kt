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

package ch.protonmail.android.mailupselling.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObservePlusToUnlimitedEligibilityTest {

    private val userId = UserId("user-id")
    private val upsellEligibilityRepository = mockk<UpsellEligibilityRepository>()
    private val availableUpgradesCache = mockk<AvailableUpgradesCache>()
    private val sdkUpgradesReadEnabled = mockk<FeatureFlag<Boolean>>()
    private val sdkUpgradesPurchaseEnabled = mockk<FeatureFlag<Boolean>>()

    private val observePlusToUnlimitedEligibility = ObservePlusToUnlimitedEligibility(
        upsellEligibilityRepository,
        availableUpgradesCache,
        sdkUpgradesReadEnabled,
        sdkUpgradesPurchaseEnabled
    )

    @Test
    fun `is eligible when an Unlimited upgrade is available`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(true)
        expectUpgrades(PlanUpgradeIds.UnlimitedPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when no Unlimited upgrade is available`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(true)
        expectUpgrades(PlanUpgradeIds.PlusPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the SDK purchase flag is off`() = runTest {
        expectSdkFlags(read = true, purchase = false)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the SDK read flag is off`() = runTest {
        expectSdkFlags(read = false, purchase = true)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the subscription does not qualify`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(false)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    private fun expectSdkFlags(read: Boolean, purchase: Boolean) {
        coEvery { sdkUpgradesReadEnabled.get() } returns read
        coEvery { sdkUpgradesPurchaseEnabled.get() } returns purchase
    }

    private fun expectRustEligibility(value: Boolean) {
        coEvery { upsellEligibilityRepository.getPlusToUnlimitedEligibility(userId) } returns value
    }

    private fun expectUpgrades(planName: String) {
        val metadata = mockk<ProductMetadata> { every { this@mockk.planName } returns planName }
        val offer = mockk<ProductOfferList> { every { this@mockk.metadata } returns metadata }
        every { availableUpgradesCache.observe(userId) } returns flowOf(listOf(offer))
    }
}
