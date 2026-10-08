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

import ch.protonmail.android.mailupselling.domain.model.YearlySaving
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductMetadata
import me.proton.android.core.payment.domain.model.ProductOffer
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.model.ProductOfferPrice
import me.proton.android.core.payment.domain.model.ProductOfferTags
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.domain.model.ProductSelectionHeader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class GetMonthlyToYearlyOfferTest {

    private val getOwnedMonthlyOffer = mockk<GetOwnedMonthlyOffer>()
    private val getMonthlyToYearlyOffer = GetMonthlyToYearlyOffer(
        getOwnedMonthlyOffer,
        GetYearlySaving(),
        GetDiscountRate()
    )

    private val yearlyPlus = upgrade(YearlyPlusId, "mail2022", cycle = 12, amount = 47_900_000)
    private val monthlyPlus = upgrade(MonthlyPlusId, "mail2022", cycle = 1, amount = 5_990_000)
    private val yearlyUnlimited = upgrade(YearlyUnlimitedId, "bundle2022", cycle = 12, amount = 119_880_000)

    @Test
    fun `returns the yearly offer and the saving for a monthly Plus subscriber`() = runTest {
        expectOwnedMonthly(YearlyPlusId, offer(MonthlyPlusId, cycle = 1, amount = 5_990_000))

        val offer = assertNotNull(getMonthlyToYearlyOffer(listOf(yearlyPlus)))

        assertEquals(UpsellVariantPlan.MAIL_PLUS, offer.plan)
        assertEquals(MonthlyPlusId, offer.monthly.current.productId)
        assertEquals(YearlyPlusId, offer.yearly.metadata.productId)
        assertEquals(YearlySaving("CHF", BigDecimal("23.98")), offer.saving)
        assertEquals(33, offer.discountRate)
    }

    @Test
    fun `picks the plan whose monthly product is owned`() = runTest {
        expectOwnedMonthly(YearlyPlusId, null)
        expectOwnedMonthly(YearlyUnlimitedId, offer(MonthlyUnlimitedId, cycle = 1, amount = 12_990_000))

        val offer = assertNotNull(getMonthlyToYearlyOffer(listOf(yearlyPlus, yearlyUnlimited)))

        assertEquals(UpsellVariantPlan.UNLIMITED, offer.plan)
        assertEquals(YearlyUnlimitedId, offer.yearly.metadata.productId)
        assertEquals(MonthlyUnlimitedId, offer.monthly.current.productId)
    }

    @Test
    fun `has no offer when the yearly product is not among the upgrades`() = runTest {
        assertNull(getMonthlyToYearlyOffer(listOf(monthlyPlus)))
        coVerify(exactly = 0) { getOwnedMonthlyOffer(any()) }
    }

    @Test
    fun `has no offer when the monthly product is not owned`() = runTest {
        expectOwnedMonthly(YearlyPlusId, null)

        assertNull(getMonthlyToYearlyOffer(listOf(yearlyPlus)))
    }

    @Test
    fun `has no offer when yearly billing does not save anything`() = runTest {
        expectOwnedMonthly(YearlyPlusId, offer(MonthlyPlusId, cycle = 1, amount = 3_990_000))

        assertNull(getMonthlyToYearlyOffer(listOf(yearlyPlus)))
    }

    private fun expectOwnedMonthly(yearlyProductId: String, monthly: ProductOffer?) {
        coEvery { getOwnedMonthlyOffer(match { it.metadata.productId == yearlyProductId }) } returns monthly
    }

    private fun offer(
        productId: String,
        cycle: Int,
        amount: Long
    ): ProductOffer {
        val price = ProductOfferPrice(
            productId = productId,
            customerId = null,
            cycle = cycle,
            amount = amount,
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

    private fun upgrade(
        productId: String,
        planName: String,
        cycle: Int,
        amount: Long
    ) = ProductOfferList(
        metadata = ProductMetadata(
            productId = productId,
            customerId = "customer-42",
            planName = planName,
            entitlements = emptyList()
        ),
        header = ProductSelectionHeader(title = "Plan", description = "", cycleText = "", starred = false),
        offers = listOf(offer(productId, cycle, amount))
    )

    private companion object {

        const val MonthlyPlusId = "giapmail_mail2022_1_renewing"
        const val YearlyPlusId = "giapmail_mail2022_12_renewing"
        const val MonthlyUnlimitedId = "giapmail_bundle2022_1_renewing"
        const val YearlyUnlimitedId = "giapmail_bundle2022_12_renewing"
    }
}
