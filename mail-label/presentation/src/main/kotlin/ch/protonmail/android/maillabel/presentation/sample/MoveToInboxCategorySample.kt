/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.maillabel.presentation.sample

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetDestinationUiModel

object MoveToInboxCategorySample {

    val primary = build(
        id = CategorySystemLabelId.Primary,
        text = "Primary",
        iconTint = Color(0xFF6D4AFF)
    )
    val social = build(
        id = CategorySystemLabelId.Social,
        text = "Social",
        iconTint = Color(0xFF1C9DBB)
    )
    val promotions = build(
        id = CategorySystemLabelId.Promotions,
        text = "Promotions",
        iconTint = Color(0xFF3BA36B)
    )
    val newsletters = build(
        id = CategorySystemLabelId.Newsletter,
        text = "Newsletter",
        iconTint = Color(0xFFE15922)
    )
    val transactions = build(
        id = CategorySystemLabelId.Transactions,
        text = "Transactions",
        iconTint = Color(0xFFF2364E)
    )
    val updates = build(
        id = CategorySystemLabelId.Updates,
        text = "Updates",
        iconTint = Color(0xFFF22287)
    )
    val forums = build(
        id = CategorySystemLabelId.Forums,
        text = "Forums",
        iconTint = Color(0xFF2C96F5)
    )

    val all = listOf(primary, social, promotions, newsletters, transactions, updates, forums)

    private fun build(
        id: CategorySystemLabelId,
        text: String,
        iconTint: Color
    ) = MoveToBottomSheetDestinationUiModel.Inbox.Category(
        id = MailLabelId.Category(id.labelId),
        text = TextUiModel.Text(text),
        icon = R.drawable.ic_proton_circle_filled,
        iconTint = iconTint
    )
}

