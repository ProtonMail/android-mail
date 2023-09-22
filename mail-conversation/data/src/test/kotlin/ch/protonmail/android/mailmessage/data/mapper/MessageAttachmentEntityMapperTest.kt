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

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentEntityTestData
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import org.junit.Test
import kotlin.test.assertEquals

class MessageAttachmentEntityMapperTest {

    private val messageAttachmentEntityMapper = MessageAttachmentEntityMapper()

    @Test
    fun `attachment domain model is correctly mapped to attachment entity`() {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val attachment = MessageAttachmentSample.invoice
        val expected = MessageAttachmentEntityTestData.invoice()

        // When
        val actual = messageAttachmentEntityMapper.toMessageAttachmentEntity(userId, messageId, attachment)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `attachment entity is correctly mapped to attachment domain model`() {
        // Given
        val attachmentEntity = MessageAttachmentEntityTestData.invoice()
        val expected = MessageAttachmentSample.invoice

        // When
        val actual = messageAttachmentEntityMapper.toMessageAttachment(attachmentEntity)

        // Then
        assertEquals(expected, actual)
    }

}
