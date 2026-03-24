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

package ch.protonmail.android.mailupselling.presentation.model.comparisontable

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R

internal object ComparisonTableEntitlements {

    val MailPlusEntitlements = listOf(
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Paid.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_plus)
            )
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Paid.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_plus)
            )
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_unlimited_folders_labels),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        )
    )

    val UnlimitedEntitlements = listOf(
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Paid.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_unlimited)
            )
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_aliases),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_aliases_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Paid.Unlimited
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_folders_labels),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_folders_labels_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Paid.Unlimited
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_premium_products),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_dark_web_monitoring),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Paid.Present
        )
    )
}
