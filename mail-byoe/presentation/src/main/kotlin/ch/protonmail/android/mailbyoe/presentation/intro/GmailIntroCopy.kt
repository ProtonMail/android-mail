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

package ch.protonmail.android.mailbyoe.presentation.intro

import androidx.annotation.StringRes
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.presentation.R

/**
 * The copy of the Gmail intro that differs between the experiment treatments. The title, the import row and the
 * buttons are the same for both.
 */
internal data class GmailIntroCopy(
    @StringRes val subtitle: Int,
    val benefits: List<Int>,
    @StringRes val permissionsNote: Int
) {

    companion object {

        fun of(treatment: ByoeTreatment) = when (treatment) {
            ByoeTreatment.Treatment1 -> GmailIntroCopy(
                subtitle = R.string.byoe_spotlight_subtitle,
                benefits = listOf(
                    R.string.byoe_spotlight_benefit_read,
                    R.string.byoe_spotlight_benefit_no_ads,
                    R.string.byoe_spotlight_benefit_no_tracking
                ),
                permissionsNote = R.string.byoe_spotlight_permissions_note
            )

            ByoeTreatment.Treatment2 -> GmailIntroCopy(
                subtitle = R.string.byoe_spotlight_treatment2_subtitle,
                benefits = listOf(
                    R.string.byoe_spotlight_treatment2_benefit_gmail_address,
                    R.string.byoe_spotlight_treatment2_benefit_no_edit,
                    R.string.byoe_spotlight_treatment2_benefit_disconnect
                ),
                permissionsNote = R.string.byoe_spotlight_treatment2_permissions_note
            )
        }
    }
}
