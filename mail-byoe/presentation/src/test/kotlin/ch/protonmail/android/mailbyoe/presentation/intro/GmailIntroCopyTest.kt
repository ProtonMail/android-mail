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

import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.presentation.R
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GmailIntroCopyTest {

    @Test
    fun `given treatment 1, when the intro shows, then it uses the reassuring copy`() {
        // When
        val copy = GmailIntroCopy.of(ByoeTreatment.Treatment1)

        // Then
        val expected = GmailIntroCopy(
            subtitle = R.string.byoe_spotlight_subtitle,
            benefits = listOf(
                R.string.byoe_spotlight_benefit_read,
                R.string.byoe_spotlight_benefit_no_ads,
                R.string.byoe_spotlight_benefit_no_tracking
            ),
            permissionsNote = R.string.byoe_spotlight_permissions_note
        )
        assertEquals(expected, copy)
    }

    @Test
    fun `given treatment 2, when the intro shows, then it uses the value proposition copy`() {
        // When
        val copy = GmailIntroCopy.of(ByoeTreatment.Treatment2)

        // Then
        val expected = GmailIntroCopy(
            subtitle = R.string.byoe_spotlight_treatment2_subtitle,
            benefits = listOf(
                R.string.byoe_spotlight_treatment2_benefit_gmail_address,
                R.string.byoe_spotlight_treatment2_benefit_no_edit,
                R.string.byoe_spotlight_treatment2_benefit_disconnect
            ),
            permissionsNote = R.string.byoe_spotlight_treatment2_permissions_note
        )
        assertEquals(expected, copy)
    }
}
