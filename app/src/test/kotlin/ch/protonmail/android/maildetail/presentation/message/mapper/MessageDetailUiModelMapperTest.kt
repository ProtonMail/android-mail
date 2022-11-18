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

package ch.protonmail.android.maildetail.presentation.message.mapper

import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailMetadataUiModel
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageDetailUiModelMapperTest {

    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper = mockk {
        every { toUiModel(any(), any()) } returns MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
    }

    private val mapper = MessageDetailUiModelMapper(messageDetailHeaderUiModelMapper)

    @Test
    fun `map message to message ui model`() {
        // Given
        val message = buildMessage(
            userId = userId,
            id = RAW_MESSAGE_ID,
            subject = SUBJECT
        )
        val messageWithLabels = MessageWithLabels(message, emptyList())
        // When
        val actual = mapper.toUiModel(messageWithLabels, ContactTestData.contacts)
        // Then
        val expected = MessageDetailMetadataUiModel(
            SUBJECT,
            false,
            MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `map message with starred label to starred message ui model`() {
        // Given
        val message = buildMessage(
            userId = userId,
            id = RAW_MESSAGE_ID,
            labelIds = listOf("10")
        )
        val messageWithLabels = MessageWithLabels(message, emptyList())
        // When
        val actual = mapper.toUiModel(messageWithLabels, emptyList())
        // Then
        assertTrue(actual.isStarred)
    }

    companion object {

        const val RAW_MESSAGE_ID = "rawMessageId"
        const val SUBJECT = "Here's a new email"
    }
}
