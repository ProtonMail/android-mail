/*
 * Copyright (C) 2026 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.google.data.model

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import io.mockk.every
import io.mockk.mockk
import me.proton.android.payment.billing.model.StoreProductOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BillingStoreProductMapperTest {

    @Test
    fun `maps base offer (no offerId) and promo offer (offerId), preserving tags and pricing`() {
        val product = productDetails(
            id = "google_mail_plus",
            title = "Mail Plus",
            description = "Plus plan",
            offers = listOf(
                offer(
                    offerIdValue = null,
                    token = "base-token",
                    tags = emptyList(),
                    phases = listOf(phase(1200, "$12.00", "USD", cycleCount = 1, period = "P1Y"))
                ),
                offer(
                    offerIdValue = "promo",
                    token = "promo-token",
                    tags = listOf("bf-promo"),
                    phases = listOf(phase(600, "$6.00", "USD", cycleCount = 1, period = "P1Y"))
                )
            )
        )

        val result = product.toStoreProduct()

        assertEquals("google_mail_plus", result?.id)
        assertEquals("Mail Plus", result?.title)
        assertEquals("Plus plan", result?.description)
        assertEquals(2, result?.offers?.size)

        val base = result!!.offers[0]
        assertIs<StoreProductOffer.NonDiscounted>(base)
        assertEquals("base-token", base.token)
        assertTrue(base.tags.isEmpty())
        val basePhase = base.pricingPhases.single()
        assertEquals(1200, basePhase.priceAmount)
        assertEquals("$12.00", basePhase.priceFormattedAmount)
        assertEquals("USD", basePhase.currency)
        assertEquals(1, basePhase.cycle)
        assertEquals("P1Y", basePhase.period)

        val promo = result.offers[1]
        assertIs<StoreProductOffer.Discounted>(promo)
        assertEquals("promo-token", promo.token)
        assertEquals(listOf("bf-promo"), promo.tags)
        assertEquals(600, promo.pricingPhases.single().priceAmount)
    }

    @Test
    fun `drops product with no non-discounted base offer`() {
        val product = productDetails(
            id = "x",
            title = "x",
            description = "x",
            offers = listOf(
                offer("promo", "t", listOf("bf-promo"), listOf(phase(1, "a", "USD", 1, "P1M")))
            )
        )

        assertNull(product.toStoreProduct())
    }

    @Test
    fun `keeps a base-only product with a single non-discounted offer`() {
        val product = productDetails(
            id = "google_mail_plus",
            title = "Mail Plus",
            description = "Plus plan",
            offers = listOf(
                offer(null, "base-token", emptyList(), listOf(phase(1200, "$12.00", "USD", 1, "P1Y")))
            )
        )

        val result = product.toStoreProduct()

        assertEquals(1, result?.offers?.size)
        assertIs<StoreProductOffer.NonDiscounted>(result!!.offers.single())
    }

    @Test
    fun `maps all pricing phases in order for a multi-phase offer`() {
        val product = productDetails(
            id = "google_mail_plus",
            title = "Mail Plus",
            description = "Plus plan",
            offers = listOf(
                offer(
                    offerIdValue = "intro",
                    token = "intro-token",
                    tags = listOf("introductory-price"),
                    phases = listOf(
                        phase(0, "Free", "USD", cycleCount = 1, period = "P1M"),
                        phase(600, "$6.00", "USD", cycleCount = 2, period = "P1M"),
                        phase(1200, "$12.00", "USD", cycleCount = 1, period = "P1Y")
                    )
                ),
                offer(null, "base-token", emptyList(), listOf(phase(1200, "$12.00", "USD", 1, "P1Y")))
            )
        )

        val intro = product.toStoreProduct()!!.offers.first { it is StoreProductOffer.Discounted }
        assertEquals(listOf(0L, 600L, 1200L), intro.pricingPhases.map { it.priceAmount })
        assertEquals(listOf("P1M", "P1M", "P1Y"), intro.pricingPhases.map { it.period })
    }

    @Test
    fun `returns null when subscription offers is null`() {
        val product = mockk<ProductDetails> { every { subscriptionOfferDetails } returns null }

        assertNull(product.toStoreProduct())
    }

    @Test
    fun `returns null when subscription offers is empty`() {
        val product = productDetails(id = "x", title = "x", description = "x", offers = emptyList())

        assertNull(product.toStoreProduct())
    }

    private fun phase(
        amount: Long,
        formatted: String,
        currency: String,
        cycleCount: Int,
        period: String
    ) = mockk<PricingPhase> {
        every { priceAmountMicros } returns amount
        every { formattedPrice } returns formatted
        every { priceCurrencyCode } returns currency
        every { billingCycleCount } returns cycleCount
        every { billingPeriod } returns period
    }

    private fun offer(
        offerIdValue: String?,
        token: String,
        tags: List<String>,
        phases: List<PricingPhase>
    ) = mockk<SubscriptionOfferDetails> {
        every { offerId } returns offerIdValue
        every { offerToken } returns token
        every { offerTags } returns ArrayList(tags)
        every { pricingPhases } returns mockk<PricingPhases> { every { pricingPhaseList } returns ArrayList(phases) }
    }

    private fun productDetails(
        id: String,
        title: String,
        description: String,
        offers: List<SubscriptionOfferDetails>
    ) = mockk<ProductDetails> {
        every { productId } returns id
        every { name } returns title
        every { this@mockk.description } returns description
        every { subscriptionOfferDetails } returns ArrayList(offers)
    }
}
