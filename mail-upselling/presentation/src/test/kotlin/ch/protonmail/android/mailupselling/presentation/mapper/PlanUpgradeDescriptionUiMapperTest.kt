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

package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.testdata.upselling.UpsellingTestData
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class PlanUpgradeDescriptionUiMapperTest(
    @Suppress("unused") private val testName: String,
    private val product: ProductOfferDetail,
    private val entryPoint: UpsellingEntryPoint.Feature,
    private val variant: PlanUpgradeVariant,
    private val expected: PlanUpgradeDescriptionUiModel
) {

    private val mapper = PlanUpgradeDescriptionUiMapper()

    @Test
    fun `should return the default description`() {

        // When
        val actual = mapper.toUiModel(
            productDetail = product,
            upsellingEntryPoint = entryPoint,
            variant = variant
        )

        // Then
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "mailbox entry point - normal MailPlus variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mailbox_plus_description_override)
                )
            ),
            arrayOf(
                "mailbox entry point - normal Unlimited variant",
                UpsellingTestData.UnlimitedMailProduct.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.Normal.Unlimited,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_unlimited_description_override)
                )
            ),
            arrayOf(
                "mailbox entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mailbox_plus_promo_description_override)
                )
            ),
            arrayOf(
                "mailbox entry point - social proof variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.SocialProof,
                PlanUpgradeDescriptionUiModel.SocialProof
            ),
            arrayOf(
                "contact groups entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.ContactGroups,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_contact_groups_plus_description_override)
                )
            ),
            arrayOf(
                "contact groups entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.ContactGroups,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_contact_groups_plus_description_override)
                )
            ),
            arrayOf(
                "folders entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Folders,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_folders_plus_description_override)
                )
            ),
            arrayOf(
                "folders entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Folders,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_folders_plus_description_override)
                )
            ),
            arrayOf(
                "labels entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Labels,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_labels_plus_description_override)
                )
            ),
            arrayOf(
                "labels entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Labels,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_labels_plus_description_override)
                )
            ),
            arrayOf(
                "navbar entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mailbox_plus_description_override)
                )
            ),
            arrayOf(
                "navbar entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Navbar,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mailbox_plus_promo_description_override)
                )
            ),
            arrayOf(
                "mobile signature entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.MobileSignature,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mobile_signature_plus_description_override)
                )
            ),
            arrayOf(
                "mobile signature entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.MobileSignature,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_mobile_signature_plus_description_override)
                )
            ),
            arrayOf(
                "auto delete entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.AutoDelete,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_auto_delete_plus_description_override)
                )
            ),
            arrayOf(
                "auto delete entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.AutoDelete,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_auto_delete_plus_description_override)
                )
            ),
            arrayOf(
                "schedule send entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.ScheduleSend,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_schedule_send_plus_description_override)
                )
            ),
            arrayOf(
                "schedule send entry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.ScheduleSend,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_schedule_send_plus_description_override)
                )
            ),
            arrayOf(
                "snooze entry point - normal variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Snooze,
                PlanUpgradeVariant.Normal.MailPlus,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_snooze_plus_description_override)
                )
            ),
            arrayOf(
                "snoozeentry point - promo variant",
                UpsellingTestData.MailPlusProducts.MonthlyProductOfferDetail,
                UpsellingEntryPoint.Feature.Snooze,
                PlanUpgradeVariant.IntroductoryPrice,
                PlanUpgradeDescriptionUiModel.Simple(
                    TextUiModel.TextRes(R.string.upselling_snooze_plus_description_override)
                )
            )
        )
    }
}

