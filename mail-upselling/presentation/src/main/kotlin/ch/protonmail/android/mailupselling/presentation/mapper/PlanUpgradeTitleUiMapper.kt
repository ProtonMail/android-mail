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
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceDisplayUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import javax.inject.Inject

internal class PlanUpgradeTitleUiMapper @Inject constructor() {

    fun toUiModel(
        initialPrice: PlanUpgradePriceDisplayUiModel,
        upsellingEntryPoint: UpsellingEntryPoint.Feature,
        variant: PlanUpgradeVariant
    ): PlanUpgradeTitleUiModel {

        if (variant == PlanUpgradeVariant.SocialProof)
            return PlanUpgradeTitleUiModel(TextUiModel(R.string.upselling_mailbox_plus_title_social_proof))

        if (variant == PlanUpgradeVariant.Normal.Unlimited) {
            return PlanUpgradeTitleUiModel(TextUiModel(R.string.upselling_unlimited_title))
        }

        val stringResource = when (upsellingEntryPoint) {
            UpsellingEntryPoint.Feature.AutoDelete -> R.string.upselling_auto_delete_plus_title
            UpsellingEntryPoint.Feature.ContactGroups -> R.string.upselling_contact_groups_plus_title
            UpsellingEntryPoint.Feature.Folders -> R.string.upselling_folders_plus_title
            UpsellingEntryPoint.Feature.Labels -> R.string.upselling_labels_plus_title
            UpsellingEntryPoint.Feature.MobileSignature -> R.string.upselling_mobile_signature_plus_title
            UpsellingEntryPoint.Feature.ScheduleSend -> R.string.upselling_schedule_send_plus_title
            UpsellingEntryPoint.Feature.Snooze -> R.string.upselling_snooze_plus_title
            UpsellingEntryPoint.Feature.Sidebar,
            UpsellingEntryPoint.Feature.Navbar -> if (variant == PlanUpgradeVariant.IntroductoryPrice) {
                R.string.upselling_mailbox_plus_promo_title
            } else {
                R.string.upselling_mailbox_plus_title
            }
        }

        val isNavbarOrSidebar = upsellingEntryPoint in listOf(
            UpsellingEntryPoint.Feature.Navbar,
            UpsellingEntryPoint.Feature.Sidebar
        )

        val textUiModel = if (isNavbarOrSidebar && variant == PlanUpgradeVariant.IntroductoryPrice) {
            TextUiModel.TextResWithArgs(stringResource, listOf(initialPrice.highlightedPrice.getShorthandFormat()))
        } else {
            TextUiModel(stringResource)
        }

        return PlanUpgradeTitleUiModel(textUiModel)
    }
}
