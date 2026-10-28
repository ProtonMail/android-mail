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
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.usecase.ObserveUser
import ch.protonmail.android.mailupselling.domain.cache.AvailableUpgradesCache
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.model.PlusToUnlimitedOffer
import ch.protonmail.android.mailupselling.domain.repository.UpsellEligibilityRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellOptOutRepository
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.SubscriptionDetail
import me.proton.android.core.payment.domain.usecase.GetCurrentSubscriptions
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObservePlusToUnlimitedEligibilityTest {

    private val userId = UserId("user-id")
    private val observeUser = mockk<ObserveUser> {
        every { this@mockk(userId) } returns flowOf(UserTestData.Primary.right())
    }
    private val upsellEligibilityRepository = mockk<UpsellEligibilityRepository>()
    private val upsellOptOutRepository = mockk<UpsellOptOutRepository> {
        every { observeOptOutChanges() } returns flowOf(Unit)
    }
    private val availableUpgradesCache = mockk<AvailableUpgradesCache>()
    private val getCurrentSubscriptions = mockk<GetCurrentSubscriptions>()
    private val isEligibleForBlackFridayPromotion = mockk<IsEligibleForBlackFridayPromotion>()
    private val sdkUpgradesReadEnabled = mockk<FeatureFlag<Boolean>>()
    private val sdkUpgradesPurchaseEnabled = mockk<FeatureFlag<Boolean>>()
    private val blackFridayOptedOut = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns false
    }

    private val observePlusToUnlimitedEligibility = ObservePlusToUnlimitedEligibility(
        observeUser,
        upsellEligibilityRepository,
        upsellOptOutRepository,
        availableUpgradesCache,
        getCurrentSubscriptions,
        isEligibleForBlackFridayPromotion,
        sdkUpgradesReadEnabled,
        sdkUpgradesPurchaseEnabled,
        blackFridayOptedOut
    )

    @Test
    fun `is eligible when an Unlimited upgrade is available`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.Regular.right())
        expectUpgrades(PlanUpgradeIds.UnlimitedPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.Regular.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when no Unlimited upgrade is available`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.Regular.right())
        expectUpgrades(PlanUpgradeIds.PlusPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the SDK purchase flag is off`() = runTest {
        expectSdkFlags(read = true, purchase = false)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the SDK read flag is off`() = runTest {
        expectSdkFlags(read = false, purchase = true)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the subscription does not qualify`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.None.right())

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `propagates the error when the eligibility check could not be completed`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(DataError.Local.Unknown.left())

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(DataError.Local.Unknown.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `re-checks eligibility when the user's subscription changes`() = runTest {
        val user = MutableStateFlow(UserTestData.paidMailUser)
        every { observeUser(userId) } returns user.map { it.right() }
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.Regular.right())
        expectUpgrades(PlanUpgradeIds.UnlimitedPlanId)
        expectCurrentSubscriptions(PlanUpgradeIds.PlusPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.Regular.right(), awaitItem())

            expectCurrentSubscriptions(PlanUpgradeIds.UnlimitedPlanId)
            user.value = UserTestData.paidUser

            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
        }
    }

    @Test
    fun `is not eligible when the user is already subscribed to Unlimited`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.Regular.right())
        expectUpgrades(PlanUpgradeIds.UnlimitedPlanId)
        expectCurrentSubscriptions(PlanUpgradeIds.UnlimitedPlanId)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `stays eligible when the current subscriptions cannot be read`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.Regular.right())
        expectUpgrades(PlanUpgradeIds.UnlimitedPlanId)
        coEvery { getCurrentSubscriptions() } throws IllegalStateException("boom")

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.Regular.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is eligible for Black Friday when a Black Friday Unlimited offer is available`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.BlackFriday.right())
        expectBlackFridayPromotionEligibility(true)
        expectUpgradeOffers(UpsellingTestData.UnlimitedMailProduct.YearlyBFProductOfferList)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.BlackFriday.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible for Black Friday without a Black Friday Unlimited offer`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.BlackFriday.right())
        expectBlackFridayPromotionEligibility(true)
        expectUpgradeOffers(UpsellingTestData.UnlimitedMailProduct.YearlyProductOfferList)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the user cannot get the Black Friday promotion`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.BlackFriday.right())
        expectBlackFridayPromotionEligibility(false)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `is not eligible when the user opted out of Black Friday`() = runTest {
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.BlackFriday.right())
        expectBlackFridayPromotionEligibility(true)
        coEvery { blackFridayOptedOut.get() } returns true

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `stops being eligible once the user opts out of Black Friday`() = runTest {
        val optOutChanges = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
        every { upsellOptOutRepository.observeOptOutChanges() } returns optOutChanges
        expectSdkFlags(read = true, purchase = true)
        expectRustEligibility(PlusToUnlimitedOffer.BlackFriday.right())
        expectBlackFridayPromotionEligibility(true)
        expectUpgradeOffers(UpsellingTestData.UnlimitedMailProduct.YearlyBFProductOfferList)

        observePlusToUnlimitedEligibility(userId).test {
            assertEquals(PlusToUnlimitedOffer.BlackFriday.right(), awaitItem())

            coEvery { blackFridayOptedOut.get() } returns true
            optOutChanges.emit(Unit)

            assertEquals(PlusToUnlimitedOffer.None.right(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun expectSdkFlags(read: Boolean, purchase: Boolean) {
        coEvery { sdkUpgradesReadEnabled.get() } returns read
        coEvery { sdkUpgradesPurchaseEnabled.get() } returns purchase
    }

    private fun expectRustEligibility(value: Either<DataError, PlusToUnlimitedOffer>) {
        coEvery { upsellEligibilityRepository.getPlusToUnlimitedEligibility(userId) } returns value
    }

    private fun expectBlackFridayPromotionEligibility(eligible: Boolean) {
        coEvery { isEligibleForBlackFridayPromotion(userId) } returns eligible
    }

    private fun expectUpgradeOffers(vararg offers: ProductOfferList) {
        every { availableUpgradesCache.observe(userId) } returns flowOf(offers.toList())
        expectCurrentSubscriptions(PlanUpgradeIds.PlusPlanId)
    }

    private fun expectCurrentSubscriptions(vararg planNames: String) {
        val subscriptions = planNames.map { planName ->
            mockk<SubscriptionDetail> { every { name } returns planName }
        }
        coEvery { getCurrentSubscriptions() } returns subscriptions
    }

    private fun expectUpgrades(planName: String) {
        val metadata = mockk<ProductMetadata> { every { this@mockk.planName } returns planName }
        val offer = mockk<ProductOfferList> { every { this@mockk.metadata } returns metadata }
        every { availableUpgradesCache.observe(userId) } returns flowOf(listOf(offer))
        expectCurrentSubscriptions(PlanUpgradeIds.PlusPlanId)
    }
}
