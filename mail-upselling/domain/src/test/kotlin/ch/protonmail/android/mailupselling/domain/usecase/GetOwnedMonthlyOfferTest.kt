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

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.payment.billing.model.StoreOffer
import me.proton.android.payment.billing.model.StorePricingDuration
import me.proton.android.payment.billing.model.StorePricingPeriod
import me.proton.android.payment.billing.model.StoreProduct
import me.proton.android.payment.billing.model.StorePurchase
import me.proton.android.payment.billing.model.StorePurchaseState
import me.proton.android.payment.billing.usecase.GetActivePurchases
import me.proton.android.payment.billing.usecase.GetStoreProducts
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class GetOwnedMonthlyOfferTest {

    private val getActivePurchases = mockk<GetActivePurchases>()
    private val getStoreProducts = mockk<GetStoreProducts>()
    private val getOwnedMonthlyOffer = GetOwnedMonthlyOffer(
        Optional.of(getActivePurchases),
        Optional.of(getStoreProducts)
    )
    private val yearlyPlus = yearlyDetail(YearlyPlusId, "mail2022")

    @Test
    fun `returns the base monthly offer of the plan's monthly product`() = runTest {
        expectPurchases(purchase(MonthlyPlusId))
        expectProducts(storeProduct(MonthlyPlusId, baseOffer(monthlyPeriod(5_990_000))))

        val monthly = assertNotNull(getOwnedMonthlyOffer(yearlyPlus))

        assertEquals(MonthlyPlusId, monthly.current.productId)
        assertEquals(5_990_000, monthly.current.amount)
        assertEquals(1, monthly.current.cycle)
        assertEquals("CHF", monthly.current.currency)
        assertEquals(true, monthly.isBaseOffer)
    }

    @Test
    fun `prefers the base offer over an introductory one`() = runTest {
        expectPurchases(purchase(MonthlyPlusId))
        val introductory = StoreOffer.Discounted(
            tags = emptyList(),
            token = "intro",
            pricingPeriods = listOf(monthlyPeriod(990_000), monthlyPeriod(5_990_000))
        )
        expectProducts(storeProduct(MonthlyPlusId, introductory, baseOffer(monthlyPeriod(5_990_000))))

        assertEquals(5_990_000, assertNotNull(getOwnedMonthlyOffer(yearlyPlus)).current.amount)
    }

    @Test
    fun `has nothing when the base offer is not billed monthly`() = runTest {
        expectPurchases(purchase(MonthlyPlusId))
        val yearlyPeriod = monthlyPeriod(47_900_000).copy(cycleDurationUnit = StorePricingDuration.YEAR)
        expectProducts(storeProduct(MonthlyPlusId, baseOffer(yearlyPeriod)))

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
    }

    @Test
    fun `ignores purchases of other plans`() = runTest {
        expectPurchases(purchase(MonthlyUnlimitedId))

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
        coVerify(exactly = 0) { getStoreProducts(any()) }
    }

    @Test
    fun `ignores a purchase that is not acknowledged yet`() = runTest {
        expectPurchases(purchase(MonthlyPlusId, acknowledged = false))

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
    }

    @Test
    fun `has nothing when the owned product is already the yearly one`() = runTest {
        expectPurchases(purchase(YearlyPlusId))

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
    }

    @Test
    fun `has nothing when nothing was bought on this Play Store account`() = runTest {
        expectPurchases()

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
    }

    @Test
    fun `has nothing when the Play Store cannot be reached`() = runTest {
        coEvery { getActivePurchases() } returns Result.failure(IllegalStateException("billing unavailable"))

        assertNull(getOwnedMonthlyOffer(yearlyPlus))
    }

    @Test
    fun `has nothing when Play billing is not part of the build`() = runTest {
        val withoutBilling = GetOwnedMonthlyOffer(Optional.empty(), Optional.empty())

        assertNull(withoutBilling(yearlyPlus))
    }

    private fun expectPurchases(vararg purchases: StorePurchase) {
        coEvery { getActivePurchases() } returns Result.success(purchases.toList())
    }

    private fun expectProducts(vararg products: StoreProduct) {
        coEvery { getStoreProducts(products.map { it.id }) } returns Result.success(products.toList())
    }

    private fun purchase(productId: String, acknowledged: Boolean = true) = StorePurchase(
        productId = productId,
        packageName = "ch.protonmail.android",
        purchaseState = StorePurchaseState.PURCHASED,
        orderId = "order-id",
        userId = "user-id",
        acknowledged = acknowledged
    )

    private fun storeProduct(productId: String, vararg offers: StoreOffer) = StoreProduct(
        id = productId,
        title = "Mail Plus",
        description = "",
        offers = offers.toList()
    )

    private fun baseOffer(vararg periods: StorePricingPeriod) = StoreOffer.NonDiscounted(
        tags = emptyList(),
        token = "base",
        pricingPeriods = periods.toList()
    )

    private fun monthlyPeriod(amount: Long) = StorePricingPeriod(
        amount = amount,
        currency = "CHF",
        displayPrice = "",
        cycleRecurrence = 0,
        cycleDurationUnit = StorePricingDuration.MONTH,
        cycleDurationCount = 1
    )

    private fun yearlyDetail(productId: String, planName: String) = ProductOfferDetail(
        metadata = ProductMetadata(
            productId = productId,
            customerId = "customer-42",
            planName = planName,
            entitlements = emptyList()
        ),
        header = ProductDetailHeader(title = "Plan", description = "", priceText = "", cycleText = "", starred = false),
        offer = yearlyOffer(productId)
    )

    private fun yearlyOffer(productId: String): ProductOffer {
        val price = ProductOfferPrice(
            productId = productId,
            customerId = null,
            cycle = 12,
            amount = 47_900_000,
            currency = "CHF",
            formatted = ""
        )
        return ProductOffer(
            isBaseOffer = true,
            tags = ProductOfferTags(emptySet()),
            token = ProductOfferToken("token"),
            current = price,
            renew = price
        )
    }

    private companion object {

        const val MonthlyPlusId = "giapmail_mail2022_1_renewing"
        const val YearlyPlusId = "giapmail_mail2022_12_renewing"
        const val MonthlyUnlimitedId = "giapmail_bundle2022_1_renewing"
    }
}
