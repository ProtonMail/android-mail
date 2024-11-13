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

package ch.protonmail.android.mailupselling.presentation.ui.screen.entitlements.comparisontable

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlement
import ch.protonmail.android.mailupselling.presentation.model.comparisontable.ComparisonTableEntitlementItemUiModel

internal object ComparisonTableElementPreviewData {

    val Entitlements = listOf(
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_storage),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Plus.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_storage_value_plus)
            )
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses),
            freeValue = ComparisonTableEntitlement.Free.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_free)
            ),
            paidValue = ComparisonTableEntitlement.Plus.Value(
                TextUiModel.TextRes(R.string.upselling_comparison_table_email_addresses_value_plus)
            )
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_custom_email_domain),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Plus.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_desktop_app),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Plus.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_unlimited_folders_labels),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Plus.Present
        ),
        ComparisonTableEntitlementItemUiModel(
            title = TextUiModel.TextRes(R.string.upselling_comparison_table_priority_support),
            freeValue = ComparisonTableEntitlement.Free.NotPresent,
            paidValue = ComparisonTableEntitlement.Plus.Present
        )
    )
}
