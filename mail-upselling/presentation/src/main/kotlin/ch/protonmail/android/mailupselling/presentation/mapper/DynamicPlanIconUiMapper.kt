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
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanIconUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import javax.inject.Inject

internal class DynamicPlanIconUiMapper @Inject constructor() {

    @Suppress("MaxLineLength")
    fun toUiModel(upsellingEntryPoint: UpsellingEntryPoint.BottomSheet): DynamicPlanIconUiModel =
        when (upsellingEntryPoint) {
            UpsellingEntryPoint.BottomSheet.ContactGroups -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_contact_groups)
            UpsellingEntryPoint.BottomSheet.Folders -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels)
            UpsellingEntryPoint.BottomSheet.Labels -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels)
            UpsellingEntryPoint.BottomSheet.MobileSignature -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_mobile_signature)
            UpsellingEntryPoint.BottomSheet.Mailbox -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox)
            UpsellingEntryPoint.BottomSheet.AutoDelete -> DynamicPlanIconUiModel(R.drawable.illustration_upselling_auto_delete)
        }
}
