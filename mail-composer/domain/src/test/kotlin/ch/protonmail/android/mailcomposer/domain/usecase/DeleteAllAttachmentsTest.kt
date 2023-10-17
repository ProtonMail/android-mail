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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteAllAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val senderEmail = SenderEmail("senderEmail")
    private val messageId = MessageIdSample.MessageWithAttachments

    private val getLocalDraft = mockk<GetLocalDraft>()
    private val attachmentRepository = mockk<AttachmentRepository>()

    private val deleteAllAttachments = DeleteAllAttachments(getLocalDraft, attachmentRepository)

    @Test
    fun `when delete all attachment is called, then the repository is called for all attachments`() = runTest {
        // Given
        expectGetLocalDraftSucceeds()
        MessageWithBodySample.MessageWithAttachments.messageBody.attachments.forEach {
            expectAttachmentRepositoryDeleteAttachmentSucceeds(it.attachmentId)
        }

        // When
        deleteAllAttachments(userId, senderEmail, messageId)

        // Then
        MessageWithBodySample.MessageWithAttachments.messageBody.attachments.forEach {
            coVerify { attachmentRepository.deleteAttachment(userId, messageId, it.attachmentId) }
        }
    }

    @Test
    fun `when deleting of an attachment fails, the other attachment still get deleted`() = runTest {
        // Given
        expectGetLocalDraftSucceeds()
        val attachments = MessageWithBodySample.MessageWithAttachments.messageBody.attachments
        expectAttachmentRepositoryDeleteAttachmentFails(attachments.first().attachmentId)
        attachments.takeLast(2).forEach {
            expectAttachmentRepositoryDeleteAttachmentSucceeds(it.attachmentId)
        }

        // When
        deleteAllAttachments(userId, senderEmail, messageId)

        // Then
        MessageWithBodySample.MessageWithAttachments.messageBody.attachments.forEach {
            coVerify { attachmentRepository.deleteAttachment(userId, messageId, it.attachmentId) }
        }
    }

    private fun expectGetLocalDraftSucceeds() {
        coEvery {
            getLocalDraft(userId, messageId, senderEmail)
        } returns MessageWithBodySample.MessageWithAttachments.right()
    }

    private fun expectAttachmentRepositoryDeleteAttachmentSucceeds(attachmentId: AttachmentId) {
        coEvery {
            attachmentRepository.deleteAttachment(userId, messageId, attachmentId)
        } returns Unit.right()
    }

    private fun expectAttachmentRepositoryDeleteAttachmentFails(attachmentId: AttachmentId) {
        coEvery {
            attachmentRepository.deleteAttachment(userId, messageId, attachmentId)
        } returns DataError.Local.FailedToDeleteFile.left()
    }

}
