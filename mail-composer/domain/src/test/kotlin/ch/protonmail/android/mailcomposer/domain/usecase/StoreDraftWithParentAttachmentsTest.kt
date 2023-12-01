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
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreDraftWithParentAttachmentsTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val userId = UserIdSample.Primary
    private val draftMessageId = MessageIdSample.build()
    private val senderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)

    private val attachmentRepository = mockk<AttachmentRepository>()
    private val deleteAllAttachments = mockk<DeleteAllAttachments>()
    private val saveDraftMock = mockk<SaveDraft>()
    private val getLocalDraftMock = mockk<GetLocalDraft>()
    private val storeParentAttachmentStates = mockk<StoreParentAttachmentStates>()
    private val fakeTransactor = FakeTransactor()

    private val storeDraftWithParentAttachments = StoreDraftWithParentAttachments(
        attachmentRepository = attachmentRepository,
        deleteAllAttachments = deleteAllAttachments,
        getLocalDraft = getLocalDraftMock,
        saveDraft = saveDraftMock,
        storeParentAttachmentStates = storeParentAttachmentStates,
        transactor = fakeTransactor
    )

    @Test
    fun `store draft with all attachments when action is forward`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.MessageWithAttachments,
            DecryptedMessageBodyTestData.MessageWithAttachments
        )
        val expectedAction = DraftAction.Forward(expectedParentMessage.messageWithBody.message.messageId)
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedSavedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(
                attachments = expectedParentMessage.decryptedMessageBody.attachments
            )
        )
        givenSaveDraftSucceeds(expectedSavedDraft, userId)
        givenStoreParentAttachmentsSucceeds(
            userId = userId,
            messageId = expectedSavedDraft.message.messageId,
            attachments = expectedSavedDraft.messageBody.attachments.map { it.attachmentId },
            syncState = AttachmentSyncState.External
        )

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerifyOrder {
            saveDraftMock(expectedSavedDraft, userId)
            storeParentAttachmentStates(
                userId = userId,
                messageId = expectedSavedDraft.message.messageId,
                attachmentIds = expectedSavedDraft.messageBody.attachments.map { it.attachmentId },
                syncState = AttachmentSyncState.External
            )
        }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `store draft with inline attachments only when action is reply or reply all`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.MessageWithAttachments,
            DecryptedMessageBodyTestData.MessageWithAttachments
        )
        val expectedAction = DraftAction.Reply(expectedParentMessage.messageWithBody.message.messageId)
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments.filter {
            it.disposition == "inline"
        }
        val expectedAttachmentIds = expectedAttachments.map { it.attachmentId }
        val expectedSavedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
        )
        givenSaveDraftSucceeds(expectedSavedDraft, userId)
        givenStoreParentAttachmentsSucceeds(
            userId = userId,
            messageId = expectedSavedDraft.message.messageId,
            attachments = expectedAttachmentIds,
            syncState = AttachmentSyncState.External
        )

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerifyOrder {
            saveDraftMock(expectedSavedDraft, userId)
            storeParentAttachmentStates(
                userId = userId,
                messageId = expectedSavedDraft.message.messageId,
                attachmentIds = expectedAttachmentIds,
                syncState = AttachmentSyncState.External
            )
        }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `returns no attachment to be stored when parent has no attachments to store for the given action`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.Invoice,
            DecryptedMessageBodyTestData.buildDecryptedMessageBody(messageId = MessageIdSample.Invoice)
        )
        val expectedAction = DraftAction.ReplyAll(expectedParentMessage.messageWithBody.message.messageId)

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerify { saveDraftMock wasNot Called }
        assertEquals(StoreDraftWithParentAttachments.Error.NoAttachmentsToBeStored.left(), result)
    }

    @Test
    fun `returns action with no parent error when called with Compose action`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.Invoice,
            DecryptedMessageBodyTestData.buildDecryptedMessageBody(messageId = MessageIdSample.Invoice)
        )
        val expectedAction = DraftAction.Compose

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerify { saveDraftMock wasNot Called }
        assertEquals(StoreDraftWithParentAttachments.Error.ActionWithNoParent.left(), result)
    }

    @Test
    fun `returns draft data error when failing to store the draft`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.MessageWithAttachments,
            DecryptedMessageBodyTestData.MessageWithAttachments
        )
        val expectedAction = DraftAction.ReplyAll(expectedParentMessage.messageWithBody.message.messageId)
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments.filter {
            it.disposition == "inline"
        }
        val expectedSavedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
        )
        givenSaveDraftFails(expectedSavedDraft, userId)

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, userId) }
        assertEquals(StoreDraftWithParentAttachments.Error.DraftDataError.left(), result)
    }

    @Test
    fun `removes signature and encrypted signature from parent attachments before storing them`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.MessageWithSignedAttachments,
            DecryptedMessageBodyTestData.MessageWithSignedAttachments
        )
        val expectedAction = DraftAction.Forward(expectedParentMessage.messageWithBody.message.messageId)
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments.map {
            it.copy(signature = null, encSignature = null)
        }
        val expectedSavedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
        )
        givenSaveDraftSucceeds(expectedSavedDraft, userId)
        givenStoreParentAttachmentsSucceeds(
            userId = userId,
            messageId = expectedSavedDraft.message.messageId,
            attachments = expectedAttachments.map { it.attachmentId },
            syncState = AttachmentSyncState.External
        )

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerifySequence {
            saveDraftMock(expectedSavedDraft, userId)
            storeParentAttachmentStates(
                userId = userId,
                messageId = expectedSavedDraft.message.messageId,
                attachmentIds = expectedAttachments.map { it.attachmentId },
                syncState = AttachmentSyncState.External
            )
        }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should copy embedded attachments from parent pgp mime message and set their status to local when replying`() =
        runTest {
            // Given
            val expectedParentMessage = MessageWithDecryptedBody(
                MessageWithBodySample.PgpMimeMessage,
                DecryptedMessageBodyTestData.PgpMimeMessage
            )
            val expectedAction = DraftAction.Reply(expectedParentMessage.messageWithBody.message.messageId)
            val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
                MessageWithBodySample.EmptyDraft
            }
            val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments.filter {
                it.disposition == "inline"
            }
            val expectedSavedDraft = draftWithBody.copy(
                messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
            )
            givenSaveDraftSucceeds(expectedSavedDraft, userId)
            givenCopyAttachmentsFromParentMessageSucceeds(
                userId = userId,
                sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
                targetMessageId = draftMessageId,
                attachmentIds = expectedAttachments.map { it.attachmentId }
            )
            givenStoreParentAttachmentsSucceeds(
                userId = userId,
                messageId = expectedSavedDraft.message.messageId,
                attachments = expectedAttachments.map { it.attachmentId },
                syncState = AttachmentSyncState.Local
            )

            // When
            val result = storeDraftWithParentAttachments(
                userId,
                draftMessageId,
                expectedParentMessage,
                senderEmail,
                expectedAction
            )

            // Then
            coVerifySequence {
                saveDraftMock(expectedSavedDraft, userId)
                attachmentRepository.copyMimeAttachmentsToMessage(
                    userId = userId,
                    sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
                    targetMessageId = draftMessageId,
                    attachmentIds = expectedAttachments.map { it.attachmentId }
                )
                storeParentAttachmentStates(
                    userId = userId,
                    messageId = expectedSavedDraft.message.messageId,
                    attachmentIds = expectedAttachments.map { it.attachmentId },
                    syncState = AttachmentSyncState.Local
                )
            }
            assertEquals(Unit.right(), result)
        }

    @Test
    fun `should copy all attachments from parent pgp mime message and set their status to local when forwarding`() =
        runTest {
            // Given
            val expectedParentMessage = MessageWithDecryptedBody(
                MessageWithBodySample.PgpMimeMessage,
                DecryptedMessageBodyTestData.PgpMimeMessage
            )
            val expectedAction = DraftAction.Forward(expectedParentMessage.messageWithBody.message.messageId)
            val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
                MessageWithBodySample.EmptyDraft
            }
            val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments
            val expectedSavedDraft = draftWithBody.copy(
                messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
            )
            givenSaveDraftSucceeds(expectedSavedDraft, userId)
            givenCopyAttachmentsFromParentMessageSucceeds(
                userId = userId,
                sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
                targetMessageId = draftMessageId,
                attachmentIds = expectedAttachments.map { it.attachmentId }
            )
            givenStoreParentAttachmentsSucceeds(
                userId = userId,
                messageId = expectedSavedDraft.message.messageId,
                attachments = expectedAttachments.map { it.attachmentId },
                syncState = AttachmentSyncState.Local
            )

            // When
            val result = storeDraftWithParentAttachments(
                userId,
                draftMessageId,
                expectedParentMessage,
                senderEmail,
                expectedAction
            )

            // Then
            coVerifySequence {
                saveDraftMock(expectedSavedDraft, userId)
                attachmentRepository.copyMimeAttachmentsToMessage(
                    userId = userId,
                    sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
                    targetMessageId = draftMessageId,
                    attachmentIds = expectedAttachments.map { it.attachmentId }
                )
                storeParentAttachmentStates(
                    userId = userId,
                    messageId = expectedSavedDraft.message.messageId,
                    attachmentIds = expectedAttachments.map { it.attachmentId },
                    syncState = AttachmentSyncState.Local
                )
            }
            assertEquals(Unit.right(), result)
        }

    @Test
    fun `should delete all parent attachments when copying them from parent pgp mime message fails`() = runTest {
        // Given
        val expectedParentMessage = MessageWithDecryptedBody(
            MessageWithBodySample.PgpMimeMessage,
            DecryptedMessageBodyTestData.PgpMimeMessage
        )
        val expectedAction = DraftAction.Forward(expectedParentMessage.messageWithBody.message.messageId)
        val draftWithBody = expectedGetLocalDraft(userId, draftMessageId, senderEmail) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedAttachments = expectedParentMessage.decryptedMessageBody.attachments
        val expectedSavedDraft = draftWithBody.copy(
            messageBody = draftWithBody.messageBody.copy(attachments = expectedAttachments)
        )
        givenSaveDraftSucceeds(expectedSavedDraft, userId)
        givenCopyAttachmentsFromParentMessageFails(
            userId = userId,
            sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
            targetMessageId = draftMessageId,
            attachmentIds = expectedAttachments.map { it.attachmentId }
        )
        givenDeleteAllAttachmentsSucceeds(userId, senderEmail, draftMessageId)

        // When
        val result = storeDraftWithParentAttachments(
            userId,
            draftMessageId,
            expectedParentMessage,
            senderEmail,
            expectedAction
        )

        // Then
        coVerifySequence {
            saveDraftMock(expectedSavedDraft, userId)
            attachmentRepository.copyMimeAttachmentsToMessage(
                userId = userId,
                sourceMessageId = expectedParentMessage.messageWithBody.message.messageId,
                targetMessageId = draftMessageId,
                attachmentIds = expectedAttachments.map { it.attachmentId }
            )
            deleteAllAttachments(userId, senderEmail, draftMessageId)
        }
        assertEquals(StoreDraftWithParentAttachments.Error.DraftAttachmentError.left(), result)
    }

    private fun expectedGetLocalDraft(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        localDraft: () -> MessageWithBody
    ): MessageWithBody = localDraft().also {
        coEvery { getLocalDraftMock.invoke(userId, messageId, senderEmail) } returns it.right()
    }

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
    }

    private fun givenCopyAttachmentsFromParentMessageSucceeds(
        userId: UserId,
        sourceMessageId: MessageId,
        targetMessageId: MessageId,
        attachmentIds: List<AttachmentId>
    ) {
        coEvery {
            attachmentRepository.copyMimeAttachmentsToMessage(userId, sourceMessageId, targetMessageId, attachmentIds)
        } returns Unit.right()
    }

    private fun givenCopyAttachmentsFromParentMessageFails(
        userId: UserId,
        sourceMessageId: MessageId,
        targetMessageId: MessageId,
        attachmentIds: List<AttachmentId>
    ) {
        coEvery {
            attachmentRepository.copyMimeAttachmentsToMessage(userId, sourceMessageId, targetMessageId, attachmentIds)
        } returns DataError.Local.FailedToStoreFile.left()
    }

    private fun givenDeleteAllAttachmentsSucceeds(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId
    ) {
        coEvery { deleteAllAttachments(userId, senderEmail, messageId) } just Runs
    }

    private fun givenStoreParentAttachmentsSucceeds(
        userId: UserId,
        messageId: MessageId,
        attachments: List<AttachmentId>,
        syncState: AttachmentSyncState
    ) {
        coEvery {
            storeParentAttachmentStates(userId, messageId, attachments, syncState)
        } returns Unit.right()
    }
}
