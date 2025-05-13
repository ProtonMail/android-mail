/*
 * Copyright (c) 2022 Proton Technologies AG
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
import ch.protonmail.android.mailupselling.domain.annotations.ForceOneClickUpsellingDetailsOverride
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanDescriptionUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import me.proton.core.plan.domain.entity.DynamicPlan
import javax.inject.Inject

internal class DynamicPlanDescriptionUiMapper @Inject constructor(
    @ForceOneClickUpsellingDetailsOverride private val shouldOverrideEntitlementsList: Boolean
) {

    fun toUiModel(
        dynamicPlan: DynamicPlan,
        upsellingEntryPoint: UpsellingEntryPoint.Feature,
        variant: DynamicPlansVariant
    ): DynamicPlanDescriptionUiModel {
        if (variant == DynamicPlansVariant.SocialProof) {
            return DynamicPlanDescriptionUiModel.SocialProof
        }
        if (!shouldOverrideEntitlementsList)
            return DynamicPlanDescriptionUiModel.Simple(getDefaultDescription(dynamicPlan))

        val description = when (dynamicPlan.name) {
            DynamicPlansOneClickIds.UnlimitedPlanId -> getUnlimitedDescription()
            DynamicPlansOneClickIds.PlusPlanId -> getPlusDescription(upsellingEntryPoint)
            else -> getDefaultDescription(dynamicPlan)
        }

        return DynamicPlanDescriptionUiModel.Simple(description)
    }

    private fun getDefaultDescription(dynamicPlan: DynamicPlan) = TextUiModel.Text(dynamicPlan.description ?: "")
    private fun getUnlimitedDescription() = TextUiModel.TextRes(R.string.upselling_unlimited_description_override)
    private fun getPlusDescription(upsellingEntryPoint: UpsellingEntryPoint.Feature) = when (upsellingEntryPoint) {
        UpsellingEntryPoint.Feature.ContactGroups -> TextUiModel.TextRes(
            R.string.upselling_contact_groups_plus_description_override
        )
        UpsellingEntryPoint.Feature.Folders -> TextUiModel.TextRes(
            R.string.upselling_folders_plus_description_override
        )
        UpsellingEntryPoint.Feature.Labels -> TextUiModel.TextRes(
            R.string.upselling_labels_plus_description_override
        )
        UpsellingEntryPoint.Feature.MailboxPromo -> TextUiModel.TextRes(
            R.string.upselling_mailbox_plus_promo_description_override
        )
        UpsellingEntryPoint.Feature.Mailbox,
        UpsellingEntryPoint.Feature.Navbar -> TextUiModel.TextRes(
            R.string.upselling_mailbox_plus_description_override
        )
        UpsellingEntryPoint.Feature.MobileSignature -> TextUiModel.TextRes(
            R.string.upselling_mobile_signature_plus_description_override
        )
        UpsellingEntryPoint.Feature.AutoDelete -> TextUiModel.TextRes(
            R.string.upselling_auto_delete_plus_description_override
        )
    }
}
