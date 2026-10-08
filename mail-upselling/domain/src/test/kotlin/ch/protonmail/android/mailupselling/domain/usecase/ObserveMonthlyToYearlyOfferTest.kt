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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveMonthlyToYearlyOfferTest {

    private val userId = UserId("user-id")
    private val upsellEligibilityRepository = mockk<UpsellEligibilityRepository>()
    private val availableUpgradesCache = mockk<AvailableUpgradesCache>()
    private val getMonthlyToYearlyOffer = mockk<GetMonthlyToYearlyOffer>()
    private val sdkUpgradesReadEnabled = mockk<FeatureFlag<Boolean>>()
    private val sdkUpgradesPurchaseEnabled = mockk<FeatureFlag<Boolean>>()
    private val upgrades = listOf(mockk<ProductOfferList>())
    private val offer = mockk<MonthlyToYearlyOffer>()

    private val observeMonthlyToYearlyOffer = ObserveMonthlyToYearlyOffer(
        upsellEligibilityRepository,
        availableUpgradesCache,
        getMonthlyToYearlyOffer,
        sdkUpgradesReadEnabled,
        sdkUpgradesPurchaseEnabled
    )

    @Test
    fun `emits the offer when Rust says the user qualifies`() = runTest {
        expectSdkFlags()
        every { availableUpgradesCache.observe(userId) } returns flowOf(upgrades)
        coEvery { upsellEligibilityRepository.getMonthlyToYearlyEligibility(userId) } returns true.right()
        coEvery { getMonthlyToYearlyOffer(upgrades) } returns offer

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(offer.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits no offer when Rust says the user does not qualify`() = runTest {
        expectSdkFlags()
        every { availableUpgradesCache.observe(userId) } returns flowOf(upgrades)
        coEvery { upsellEligibilityRepository.getMonthlyToYearlyEligibility(userId) } returns false.right()

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(null.right(), awaitItem())
            awaitComplete()
        }
        coVerify(exactly = 0) { getMonthlyToYearlyOffer(any()) }
    }

    @Test
    fun `passes the eligibility error through`() = runTest {
        expectSdkFlags()
        every { availableUpgradesCache.observe(userId) } returns flowOf(upgrades)
        coEvery { upsellEligibilityRepository.getMonthlyToYearlyEligibility(userId) } returns
            DataError.Local.Unknown.left()

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(DataError.Local.Unknown.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits no offer when a Payments SDK flag is off`() = runTest {
        expectSdkFlags(read = false)

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(null.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits no offer without asking Rust while no upgrades are loaded`() = runTest {
        expectSdkFlags()
        every { availableUpgradesCache.observe(userId) } returns flowOf(emptyList())

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(null.right(), awaitItem())
            awaitComplete()
        }
        coVerify(exactly = 0) { upsellEligibilityRepository.getMonthlyToYearlyEligibility(any()) }
    }

    @Test
    fun `re-evaluates whenever the upgrades reload`() = runTest {
        expectSdkFlags()
        every { availableUpgradesCache.observe(userId) } returns flowOf(upgrades, upgrades)
        coEvery { upsellEligibilityRepository.getMonthlyToYearlyEligibility(userId) } returnsMany
            listOf(true.right(), false.right())
        coEvery { getMonthlyToYearlyOffer(upgrades) } returns offer

        observeMonthlyToYearlyOffer(userId).test {
            assertEquals(offer.right(), awaitItem())
            assertEquals(null.right(), awaitItem())
            awaitComplete()
        }
    }

    private fun expectSdkFlags(read: Boolean = true, purchase: Boolean = true) {
        coEvery { sdkUpgradesReadEnabled.get() } returns read
        coEvery { sdkUpgradesPurchaseEnabled.get() } returns purchase
    }
}
