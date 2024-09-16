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

package ch.protonmail.upselling.presentation.mapper

import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanIconUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanIconUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
internal class DynamicPlanIconUiMapperTest {

    private val mapper = DynamicPlanIconUiMapper()

    @Test
    fun `should map to the corresponding ui models`() {
        // Given
        val expected = mapOf(
            UpsellingEntryPoint.BottomSheet.ContactGroups to DynamicPlanIconUiModel(R.drawable.illustration_upselling_contact_groups),
            UpsellingEntryPoint.BottomSheet.Folders to DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels),
            UpsellingEntryPoint.BottomSheet.Labels to DynamicPlanIconUiModel(R.drawable.illustration_upselling_labels),
            UpsellingEntryPoint.BottomSheet.MobileSignature to DynamicPlanIconUiModel(R.drawable.illustration_upselling_mobile_signature),
            UpsellingEntryPoint.BottomSheet.Mailbox to DynamicPlanIconUiModel(R.drawable.illustration_upselling_mailbox)
        )

        // When
        val actual = mapOf(
            UpsellingEntryPoint.BottomSheet.ContactGroups to mapper.toUiModel(UpsellingEntryPoint.BottomSheet.ContactGroups),
            UpsellingEntryPoint.BottomSheet.Folders to mapper.toUiModel(UpsellingEntryPoint.BottomSheet.Folders),
            UpsellingEntryPoint.BottomSheet.Labels to mapper.toUiModel(UpsellingEntryPoint.BottomSheet.Labels),
            UpsellingEntryPoint.BottomSheet.MobileSignature to mapper.toUiModel(UpsellingEntryPoint.BottomSheet.MobileSignature),
            UpsellingEntryPoint.BottomSheet.Mailbox to mapper.toUiModel(UpsellingEntryPoint.BottomSheet.Mailbox)
        )

        // Then
        for (pair in expected) {
            assertEquals(pair.value, actual[pair.key])
        }
    }
}
