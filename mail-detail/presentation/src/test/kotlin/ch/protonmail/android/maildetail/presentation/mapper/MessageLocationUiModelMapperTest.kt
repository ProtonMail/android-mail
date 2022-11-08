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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.iconRes
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageLocationUiModelMapperTest {

    private val messageLocationUiModelMapper = MessageLocationUiModelMapper()

    @Test
    fun `when an exclusive system label is found in the list of labels its name and icon are returned`() {
        // Given
        val labelIds = listOf(SystemLabelId.AllMail.labelId, SystemLabelId.Archive.labelId)
        val expectedResult = MessageLocationUiModel(
            SystemLabelId.Archive.name,
            SystemLabelId.enumOf(SystemLabelId.Archive.labelId.id).iconRes()
        )
        // When
        val result = messageLocationUiModelMapper(labelIds)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when no exclusive label has been found then the name and icon of the all mail location are returned`() {
        // Given
        val labelIds = listOf(SystemLabelId.AllMail.labelId)
        val expectedResult = MessageLocationUiModel(
            SystemLabelId.AllMail.name,
            SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
        )
        // When
        val result = messageLocationUiModelMapper(labelIds)
        // Then
        assertEquals(expectedResult, result)
    }
}
