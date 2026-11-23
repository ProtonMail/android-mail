package me.proton.android.core.payment.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProductOfferTest {

    @Test
    fun `base offer price shows the currency code for a dollar currency`() {
        val offerList = makeOfferList(amountMicros = 12_990_000, currency = "NZD", formatted = "$12.99")

        assertEquals(expectedHeader(priceText = "NZD 12.99"), offerList.getBaseOffer()?.header)
    }

    @Test
    fun `base offer price keeps the dollar sign for USD`() {
        val offerList = makeOfferList(amountMicros = 12_990_000, currency = "USD", formatted = "$12.99")

        assertEquals(expectedHeader(priceText = "$12.99"), offerList.getBaseOffer()?.header)
    }

    @Test
    fun `base offer price keeps the store text for other currencies`() {
        val offerList = makeOfferList(amountMicros = 12_990_000, currency = "EUR", formatted = "€12.99")

        assertEquals(expectedHeader(priceText = "€12.99"), offerList.getBaseOffer()?.header)
    }

    private fun expectedHeader(priceText: String) = ProductDetailHeader(
        title = "Mail Plus",
        description = "",
        priceText = priceText,
        cycleText = "",
        starred = false
    )

    private fun makeOfferList(
        amountMicros: Long,
        currency: String,
        formatted: String
    ): ProductOfferList {
        val price = ProductOfferPrice(
            productId = "productId",
            customerId = "customerId",
            cycle = 1,
            amount = amountMicros,
            currency = currency,
            formatted = formatted
        )
        return ProductOfferList(
            metadata = ProductMetadata(
                productId = "productId",
                customerId = "customerId",
                planName = "mail2022",
                entitlements = emptyList()
            ),
            header = ProductSelectionHeader(title = "Mail Plus", description = "", cycleText = "", starred = false),
            offers = listOf(
                ProductOffer(
                    isBaseOffer = true,
                    tags = ProductOfferTags(emptySet()),
                    token = ProductOfferToken("token"),
                    current = price,
                    renew = price
                )
            )
        )
    }
}
