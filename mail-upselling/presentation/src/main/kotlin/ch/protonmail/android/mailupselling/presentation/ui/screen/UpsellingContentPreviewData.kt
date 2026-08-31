/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlements as ComparisonTableEntitlementsData
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeIconUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PromoKind
import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.domain.model.ProductOfferToken
import me.proton.android.core.payment.presentation.model.Product
import java.math.BigDecimal

internal object UpsellingContentPreviewData {

    private val MailPlusPlanModelMonthly = PlanUpgradeInstanceUiModel.Standard(
        name = "Mail Plus",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        totalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        discountRate = null,
        cycle = PlanUpgradeCycle.Monthly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 1,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )

    private val mailMonthlyPromoParams = PlanUpgradeInstanceUiModel.Promotional.Params(
        name = "Mail Plus",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(5.99), currencyCode = "EUR"),
        promotionalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        renewalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(5.99), currencyCode = "EUR"),
        discountRate = null,
        cycle = PlanUpgradeCycle.Monthly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 1,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )

    private val MailPlusPlanModelMonthlyPromo = PlanUpgradeInstanceUiModel.Promotional(
        promoKind = PromoKind.IntroPrice,
        params = mailMonthlyPromoParams
    )

    private val MailPlusPlanModelYearly = PlanUpgradeInstanceUiModel.Standard(
        name = "Mail Plus",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        totalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(49.99), currencyCode = "EUR"),
        discountRate = null,
        cycle = PlanUpgradeCycle.Yearly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 1,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )

    private val mailYearlyPromoParams = PlanUpgradeInstanceUiModel.Promotional.Params(
        name = "Mail Plus",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(49.99), currencyCode = "EUR"),
        promotionalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(39.99), currencyCode = "EUR"),
        renewalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(49.99), currencyCode = "EUR"),
        discountRate = 20,
        cycle = PlanUpgradeCycle.Monthly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 1,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )

    private val MailPlusPlanModelYearlyPromo = PlanUpgradeInstanceUiModel.Promotional(
        promoKind = PromoKind.IntroPrice,
        params = mailYearlyPromoParams
    )

    private val UnlimitedPlanModelMonthly = PlanUpgradeInstanceUiModel.Standard(
        name = "Proton Unlimited",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        totalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.99), currencyCode = "EUR"),
        discountRate = null,
        cycle = PlanUpgradeCycle.Monthly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 1,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )
    private val UnlimitedPlanModelYearly = PlanUpgradeInstanceUiModel.Standard(
        name = "Proton Unlimited",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal("9.99"), currencyCode = "EUR"),
        totalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal("119.88"), currencyCode = "EUR"),
        discountRate = null,
        cycle = PlanUpgradeCycle.Yearly,
        yearlySaving = null,
        product = Product(
            planName = "Plan name",
            productId = "123",
            accountId = "456",
            cycle = 12,
            header = ProductDetailHeader("Title", "Description", "EUR 12.99", "Cycle text", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = null,
            amount = 4_990_000L,
            currency = "EUR"
        )
    )

    val NormalList = PlanUpgradeInstanceListUiModel.Data.StandardMailPlus(
        MailPlusPlanModelMonthly,
        MailPlusPlanModelYearly
    )

    val SocialProofList = PlanUpgradeInstanceListUiModel.Data.SocialProof(
        MailPlusPlanModelMonthly,
        MailPlusPlanModelYearly
    )

    val PlusToUnlimitedList = PlanUpgradeInstanceListUiModel.Data.PlusToUnlimited(
        UnlimitedPlanModelMonthly,
        UnlimitedPlanModelYearly
    )

    val PromoList = PlanUpgradeInstanceListUiModel.Data.IntroPrice(
        MailPlusPlanModelMonthlyPromo,
        MailPlusPlanModelYearlyPromo
    )

    val BlackFridayList = PlanUpgradeInstanceListUiModel.Data.BlackFriday(
        blackFridayVariant = PlanUpgradeVariant.BlackFriday.Wave1,
        UnlimitedPlanModelMonthly,
        UnlimitedPlanModelYearly
    )

    private val summerYearlyPromoParams = PlanUpgradeInstanceUiModel.Promotional.Params(
        name = "Mail Plus",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal("35.88"), currencyCode = "USD"),
        promotionalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal("35.88"), currencyCode = "USD"),
        renewalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal("47.88"), currencyCode = "USD"),
        discountRate = 40,
        cycle = PlanUpgradeCycle.Yearly,
        yearlySaving = null,
        product = Product(
            planName = "Mail Plus",
            productId = "summer-yearly",
            accountId = "456",
            cycle = 12,
            header = ProductDetailHeader("Mail Plus", "12 months", "USD 35.88", "/year", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = "Auto-renews at \$155.76/year",
            amount = 35_880_000L,
            currency = "USD"
        )
    )

    private val SummerYearlyPromo = PlanUpgradeInstanceUiModel.Promotional(
        promoKind = PromoKind.SummerCampaign,
        params = summerYearlyPromoParams
    )

    val SummerCampaignList = PlanUpgradeInstanceListUiModel.Data.SummerCampaign(
        summerCampaignVariant = PlanUpgradeVariant.SummerCampaign.Wave1,
        shorterCycle = MailPlusPlanModelMonthly,
        longerCycle = SummerYearlyPromo
    )

    private val fallPromoYearlyPromoParams = PlanUpgradeInstanceUiModel.Promotional.Params(
        name = "Proton Unlimited",
        pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal("77.88"), currencyCode = "USD"),
        promotionalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal("77.88"), currencyCode = "USD"),
        renewalPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal("155.76"), currencyCode = "USD"),
        discountRate = 50,
        cycle = PlanUpgradeCycle.Yearly,
        yearlySaving = null,
        product = Product(
            planName = "Proton Unlimited",
            productId = "fall-promo-yearly",
            accountId = "456",
            cycle = 12,
            header = ProductDetailHeader("Proton Unlimited", "12 months", "USD 77.88", "/year", false),
            offerToken = ProductOfferToken(""),
            entitlements = emptyList(),
            renewalText = "Auto-renews at \$155.76/year",
            amount = 77_880_000L,
            currency = "USD"
        )
    )

    private val FallPromoYearlyPromo = PlanUpgradeInstanceUiModel.Promotional(
        promoKind = PromoKind.FallPromo,
        params = fallPromoYearlyPromoParams
    )

    val FallPromoList = PlanUpgradeInstanceListUiModel.Data.FallPromo(
        fallPromoVariant = PlanUpgradeVariant.FallPromo.Wave1,
        shorterCycle = UnlimitedPlanModelMonthly,
        longerCycle = FallPromoYearlyPromo
    )

    val SimpleListEntitlements = PlanUpgradeEntitlementsListUiModel.SimpleList(
        listOf(
            PlanUpgradeEntitlementListUiModel.Local(
                text = TextUiModel.Text("Entitlement 1"),
                localResource = R.drawable.ic_upselling_pass
            ),
            PlanUpgradeEntitlementListUiModel.Local(
                text = TextUiModel.Text("Entitlement 2"),
                localResource = R.drawable.ic_upselling_mail
            ),
            PlanUpgradeEntitlementListUiModel.Local(
                text = TextUiModel.Text("Entitlement 3"),
                localResource = R.drawable.ic_upselling_gift
            )
        )
    )

    val FallPromoComparisonTableEntitlements = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
        ComparisonTableEntitlementsData.FallPromoEntitlements
    )

    val UnlimitedComparisonTableEntitlements = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
        ComparisonTableEntitlementsData.UnlimitedEntitlements
    )

    val ComparisonTableEntitlements = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
        listOf(
            ComparisonTableEntitlementItemUiModel(
                title = TextUiModel.Text("Item 1"),
                freeValue = ComparisonTableEntitlement.Free.NotPresent,
                paidValue = ComparisonTableEntitlement.Paid.Present
            ),
            ComparisonTableEntitlementItemUiModel(
                title = TextUiModel.Text("Item 2"),
                freeValue = ComparisonTableEntitlement.Free.NotPresent,
                paidValue = ComparisonTableEntitlement.Paid.Present
            ),
            ComparisonTableEntitlementItemUiModel(
                title = TextUiModel.Text("Item 3"),
                freeValue = ComparisonTableEntitlement.Free.NotPresent,
                paidValue = ComparisonTableEntitlement.Paid.Present
            )
        )
    )

    val Base = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.illustration_upselling_mailbox),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.Simple(TextUiModel.Text("Description")),
            entitlements = SimpleListEntitlements,
            variant = PlanUpgradeVariant.Normal.MailPlus,
            list = NormalList
        )
    )

    val IntroductoryPrice = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.illustration_upselling_mailbox),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.Simple(
                TextUiModel.Text("To unlock more storage and premium features")
            ),
            entitlements = SimpleListEntitlements,
            variant = PlanUpgradeVariant.IntroductoryPrice,
            list = PromoList
        )
    )

    val BlackFriday = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.upselling_bf_header_wave2),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.Simple(
                TextUiModel.Text("To unlock more storage and premium features")
            ),
            entitlements = ComparisonTableEntitlements,
            variant = PlanUpgradeVariant.BlackFriday.Wave1,
            list = BlackFridayList
        )
    )

    val SpringPromo = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.spring_promo_header),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.Simple(
                TextUiModel.Text("To unlock more storage and premium features")
            ),
            entitlements = ComparisonTableEntitlements,
            variant = PlanUpgradeVariant.SpringPromo.Wave1,
            list = BlackFridayList
        )
    )

    val SummerCampaign = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.summer_campaign_bg),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.Simple(
                TextUiModel.Text("To unlock more storage and premium features")
            ),
            entitlements = ComparisonTableEntitlements,
            variant = PlanUpgradeVariant.SummerCampaign.Wave1,
            list = SummerCampaignList
        )
    )

    val FallPromo = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.fall_promo_bg),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Proton Unlimited")),
            description = PlanUpgradeDescriptionUiModel.Simple(
                TextUiModel.Text("To unlock more storage and premium features")
            ),
            entitlements = FallPromoComparisonTableEntitlements,
            variant = PlanUpgradeVariant.FallPromo.Wave1,
            list = FallPromoList
        )
    )

    val SocialProof = UpsellingScreenContentState.Data(
        PlanUpgradeUiModel(
            icon = PlanUpgradeIconUiModel(R.drawable.ic_mail_social_proof),
            title = PlanUpgradeTitleUiModel(TextUiModel.Text("Upgrade to Mail Plus")),
            description = PlanUpgradeDescriptionUiModel.SocialProof,
            entitlements = SimpleListEntitlements,
            variant = PlanUpgradeVariant.SocialProof,
            list = SocialProofList
        )
    )

    val PlusUnlimitedThemes = UpsellContentTheme.entries.map { theme ->
        UpsellingScreenContentState.Data(
            PlanUpgradeUiModel(
                icon = PlanUpgradeIconUiModel(theme.headerDrawable),
                title = PlanUpgradeTitleUiModel(TextUiModel.TextRes(theme.titleRes)),
                description = PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_unlimited_description_override)
                ),
                entitlements = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
                    items = ComparisonTableEntitlementsData.PlusToUnlimitedEntitlements,
                    baseColumnLabel = TextUiModel.TextRes(R.string.upselling_plus_plan)
                ),
                variant = PlanUpgradeVariant.Normal.Unlimited,
                list = PlusToUnlimitedList
            )
        )
    }
}
