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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.testdata.message.MessageBodyEntityTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageEntityTestData
import ch.protonmail.android.testdata.message.MessageTestData
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageWithBodyEntityMapperTest {

    private val messageWithBodyEntityMapper = MessageWithBodyEntityMapper()

    @Test
    fun `message with body relation is correctly mapped to a message with body domain model`() {
        // Given
        val messageWithBodyEntity = MessageWithBodyEntity(
            MessageEntityTestData.messageEntity,
            MessageBodyEntityTestData.messageBodyEntity,
            listOf(LabelIdSample.Inbox)
        )
        val expected = MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)

        // When
        val actual = messageWithBodyEntityMapper.toMessageWithBody(
            messageWithBodyEntity,
            listOf(MessageAttachmentSample.invoice)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `message body domain model is correctly mapped to message body entity`() {
        // Given
        val messageWithBody = MessageBodyTestData.messageBody
        val expected = MessageBodyEntityTestData.messageBodyEntity

        // When
        val actual = messageWithBodyEntityMapper.toMessageBodyEntity(messageWithBody)

        // Then
        assertEquals(expected, actual)
    }
}
