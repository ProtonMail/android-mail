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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.message.MessageAttachmentEntityTestData.createHeaderMap
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DraftUploadTrackerTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.LocalDraft
    private val sampleDraft = createTestMessageWithBody(userId, messageId)

    private val findLocalDraft = mockk<FindLocalDraft>()
    private val draftStateRepository = mockk<DraftStateRepository>()

    private val draftUploadTracker = DraftUploadTracker(findLocalDraft, draftStateRepository)

    private val uploadRequiringChanges: List<MessageWithBody> = listOf(
        sampleDraft.copy(message = sampleDraft.message.copy(subject = "New Subject")),
        sampleDraft.copy(
            message = sampleDraft.message.copy(
                sender =
                Sender("new_sender@example.com", "New Sender", false)
            )
        ),
        sampleDraft.copy(
            message = sampleDraft.message.copy(
                toList =
                listOf(Recipient("new_to@example.com", "New Recipient", false))
            )
        ),
        sampleDraft.copy(
            message = sampleDraft.message.copy(
                ccList =
                listOf(Recipient("new_cc@example.com", "New CC Recipient", false))
            )
        ),
        sampleDraft.copy(
            message = sampleDraft.message.copy(
                bccList =
                listOf(Recipient("new_bcc@example.com", "New BCC Recipient", false))
            )
        ),
        sampleDraft.copy(message = sampleDraft.message.copy(order = 2L)),
        sampleDraft.copy(message = sampleDraft.message.copy(flags = 1L)),
        sampleDraft.copy(message = sampleDraft.message.copy(labelIds = listOf(LabelId("new_label")))),
        sampleDraft.copy(message = sampleDraft.message.copy(size = 150L)),
        sampleDraft.copy(message = sampleDraft.message.copy(conversationId = ConversationId("new_conversation"))),
        sampleDraft.copy(message = sampleDraft.message.copy(expirationTime = System.currentTimeMillis() + 6_000)),
        sampleDraft.copy(message = sampleDraft.message.copy(addressId = AddressId("new_address"))),
        sampleDraft.copy(message = sampleDraft.message.copy(externalId = "new_external_id")),
        sampleDraft.copy(message = sampleDraft.message.copy(numAttachments = 1)),
        sampleDraft.copy(message = sampleDraft.message.copy(attachmentCount = AttachmentCount(1))),

        sampleDraft.copy(messageBody = sampleDraft.messageBody.copy(body = "New Body")),
        sampleDraft.copy(messageBody = sampleDraft.messageBody.copy(spamScore = "1")),
        sampleDraft.copy(messageBody = sampleDraft.messageBody.copy(header = "New Header")),
        sampleDraft.copy(messageBody = sampleDraft.messageBody.copy(attachments = listOf(createSampleAttachment()))),
        sampleDraft.copy(messageBody = sampleDraft.messageBody.copy(mimeType = MimeType.PlainText)),
        sampleDraft.copy(
            messageBody = sampleDraft.messageBody.copy(
                replyTo =
                Recipient("new_replyto@example.com", "New Reply Recipient Name", false)
            )
        )
    )

    @Test
    fun `upload is required when draft state is not synchronized`() = runTest {
        // given
        val draftState = DraftState(
            userId = userId,
            messageId = messageId,
            apiMessageId = MessageIdSample.RemoteDraft,
            state = DraftSyncState.Synchronized,
            action = DraftAction.Compose,
            sendingError = null,
            sendingStatusConfirmed = false
        )
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(draftState.right())

        // when
        val uploadRequired = draftUploadTracker.uploadRequired(userId, messageId)

        // then
        assertTrue { uploadRequired }
    }

    @Test
    fun `upload is required when there is no last updated draft available`() = runTest {
        // given
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf()
        coEvery { findLocalDraft(userId, messageId) } returns sampleDraft

        // when
        val uploadRequired = draftUploadTracker.uploadRequired(userId, messageId)

        // then
        assertTrue { uploadRequired }
    }

    @Test
    fun `uploadRequired returns true when subject is changed`() = runTest {
        // given
        val lastUploadedDraft = sampleDraft
        val localDraft = sampleDraft.copy(message = sampleDraft.message.copy(subject = "New Subject"))
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf()
        coEvery { findLocalDraft(userId, messageId) } returns localDraft
        draftUploadTracker.notifyUploadedDraft(messageId, lastUploadedDraft)

        // when
        val uploadRequired = draftUploadTracker.uploadRequired(userId, messageId)

        // then
        assertTrue { uploadRequired }
    }

    @Test
    fun `upload is not required when localDraft is equal to last uploaded draft`() = runTest {
        // given
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf()
        coEvery { findLocalDraft(userId, messageId) } returns sampleDraft
        draftUploadTracker.notifyUploadedDraft(messageId, sampleDraft)

        // when
        val uploadRequired = draftUploadTracker.uploadRequired(userId, messageId)

        // then
        assertFalse { uploadRequired }
    }

    @Test
    fun `upload is required when any upload requiring parameter changes`() = runTest {

        // given
        val lastUploadedDraft = sampleDraft
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf()
        uploadRequiringChanges.forEach { updatedLocalDraft ->

            coEvery { findLocalDraft(userId, messageId) } returns updatedLocalDraft
            draftUploadTracker.notifyUploadedDraft(messageId, lastUploadedDraft)

            // when
            val uploadRequired = draftUploadTracker.uploadRequired(userId, messageId)

            // then
            assertTrue { uploadRequired }
        }
    }

    private fun createTestMessageWithBody(userId: UserId, messageId: MessageId): MessageWithBody {
        return MessageWithBody(
            message = createTestMessage(userId, messageId),
            messageBody = createTestMessageBody(userId, messageId)
        )
    }

    private fun createTestMessage(userId: UserId, messageId: MessageId): Message {
        return Message(
            userId = userId,
            messageId = messageId,
            conversationId = ConversationIdSample.Invoices,
            time = System.currentTimeMillis(),
            size = 100,
            order = 1,
            labelIds = emptyList(),
            subject = "Test Subject",
            unread = true,
            sender = Sender("sender@example.com", "Sender Name", false),
            toList = listOf(Recipient("to@example.com", "Recipient Name", false)),
            ccList = emptyList(),
            bccList = emptyList(),
            expirationTime = 0,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            addressId = AddressId("testAddressId"),
            externalId = null,
            numAttachments = 0,
            flags = 0,
            attachmentCount = AttachmentCount(0)
        )
    }

    private fun createTestMessageBody(userId: UserId, messageId: MessageId): MessageBody {
        return MessageBody(
            messageId = messageId,
            userId = userId,
            body = "Test Body",
            spamScore = "0",
            header = "Test Header",
            attachments = emptyList(),
            mimeType = MimeType.Html,
            replyTo = Recipient("replyto@example.com", "Reply Recipient Name", false),
            replyTos = emptyList(),
            unsubscribeMethods = null
        )
    }


    private fun createSampleAttachment(): MessageAttachment {
        return MessageAttachment(
            attachmentId = AttachmentId("sample_attachment_id"),
            name = "Sample Attachment",
            size = 1024L, // 1 KB
            mimeType = "application/pdf",
            disposition = "inline",
            keyPackets = "sample_key_packets",
            signature = "sample_signature",
            encSignature = "sample_encryption_signature",
            headers = createHeaderMap(
                "Content-Type" to "application/pdf",
                "Content-Disposition" to "inline; filename=sample_attachment.pdf",
                "X-Custom-Header" to "Custom Value"
            )
        )
    }
}
