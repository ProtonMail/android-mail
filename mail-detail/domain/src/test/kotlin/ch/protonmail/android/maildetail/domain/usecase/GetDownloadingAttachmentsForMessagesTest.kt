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

import android.net.Uri
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetDownloadingAttachmentsForMessagesTest {

    private val userId = UserIdSample.Primary

    private val attachmentRepo = mockk<AttachmentRepository>()

    private val getDownloadingAttachmentsForMessages = GetDownloadingAttachmentsForMessages(attachmentRepo)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `should return only attachment metadata of requested messages when multiple messages are affected`() = runTest {
        // Given
        val attachment1 = buildMessageAttachmentMetadata(
            attachmentId = AttachmentId("attachment1"),
            messageId = MessageIdSample.Invoice
        )
        val attachment2 = buildMessageAttachmentMetadata(
            attachmentId = AttachmentId("attachment2"),
            messageId = MessageIdSample.Invoice
        )

        val messageIds = listOf(MessageIdSample.Invoice)
        coEvery {
            attachmentRepo.getDownloadingAttachmentsForMessages(userId, messageIds)
        } returns listOf(
            attachment1,
            attachment2
        )

        // When
        val result = getDownloadingAttachmentsForMessages(userId, messageIds)

        // Then
        assertEquals(listOf(attachment1, attachment2), result)
    }

    @Test
    fun `should return empty list when no attachments are running`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.Invoice)
        coEvery { attachmentRepo.getDownloadingAttachmentsForMessages(userId, messageIds) } returns emptyList()

        // When
        val result = getDownloadingAttachmentsForMessages(userId, messageIds)

        // Then
        assertEquals(emptyList(), result)
    }
}
