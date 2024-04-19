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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanTitleUiMapper
import ch.protonmail.android.mailupselling.presentation.model.DynamicPlanTitleUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DynamicPlanTitleUiMapperTest {

    private val mapper = DynamicPlanTitleUiMapper()

    @Test
    fun `should map to the corresponding ui model`() {
        // Given
        val title = "Title"
        val expected = DynamicPlanTitleUiModel(TextUiModel.Text(title))

        // When
        val actual = mapper.toUiModel(title)

        // Then
        assertEquals(expected, actual)
    }
}
