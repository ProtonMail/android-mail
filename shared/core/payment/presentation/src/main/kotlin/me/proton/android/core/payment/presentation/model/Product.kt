/*
 * Copyright (C) 2025 Proton AG
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

package me.proton.android.core.payment.presentation.model

import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.domain.model.ProductEntitlement
import me.proton.android.core.payment.domain.model.ProductOfferToken

data class Product(
    val planName: String,
    val productId: String,
    val accountId: String,
    val cycle: Int,
    val header: ProductDetailHeader,
    val offerToken: ProductOfferToken,
    val entitlements: List<ProductEntitlement>,
    val renewalText: String?,
    val amount: Long = 0L,
    val currency: String = ""
) {

    companion object {

        private val dummyOfferToken = ProductOfferToken("test")

        val test = Product(
            planName = "bundle2022",
            productId = "giapmail_bundle2022_12_renewing",
            accountId = "cus_google_1234",
            cycle = 12,
            header = ProductDetailHeader(
                title = "Proton Unlimited",
                description = "Comprehensive privacy and security with all Proton services combined.",
                priceText = "CHF99.00",
                cycleText = "Per year",
                starred = true
            ),
            offerToken = dummyOfferToken,
            entitlements = listOf(
                ProductEntitlement.Description(
                    iconName = null,
                    text = "1 of 1 user",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "1 of 1 address",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "500 GB storage",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "And the free features of all other Proton products",
                    hint = ""
                )
            ),
            renewalText = "Welcome offer. Auto renews at CHF 129.48/year",
            amount = 99_000_000L,
            currency = "CHF"
        )

        val test_mail2022_1 = Product(
            planName = "mail2022",
            productId = "giapmail_mail2022_1_renewing",
            accountId = "cus_google_1234",
            cycle = 1,
            header = ProductDetailHeader(
                title = "Mail Plus",
                description = "Secure email with advanced features for your everyday communication.",
                priceText = "CHF0.99",
                cycleText = "Per month",
                starred = false
            ),
            offerToken = dummyOfferToken,
            entitlements = listOf(
                ProductEntitlement.Description(
                    iconName = null,
                    text = "15 GB storage",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "10 email addresses",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Support for 1 custom email domain",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Unlimited folders, labels, and filters",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "25 personal calendars",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "And the free features of all other Proton products",
                    hint = ""
                )
            ),
            renewalText = "Welcome offer. Auto renews at CHF 5.39/month",
            amount = 990_000L,
            currency = "CHF"
        )

        val test_pass2022_1 = Product(
            planName = "pass2022",
            productId = "giappass_pass2022_1_renewing",
            accountId = "cus_google_1234",
            cycle = 1,
            header = ProductDetailHeader(
                title = "Pass Plus",
                description = "For next-level password management and identity protection.",
                priceText = "CHF0.99",
                cycleText = "Per month",
                starred = false
            ),
            offerToken = dummyOfferToken,
            entitlements = listOf(
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Unlimited logins and notes",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Unlimited devices",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "20 vaults",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Share each vault with up to 10 people",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Unlimited email aliases",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Integrated 2FA authenticator",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Custom fields",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "Priority support",
                    hint = ""
                ),
                ProductEntitlement.Description(
                    iconName = null,
                    text = "And the free features of all other Proton products",
                    hint = ""
                )
            ),
            renewalText = "Welcome offer. Auto renews at CHF 5.39/month",
            amount = 990_000L,
            currency = "CHF"
        )
    }
}
