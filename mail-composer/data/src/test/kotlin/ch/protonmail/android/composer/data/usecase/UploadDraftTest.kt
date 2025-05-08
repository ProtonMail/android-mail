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

package ch.protonmail.android.composer.data.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailcomposer.domain.usecase.CreateOrUpdateParentAttachmentStates
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploadTracker
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.IsDraftKnownToApi
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UploadDraftTest {

    @get:Rule
    val loggingRule = LoggingTestRule()

    private val userId = UserIdSample.Primary

    private val messageRepository = mockk<MessageRepository>()
    private val findLocalDraft = mockk<FindLocalDraft>()
    private val draftRemoteDataSource = mockk<DraftRemoteDataSource>()
    private val draftStateRepository = mockk<DraftStateRepository>()
    private val isDraftKnownToApi = mockk<IsDraftKnownToApi>()
    private val fakeTransactor = FakeTransactor()
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val updatedAttachmentStates = mockk<CreateOrUpdateParentAttachmentStates>()
    private val draftUploadTracker = mockk<DraftUploadTracker>()

    private val draftRepository = UploadDraft(
        fakeTransactor,
        messageRepository,
        findLocalDraft,
        draftStateRepository,
        draftRemoteDataSource,
        isDraftKnownToApi,
        attachmentRepository,
        updatedAttachmentStates,
        draftUploadTracker
    )

    @Test
    fun `returns success when remote data source succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedResponse = MessageWithBodySample.EmptyDraft
        val expectedDraftState = DraftStateSample.NewDraftState
        val apiAssignedMessageId = expectedResponse.message.messageId
        val apiAssignedConversationId = expectedResponse.message.conversationId
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedAction, expectedResponse)
        expectStoreSyncedStateSuccess(userId, messageId, apiAssignedMessageId)
        expectMessageUpdateSuccess(userId, messageId, apiAssignedMessageId, apiAssignedConversationId)
        expectIsDraftKnownToApi(expectedDraftState, false)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 0) { draftRemoteDataSource.update(any(), any()) }
    }

    @Test
    fun `update message and draft state with api message id when create call succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val apiAssignedMessageId = MessageIdSample.RemoteDraft
        val expectedDraft = MessageWithBodySample.NewDraftWithSubject
        val expectedAction = DraftAction.Compose
        val expectedResponse = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.NewDraftState
        val apiAssignedConversationId = expectedResponse.message.conversationId
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedAction, expectedResponse)
        expectStoreSyncedStateSuccess(userId, messageId, apiAssignedMessageId)
        expectMessageUpdateSuccess(userId, messageId, apiAssignedMessageId, apiAssignedConversationId)
        expectIsDraftKnownToApi(expectedDraftState, false)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify {
            messageRepository.updateDraftRemoteIds(
                userId,
                messageId,
                apiAssignedMessageId,
                apiAssignedConversationId
            )
        }
        coVerify { draftStateRepository.updateApiMessageIdAndSetSyncedState(userId, messageId, apiAssignedMessageId) }
    }

    @Test
    fun `update draft state to synced when update call succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.RemoteDraft
        val expectedDraft = MessageWithBodySample.RemoteDraft
        val expectedResponse = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceUpdateSuccess(userId, expectedDraft, expectedResponse)
        expectStoreSyncedStateSuccess(userId, messageId, messageId)
        expectIsDraftKnownToApi(expectedDraftState, true)
        expectDraftUploadTrackerNotified(messageId, expectedDraft)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { draftStateRepository.updateApiMessageIdAndSetSyncedState(userId, messageId, messageId) }
    }

    @Test
    fun `returns local failure when reading the message from DB fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expectedError = DataError.Local.NoDataCached
        val expectedDraftState = DraftStateSample.NewDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageFails(userId, messageId)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
        loggingRule.assertDebugLogged("Sync draft failure $messageId: No message found")
    }

    @Test
    fun `returns local failure when reading the draft state from DB fails`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedError = DataError.Local.NoDataCached
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectGetDraftStateFails(userId, messageId, expectedError)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `returns remote failure when remote data source fails`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val expectedDraft = MessageWithBodySample.Invoice
        val expectedAction = DraftAction.Compose
        val expectedError = DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft, null)
        val expectedDraftState = DraftStateSample.NewDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceFailure(userId, expectedDraft, expectedAction, expectedError)
        expectIsDraftKnownToApi(expectedDraftState, false)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        loggingRule.assertWarningLogged("Sync draft failure $messageId: Create API call error $expectedError")
        assertEquals(expectedError.left(), actual)
    }

    @Test
    fun `does not log failure on sentry when remote data source fails with create draft request not performed`() =
        runTest {
            // Given
            val messageId = MessageIdSample.LocalDraft
            val expectedDraft = MessageWithBodySample.Invoice
            val expectedAction = DraftAction.Compose
            val expectedError = DataError.Remote.CreateDraftRequestNotPerformed
            val expectedDraftState = DraftStateSample.NewDraftState
            expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
            expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
            expectRemoteDataSourceFailure(userId, expectedDraft, expectedAction, expectedError)
            expectIsDraftKnownToApi(expectedDraftState, false)

            // When
            val actual = draftRepository(userId, messageId)

            // Then
            assertEquals(expectedError.left(), actual)
            loggingRule.assertNoWarningLogs()
        }

    @Test
    fun `sync performs update on remote data source when draft is already known to the API`() = runTest {
        // Given
        val messageId = MessageIdSample.RemoteDraft
        val expectedDraft = MessageWithBodySample.RemoteDraft
        val expectedResponse = MessageWithBodySample.RemoteDraft
        val expectedDraftState = DraftStateSample.RemoteDraftState
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceUpdateSuccess(userId, expectedDraft, expectedResponse)
        expectStoreSyncedStateSuccess(userId, messageId, messageId)
        expectIsDraftKnownToApi(expectedDraftState, true)
        expectDraftUploadTrackerNotified(messageId, expectedDraft)

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify(exactly = 0) { draftRemoteDataSource.create(any(), any(), any()) }
        coVerify { draftUploadTracker.notifyUploadedDraft(messageId, expectedDraft) }
    }

    @Test
    fun `update local attachment id and key packets when create call succeeds`() = runTest {
        // Given
        val messageId = MessageIdSample.LocalDraft
        val expectedDraft = MessageWithBodySample.MessageWithInvoiceAttachment
        val expectedLocalAttachment = expectedDraft.messageBody.attachments.first()
        val expectedUpdatedAttachment = expectedLocalAttachment.copy(
            attachmentId = AttachmentId("Api-defined-id"),
            keyPackets = "keyPackets"
        )
        val expectedResponse = expectedDraft.copy(
            messageBody = expectedDraft.messageBody.copy(attachments = listOf(expectedUpdatedAttachment))
        )
        val apiAssignedMessageId = expectedResponse.message.messageId
        val apiAssignedConversationId = expectedResponse.message.conversationId
        val expectedDraftState = DraftStateSample.LocalDraftWithForwardAction
        expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
        expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
        expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedDraftState.action, expectedResponse)
        expectStoreSyncedStateSuccess(userId, messageId, messageId)
        expectIsDraftKnownToApi(expectedDraftState, false)
        expectStoreSyncedStateSuccess(userId, messageId, apiAssignedMessageId)
        expectMessageUpdateSuccess(userId, messageId, apiAssignedMessageId, apiAssignedConversationId)
        expectAttachmentUpdateSuccess(
            userId, apiAssignedMessageId, expectedLocalAttachment.attachmentId, expectedUpdatedAttachment
        )
        expectStoreParentAttachmentSucceeds(
            userId, apiAssignedMessageId, listOf(expectedUpdatedAttachment.attachmentId)
        )

        // When
        val actual = draftRepository(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerifySequence {
            attachmentRepository.updateMessageAttachment(
                userId,
                apiAssignedMessageId,
                expectedLocalAttachment.attachmentId,
                expectedUpdatedAttachment
            )
            updatedAttachmentStates(
                userId,
                apiAssignedMessageId,
                listOf(expectedUpdatedAttachment.attachmentId)
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `Should match local and remote attachments by keyPackets, name and mimeType when updating local ids, despite different attachment sizes`() =
        runTest {
            // Given
            val messageId = MessageIdSample.LocalDraft
            val expectedLocalAttachments = listOf(
                MessageAttachmentSample.embeddedImageAttachment.copy(
                    keyPackets = "keyPackets"
                ),
                MessageAttachmentSample.embeddedOctetStreamAttachment.copy(
                    keyPackets = "keyPackets"
                ),
                MessageAttachmentSample.image.copy(
                    keyPackets = "keyPackets"
                )
            )
            val expectedDraft = MessageWithBodySample.MessageWithInvoiceAttachment.copy(
                messageBody = MessageWithBodySample.MessageWithInvoiceAttachment.messageBody.copy(
                    attachments = expectedLocalAttachments
                )
            )

            val expectedUpdatedAttachments = listOf(
                MessageAttachmentSample.embeddedImageAttachment.copy(
                    attachmentId = AttachmentId("Api-defined-id-1"),
                    keyPackets = "keyPackets",
                    size = 666
                ),
                MessageAttachmentSample.embeddedOctetStreamAttachment.copy(
                    attachmentId = AttachmentId("Api-defined-id-2"),
                    keyPackets = "keyPackets",
                    size = 666
                ),
                MessageAttachmentSample.image.copy(
                    attachmentId = AttachmentId("Api-defined-id-3"),
                    keyPackets = "keyPackets",
                    size = 666
                )
            )

            val expectedResponse = expectedDraft.copy(
                messageBody = expectedDraft.messageBody.copy(attachments = expectedUpdatedAttachments)
            )
            val apiAssignedMessageId = expectedResponse.message.messageId
            val apiAssignedConversationId = expectedResponse.message.conversationId
            val expectedDraftState = DraftStateSample.LocalDraftWithForwardAction
            expectGetDraftStateSucceeds(userId, messageId, expectedDraftState)
            expectGetLocalMessageSucceeds(userId, messageId, expectedDraft)
            expectRemoteDataSourceCreateSuccess(userId, expectedDraft, expectedDraftState.action, expectedResponse)
            expectStoreSyncedStateSuccess(userId, messageId, messageId)
            expectIsDraftKnownToApi(expectedDraftState, false)
            expectStoreSyncedStateSuccess(userId, messageId, apiAssignedMessageId)
            expectMessageUpdateSuccess(userId, messageId, apiAssignedMessageId, apiAssignedConversationId)
            for (i in expectedLocalAttachments.indices) {
                expectAttachmentUpdateSuccess(
                    userId,
                    apiAssignedMessageId,
                    expectedLocalAttachments[i].attachmentId,
                    expectedUpdatedAttachments[i]
                )
            }
            expectStoreParentAttachmentSucceeds(
                userId, apiAssignedMessageId, expectedUpdatedAttachments.map { it.attachmentId }
            )

            // When
            val actual = draftRepository(userId, messageId)

            // Then
            assertEquals(Unit.right(), actual)
            for (i in expectedLocalAttachments.indices) {
                coVerify {
                    attachmentRepository.updateMessageAttachment(
                        userId,
                        apiAssignedMessageId,
                        expectedLocalAttachments[i].attachmentId,
                        expectedUpdatedAttachments[i]
                    )
                }
            }

            coVerify {
                updatedAttachmentStates(
                    userId,
                    apiAssignedMessageId,
                    expectedUpdatedAttachments.map { it.attachmentId }
                )
            }
        }

    private fun expectAttachmentUpdateSuccess(
        userId: UserId,
        messageId: MessageId,
        localAttachmentId: AttachmentId,
        updatedAttachment: MessageAttachment
    ) {
        coEvery {
            attachmentRepository.updateMessageAttachment(userId, messageId, localAttachmentId, updatedAttachment)
        } returns Unit.right()
    }

    private fun expectIsDraftKnownToApi(draftSTate: DraftState, isKnown: Boolean) {
        coEvery { isDraftKnownToApi(draftSTate) } returns isKnown
    }

    private fun expectMessageUpdateSuccess(
        userId: UserId,
        messageId: MessageId,
        remoteMessageId: MessageId,
        remoteConversationId: ConversationId
    ) {
        coEvery {
            messageRepository.updateDraftRemoteIds(userId, messageId, remoteMessageId, remoteConversationId)
        } returns Unit
    }

    private fun expectStoreSyncedStateSuccess(
        userId: UserId,
        messageId: MessageId,
        apiAssignedMessageId: MessageId
    ) {
        coEvery {
            draftStateRepository.updateApiMessageIdAndSetSyncedState(userId, messageId, apiAssignedMessageId)
        } returns Unit.right()
    }

    private fun expectRemoteDataSourceCreateSuccess(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        response: MessageWithBody
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns response.right()
    }

    private fun expectRemoteDataSourceUpdateSuccess(
        userId: UserId,
        messageWithBody: MessageWithBody,
        response: MessageWithBody
    ) {
        coEvery { draftRemoteDataSource.update(userId, messageWithBody) } returns response.right()
    }


    private fun expectRemoteDataSourceFailure(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction,
        error: DataError.Remote
    ) {
        coEvery { draftRemoteDataSource.create(userId, messageWithBody, action) } returns error.left()
    }

    private fun expectGetLocalMessageSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedMessage: MessageWithBody
    ) {
        coEvery { findLocalDraft(userId, messageId) } returns expectedMessage
    }

    private fun expectGetLocalMessageFails(userId: UserId, messageId: MessageId) {
        coEvery { findLocalDraft(userId, messageId) } returns null
    }

    private fun expectGetDraftStateSucceeds(
        userId: UserId,
        messageId: MessageId,
        expectedState: DraftState
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(expectedState.right())
    }

    private fun expectGetDraftStateFails(
        userId: UserId,
        messageId: MessageId,
        error: DataError.Local
    ) {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(error.left())
    }

    private fun expectStoreParentAttachmentSucceeds(
        userId: UserId,
        messageId: MessageId,
        attachmentIds: List<AttachmentId>
    ) {
        coJustRun { updatedAttachmentStates(userId, messageId, attachmentIds) }
    }

    private fun expectDraftUploadTrackerNotified(messageId: MessageId, expectedMessage: MessageWithBody) {
        coJustRun { draftUploadTracker.notifyUploadedDraft(messageId, expectedMessage) }
    }
}
