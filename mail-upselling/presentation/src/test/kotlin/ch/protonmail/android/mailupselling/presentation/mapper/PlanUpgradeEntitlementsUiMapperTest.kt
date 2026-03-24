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
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement.Free
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement.Paid
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeEntitlementsListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlanUpgradeEntitlementsUiMapperTest {

    private val mapper = PlanUpgradeEntitlementsUiMapper()

    @Test
    fun `should return MailPlus comparison table entitlements when requested `() {
        // Given
        val expected = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
            listOf(
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
                    freeValue = Free.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)),
                    paidValue = Paid.Value(TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_plus))
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses),
                    freeValue = Free.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_free)
                    ),
                    paidValue = Paid.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_plus)
                    )
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_unlimited_folders_labels),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                )
            )
        )

        // When
        val actual = mapper.toTableUiModel(PlanUpgradeVariant.Normal.MailPlus)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return Unlimited comparison table entitlements when requested `() {
        // Given
        val expected = PlanUpgradeEntitlementsListUiModel.ComparisonTableList(
            listOf(
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
                    freeValue = Free.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)
                    ),
                    paidValue = Paid.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_unlimited)
                    )
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_aliases),
                    freeValue = Free.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_aliases_value_free)
                    ),
                    paidValue = Paid.Unlimited
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_folders_labels),
                    freeValue = Free.Value(
                        TextUiModel.TextRes(R.string.upselling_comparison_table_folders_labels_value_free)
                    ),
                    paidValue = Paid.Unlimited
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_premium_products),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_dark_web_monitoring),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                ),
                ComparisonTableEntitlementItemUiModel(
                    title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
                    freeValue = Free.NotPresent,
                    paidValue = Paid.Present
                )
            )
        )

        // When
        val actual = mapper.toTableUiModel(PlanUpgradeVariant.Normal.Unlimited)

        // Then
        assertEquals(expected, actual)
    }
}
