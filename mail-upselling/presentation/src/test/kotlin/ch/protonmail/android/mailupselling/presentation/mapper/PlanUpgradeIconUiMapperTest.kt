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

import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeIconUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlanUpgradeIconUiMapperTest {

    private val mapper = PlanUpgradeIconUiMapper()

    @Test
    fun `should map to the corresponding ui models`() {
        // Given
        val expected = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_contact_groups
            ),
            UpsellingEntryPoint.Feature.Folders to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_labels
            ),
            UpsellingEntryPoint.Feature.Labels to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_labels
            ),
            UpsellingEntryPoint.Feature.MobileSignature to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_mobile_signature
            ),
            UpsellingEntryPoint.Feature.Navbar to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_mailbox
            ),
            UpsellingEntryPoint.Feature.Navbar to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_unlimited
            ),
            UpsellingEntryPoint.Feature.Sidebar to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_mailbox
            ),
            UpsellingEntryPoint.Feature.AutoDelete to PlanUpgradeIconUiModel(
                iconResId = R.drawable.illustration_upselling_auto_delete
            ),
            UpsellingEntryPoint.Feature.Navbar to PlanUpgradeIconUiModel(
                iconResId = R.drawable.ic_mail_social_proof
            )
        )

        // When
        val actual = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.ContactGroups,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.Folders to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Folders,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.Labels to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Labels,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.MobileSignature to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.MobileSignature,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.Navbar to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.Navbar to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar,
                variant = PlanUpgradeVariant.Normal.Unlimited
            ),
            UpsellingEntryPoint.Feature.Sidebar to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Sidebar,
                variant = PlanUpgradeVariant.IntroductoryPrice
            ),
            UpsellingEntryPoint.Feature.AutoDelete to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.AutoDelete,
                variant = PlanUpgradeVariant.Normal.MailPlus
            ),
            UpsellingEntryPoint.Feature.Navbar to mapper.toUiModel(
                upsellingEntryPoint = UpsellingEntryPoint.Feature.AutoDelete,
                variant = PlanUpgradeVariant.SocialProof
            )
        )

        // Then
        for (pair in expected) {
            assertEquals(pair.value, actual[pair.key])
        }
    }
}
