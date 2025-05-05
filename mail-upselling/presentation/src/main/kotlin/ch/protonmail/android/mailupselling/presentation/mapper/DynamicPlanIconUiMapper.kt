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

import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanIconUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlansVariant
import javax.inject.Inject

internal class DynamicPlanIconUiMapper @Inject constructor() {

    @Suppress("MaxLineLength")
    fun toUiModel(
        upsellingEntryPoint: UpsellingEntryPoint.Feature,
        variant: DynamicPlansVariant
    ): DynamicPlanIconUiModel {
        if (variant == DynamicPlansVariant.SocialProof) {
            return DynamicPlanIconUiModel(R.drawable.ic_mail_social_proof)
        }
        return when (upsellingEntryPoint) {
            UpsellingEntryPoint.Feature.ContactGroups -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_contact_groups)
            UpsellingEntryPoint.Feature.Folders -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels)
            UpsellingEntryPoint.Feature.Labels -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels)
            UpsellingEntryPoint.Feature.MobileSignature -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_mobile_signature)
            UpsellingEntryPoint.Feature.MailboxPromo -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox_promo)
            UpsellingEntryPoint.Feature.Mailbox,
            UpsellingEntryPoint.Feature.Navbar -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox)

            UpsellingEntryPoint.Feature.AutoDelete -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_auto_delete)
        }
    }
}
