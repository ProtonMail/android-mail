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
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeIds
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeDescriptionUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import me.proton.android.core.payment.domain.model.ProductOfferDetail
import javax.inject.Inject

internal class PlanUpgradeDescriptionUiMapper @Inject constructor() {

    fun toUiModel(
        productDetail: ProductOfferDetail,
        upsellingEntryPoint: UpsellingEntryPoint.Feature,
        variant: PlanUpgradeVariant
    ): PlanUpgradeDescriptionUiModel {
        if (variant == PlanUpgradeVariant.SocialProof) {
            return PlanUpgradeDescriptionUiModel.SocialProof
        }

        val description = when (productDetail.metadata.planName) {
            PlanUpgradeIds.UnlimitedPlanId -> getUnlimitedDescription()
            PlanUpgradeIds.PlusPlanId -> getPlusDescription(variant, upsellingEntryPoint)
            else -> getDefaultDescription(productDetail)
        }

        return PlanUpgradeDescriptionUiModel.Simple(description)
    }

    private fun getDefaultDescription(productDetail: ProductOfferDetail) =
        TextUiModel.Text(productDetail.header.description)
    private fun getUnlimitedDescription() = TextUiModel.TextRes(R.string.upselling_unlimited_description_override)
    private fun getPlusDescription(
        variant: PlanUpgradeVariant,
        upsellingEntryPoint: UpsellingEntryPoint.Feature
    ): TextUiModel.TextRes {

        val stringRes = when (upsellingEntryPoint) {
            UpsellingEntryPoint.Feature.AutoDelete -> R.string.upselling_auto_delete_plus_description_override
            UpsellingEntryPoint.Feature.ContactGroups -> R.string.upselling_contact_groups_plus_description_override
            UpsellingEntryPoint.Feature.Folders -> R.string.upselling_folders_plus_description_override
            UpsellingEntryPoint.Feature.Labels -> R.string.upselling_labels_plus_description_override
            UpsellingEntryPoint.Feature.MobileSignature -> R.string.upselling_mobile_signature_plus_description_override
            UpsellingEntryPoint.Feature.ScheduleSend -> R.string.upselling_schedule_send_plus_description_override
            UpsellingEntryPoint.Feature.Snooze -> R.string.upselling_snooze_plus_description_override

            UpsellingEntryPoint.Feature.PlusUnlimited,
            UpsellingEntryPoint.Feature.Sidebar,
            UpsellingEntryPoint.Feature.Navbar -> if (variant == PlanUpgradeVariant.IntroductoryPrice) {
                R.string.upselling_mailbox_plus_promo_description_override
            } else {
                R.string.upselling_mailbox_plus_description_override
            }
        }

        return TextUiModel.TextRes(stringRes)
    }
}
