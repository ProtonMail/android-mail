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

package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetConversationMessagesAttachmentsStatusTest {

    private val userId = UserIdSample.Primary

    private val attachmentRepo = mockk<AttachmentRepository>()

    private val getConversationMessagesAttachmentsStatus = GetConversationMessagesAttachmentsStatus(attachmentRepo)

    @Test
    fun `should return only attachment metadata of requested messages when multiple messages are affected`() = runTest {
        // Given
        val attachment1 = buildMessageAttachmentMetadata(attachmentId = AttachmentId("attachment1"))
        val attachment2 = buildMessageAttachmentMetadata(attachmentId = AttachmentId("attachment2"))
        val attachmentOfDifferentMessage = buildMessageAttachmentMetadata(
            messageId = MessageIdSample.EmptyDraft,
            attachmentId = AttachmentId("attachmentOfDifferentMessage")
        )

        coEvery { attachmentRepo.getRunningAttachmentsForUser(userId) } returns listOf(
            attachment1,
            attachment2,
            attachmentOfDifferentMessage
        )

        // When
        val result = getConversationMessagesAttachmentsStatus(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertEquals(listOf(attachment1, attachment2), result)
    }

    @Test
    fun `should return empty list when no attachments are running`() = runTest {
        // Given
        coEvery { attachmentRepo.getRunningAttachmentsForUser(userId) } returns emptyList()

        // When
        val result = getConversationMessagesAttachmentsStatus(userId, listOf(MessageIdSample.Invoice))

        // Then
        assertEquals(emptyList(), result)
    }
}
