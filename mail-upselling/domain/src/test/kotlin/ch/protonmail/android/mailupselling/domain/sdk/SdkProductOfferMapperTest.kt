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

package ch.protonmail.android.mailupselling.domain.sdk

import me.proton.android.core.payment.domain.model.ProductEntitlement
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.ProductSelectionHeader
import me.proton.android.payment.common.model.Entitlement as SdkEntitlement
import me.proton.android.payment.common.model.Money
import me.proton.android.payment.product.model.Offer as SdkOffer
import me.proton.android.payment.product.model.PricingPhase as SdkPricingPhase
import me.proton.android.payment.product.model.Product as SdkProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class SdkProductOfferMapperTest {

    private val mapper = SdkProductOfferMapper()

    private val legacy = ProductOfferList(
        metadata = ProductMetadata(
            productId = "google_mail_plus_1_yearly",
            customerId = "customer-42",
            planName = "mail2022",
            entitlements = emptyList()
        ),
        header = ProductSelectionHeader(
            title = "ignored-by-mapper",
            description = "ignored-by-mapper",
            cycleText = "Annually",
            starred = true
        ),
        offers = emptyList()
    )

    private val sdkProduct = SdkProduct(
        id = "google_mail_plus_1_yearly",
        title = "Mail Plus",
        description = "Plus plan",
        planId = "mail2022",
        offers = listOf(
            SdkOffer.NonDiscounted(
                pricingPhases = listOf(
                    SdkPricingPhase(price = Money(100, "$1.00", "USD"), cycle = 1, period = "P1M"),
                    SdkPricingPhase(price = Money(1200, "$12.00", "USD"), cycle = 1, period = "P1Y")
                ),
                tags = emptyList(),
                token = "base-token"
            ),
            SdkOffer.Discounted(
                pricingPhases = listOf(
                    SdkPricingPhase(price = Money(50, "$0.50", "USD"), cycle = 1, period = "P1M"),
                    SdkPricingPhase(price = Money(600, "$6.00", "USD"), cycle = 1, period = "P1Y")
                ),
                tags = listOf("bf-promo"),
                token = "promo-token-1"
            )
        ),
        entitlements = listOf(
            SdkEntitlement.Description(text = "feature-1", iconName = "icon", hint = "hint"),
            SdkEntitlement.Progress(
                text = "12 GB",
                iconName = "storage",
                title = "Storage",
                min = 0f,
                max = 12f,
                current = 3f
            )
        )
    )

    @Test
    fun `produces composite with SDK data and legacy customerId planName cycleText`() {
        val out = mapper.mapToProductOfferList(
            sdkProducts = listOf(sdkProduct),
            legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacy)
        )

        assertEquals(1, out.size)
        val list = out.single()
        assertEquals("google_mail_plus_1_yearly", list.metadata.productId)
        assertEquals("customer-42", list.metadata.customerId)
        assertEquals("mail2022", list.metadata.planName)
        assertEquals("Mail Plus", list.header.title)
        assertEquals("Plus plan", list.header.description)
        assertEquals("Annually", list.header.cycleText)
        assertEquals(true, list.header.starred)
    }

    @Test
    fun `produces base offer first then promo offer with SDK tags preserved`() {
        val list = mapper.mapToProductOfferList(
            sdkProducts = listOf(sdkProduct),
            legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacy)
        ).single()

        assertEquals(2, list.offers.size)
        val base = list.offers[0]
        assertEquals(true, base.isBaseOffer)
        assertEquals("base-token", base.token.value)
        assertEquals(emptySet<String>(), base.tags.value)
        assertEquals(100, base.current.amount)
        assertEquals(1, base.current.cycle)
        assertEquals(1200, base.renew.amount)
        assertEquals(12, base.renew.cycle)
        assertEquals("customer-42", base.current.customerId)

        val promo = list.offers[1]
        assertEquals(false, promo.isBaseOffer)
        assertEquals("promo-token-1", promo.token.value)
        assertEquals(setOf("bf-promo"), promo.tags.value)
        assertEquals(50, promo.current.amount)
        assertEquals(600, promo.renew.amount)
    }

    @Test
    fun `drops SDK products with no legacy match`() {
        val out = mapper.mapToProductOfferList(
            sdkProducts = listOf(sdkProduct.copy(id = "unknown_id")),
            legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacy)
        )

        assertTrue(out.isEmpty())
    }

    @Test
    fun `maps SDK entitlements to legacy entitlements`() {
        val list = mapper.mapToProductOfferList(
            sdkProducts = listOf(sdkProduct),
            legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacy)
        ).single()

        val ents = list.metadata.entitlements
        assertEquals(2, ents.size)
        val desc = ents[0] as ProductEntitlement.Description
        assertEquals("feature-1", desc.text)
        assertEquals("icon", desc.iconName)
        assertEquals("hint", desc.hint)
        val progress = ents[1] as ProductEntitlement.Progress
        assertEquals("Storage", progress.startText)
        assertEquals("12 GB", progress.endText)
        assertEquals(0L, progress.min)
        assertEquals(12L, progress.max)
        assertEquals(3L, progress.current)
    }

    @Test
    fun `returns empty list when no SDK products provided`() {
        val out = mapper.mapToProductOfferList(
            sdkProducts = emptyList(),
            legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacy)
        )

        assertTrue(out.isEmpty())
    }

    @Test
    fun `produces no entries when SDK has product but legacy map is empty`() {
        val out = mapper.mapToProductOfferList(
            sdkProducts = listOf(sdkProduct),
            legacyByProductId = emptyMap()
        )

        assertTrue(out.isEmpty())
        @Suppress("USELESS_IS_CHECK")
        assertNull(out.firstOrNull())
    }
}
