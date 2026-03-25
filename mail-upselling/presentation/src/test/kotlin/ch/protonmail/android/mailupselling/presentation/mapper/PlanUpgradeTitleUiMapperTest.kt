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

import java.math.BigDecimal
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceDisplayUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradePriceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeTitleUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlanUpgradeTitleUiMapperTest {

    private val mapper = PlanUpgradeTitleUiMapper()

    @Test
    fun `should map to the corresponding ui models`() {
        // Given
        val expected = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_contact_groups_plus_title)
            ),
            UpsellingEntryPoint.Feature.Folders to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_folders_plus_title)
            ),
            UpsellingEntryPoint.Feature.Labels to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_labels_plus_title)
            ),
            UpsellingEntryPoint.Feature.MobileSignature to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_mobile_signature_plus_title)
            ),
            UpsellingEntryPoint.Feature.Navbar to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_mailbox_plus_title)
            ),
            UpsellingEntryPoint.Feature.AutoDelete to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_auto_delete_plus_title)
            ),
            UpsellingEntryPoint.Feature.Navbar to PlanUpgradeTitleUiModel(
                TextUiModel(
                    R.string.upselling_mailbox_plus_promo_title,
                    initialPrice.highlightedPrice.getShorthandFormat()
                )
            ),
            UpsellingEntryPoint.Feature.ScheduleSend to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_schedule_send_plus_title)
            ),
            UpsellingEntryPoint.Feature.Snooze to PlanUpgradeTitleUiModel(
                TextUiModel(R.string.upselling_snooze_plus_title)
            )
        )

        // When
        val actual = mapOf(
            UpsellingEntryPoint.Feature.ContactGroups to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.ContactGroups,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.Folders to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Folders,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.Labels to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Labels,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.MobileSignature to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.MobileSignature,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.Navbar to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.AutoDelete to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.AutoDelete,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.Navbar to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Navbar,
                variant = PlanUpgradeVariant.IntroductoryPrice
            ),
            UpsellingEntryPoint.Feature.ScheduleSend to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.ScheduleSend,
                variant = PlanUpgradeVariant.Normal
            ),
            UpsellingEntryPoint.Feature.Snooze to mapper.toUiModel(
                initialPrice = initialPrice,
                upsellingEntryPoint = UpsellingEntryPoint.Feature.Snooze,
                variant = PlanUpgradeVariant.Normal
            )
        )

        // Then
        for (pair in expected) {
            assertEquals(pair.value, actual[pair.key])
        }
    }

    private companion object {

        val initialPrice = PlanUpgradePriceDisplayUiModel(
            pricePerCycle = PlanUpgradePriceUiModel(rawAmount = BigDecimal(4.5), currencyCode = "EUR"),
            highlightedPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(54), currencyCode = "EUR"),
            secondaryPrice = PlanUpgradePriceUiModel(rawAmount = BigDecimal(108), currencyCode = "EUR")
        )
    }
}
