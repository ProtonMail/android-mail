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

package ch.protonmail.android.maildetail.domain.mapper

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageWithLabelsMapperTest {

    private val message = MessageTestData.buildMessage(id = "messageId")
    private val labels = listOf(
        LabelTestData.buildLabel(id = SystemLabelId.Archive.labelId.id),
        LabelTestData.buildLabel(id = "CustomLabelId")
    )

    private val messageWithLabelsMapper = MessageWithLabelsMapper()

    @Test
    fun `message and labels are correctly mapped in a message detail item model`() {
        // Given
        val expectedResult = MessageWithLabels(message, labels)
        // When
        val result = messageWithLabelsMapper.toUiModel(message, labels)
        // Then
        assertEquals(expectedResult, result)
    }
}
