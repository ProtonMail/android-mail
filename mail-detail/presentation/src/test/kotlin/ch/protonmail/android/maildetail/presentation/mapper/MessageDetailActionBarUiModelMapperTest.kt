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

import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.testdata.message.MessageTestData
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDetailActionBarUiModelMapperTest {

    private val messageDetailActionBarUiModelMapper = MessageDetailActionBarUiModelMapper()

    @Test
    fun `when label id list does not contain starred, star icon should not be shown`() {
        // Given
        val subject = "Subject"
        val labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.AllMail.labelId.id)
        val message = MessageTestData.buildMessage(id = "messageId", subject = subject, labelIds = labelIds)
        val expectedResult = MessageDetailActionBarUiModel(subject, isStarred = false)

        // When
        val result = messageDetailActionBarUiModelMapper.toUiModel(message)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when label id list contains starred, star icon should be shown`() {
        // Given
        val subject = "Subject"
        val labelIds = listOf(SystemLabelId.Inbox.labelId.id, SystemLabelId.Starred.labelId.id)
        val message = MessageTestData.buildMessage(id = "messageId", subject = subject, labelIds = labelIds)
        val expectedResult = MessageDetailActionBarUiModel(subject, isStarred = true)

        // When
        val result = messageDetailActionBarUiModelMapper.toUiModel(message)

        // Then
        assertEquals(expectedResult, result)
    }
}
