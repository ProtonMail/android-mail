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

import android.net.Uri
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.usecase.ProvideNewAttachmentId
import ch.protonmail.android.test.utils.FakeTransactor
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val senderAddress = UserAddressSample.build()
    private val senderEmail = SenderEmail(senderAddress.email)
    private val localMessageId = MessageId("localMessageId")
    private val localAttachmentId = AttachmentId("localAttachmentId")

    private val uri = mockk<Uri>()
    private val messageRepository = mockk<MessageRepository>()
    private val attachmentRepository = mockk<AttachmentRepository>()
    private val attachmentStateRepository = mockk<AttachmentStateRepository>()
    private val getLocalDraft = mockk<GetLocalDraft>()
    private val saveDraft = mockk<SaveDraft>()
    private val provideNewAttachmentId = mockk<ProvideNewAttachmentId> {
        every { this@mockk.invoke() } returns localAttachmentId
    }
    private val transactor = FakeTransactor()

    private val storeAttachments = StoreAttachments(
        messageRepository,
        attachmentRepository,
        attachmentStateRepository,
        getLocalDraft,
        saveDraft,
        provideNewAttachmentId,
        transactor
    )

    @Test
    fun `should save draft and attachment with id retrieved from local draft`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        val expectedMessageBody = MessageWithBodySample.Invoice
        expectedLocalDraft(expectedMessageId, expectedMessageBody)
        expectedLocalMessageBody(expectedMessageId, null)
        expectedDraftSaving(expectedMessageBody, true)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectAttachmentStateSavingSuccess(expectedMessageId)
        expectGetFileSizeFromUriSuccess()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { saveDraft(expectedMessageBody, userId) }
        coVerify { attachmentRepository.saveAttachment(userId, expectedMessageId, localAttachmentId, uri) }
    }

    @Test
    fun `should save attachment with id retrieved from existing local draft`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        val expectedMessageBody = MessageWithBodySample.Invoice
        expectedLocalDraft(expectedMessageId, expectedMessageBody)
        expectedLocalMessageBody(expectedMessageId, expectedMessageBody)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectAttachmentStateSavingSuccess(expectedMessageId)
        expectGetFileSizeFromUriSuccess()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { saveDraft wasNot Called }
        coVerify { attachmentRepository.saveAttachment(userId, expectedMessageId, localAttachmentId, uri) }
    }

    @Test
    fun `should return failed receiving draft when storing draft fails`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        val expectedMessageBody = MessageWithBodySample.Invoice
        val expectedError = StoreDraftWithAttachmentError.FailedReceivingDraft.left()

        expectedLocalDraft(expectedMessageId, expectedMessageBody)
        expectedLocalMessageBody(expectedMessageId, null)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectedDraftSaving(expectedMessageBody, false)

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(expectedError, actual)
        coVerify { saveDraft(expectedMessageBody, userId) }
        coVerify { attachmentRepository wasNot Called }
        coVerify { attachmentStateRepository wasNot Called }
    }

    @Test
    fun `should return failed receiving draft when get local draft fails`() = runTest {
        // Given
        expectedLocalDraftError()
        val expectedError = StoreDraftWithAttachmentError.FailedReceivingDraft.left()

        // When
        val actual = storeAttachments(userId, localMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(expectedError, actual)
        coVerify { messageRepository wasNot Called }
        coVerify { attachmentRepository wasNot Called }
        coVerify { attachmentStateRepository wasNot Called }
    }

    @Test
    fun `should return failed to store attachment when storing returns error`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        expectedLocalDraft(expectedMessageId, MessageWithBodySample.Invoice)
        expectedLocalMessageBody(expectedMessageId, MessageWithBodySample.Invoice)
        expectAttachmentSavingFailed(expectedMessageId, uri)
        expectGetFileSizeFromUriSuccess()

        val expectedError = StoreDraftWithAttachmentError.FailedToStoreAttachments.left()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `should return failed to store attachment when one URI withing a list failed to get stored`() = runTest {
        // Given
        val uri2 = mockk<Uri>()
        val expectedMessageId = MessageIdSample.Invoice
        expectedLocalDraft(expectedMessageId, MessageWithBodySample.Invoice)
        expectedLocalMessageBody(expectedMessageId, MessageWithBodySample.Invoice)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectAttachmentSavingFailed(expectedMessageId, uri2)
        expectAttachmentStateSavingSuccess(expectedMessageId)
        expectGetFileSizeFromUriSuccess()
        expectGetFileSizeFromUriSuccess(uri2)

        val expectedError = StoreDraftWithAttachmentError.FailedToStoreAttachments.left()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri, uri2))

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `should return file size exceeds limit when file exceeds the limit`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        val expectedMessageBody = MessageWithBodySample.Invoice
        expectedLocalDraft(expectedMessageId, expectedMessageBody)
        expectedLocalMessageBody(expectedMessageId, null)
        expectedDraftSaving(expectedMessageBody, true)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectAttachmentStateSavingSuccess(expectedMessageId)
        expectGetFileSizeExceedsLimit()

        val expectedError = StoreDraftWithAttachmentError.FileSizeExceedsLimit.left()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `should return attachment file missing when file size cannot be retrieved`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.Invoice
        val expectedMessageBody = MessageWithBodySample.Invoice
        expectedLocalDraft(expectedMessageId, expectedMessageBody)
        expectedLocalMessageBody(expectedMessageId, null)
        expectedDraftSaving(expectedMessageBody, true)
        expectAttachmentSavingSuccessful(expectedMessageId)
        expectAttachmentStateSavingSuccess(expectedMessageId)
        expectGetFileSizeFromUriFailed()

        val expectedError = StoreDraftWithAttachmentError.AttachmentFileMissing.left()

        // When
        val actual = storeAttachments(userId, expectedMessageId, senderEmail, listOf(uri))

        // Then
        assertEquals(expectedError, actual)
    }


    private fun expectedLocalDraft(expectedMessageId: MessageId, expectedMessageBody: MessageWithBody) {
        coEvery { getLocalDraft(userId, expectedMessageId, senderEmail) } returns expectedMessageBody.right()
    }

    private fun expectedLocalDraftError() {
        coEvery {
            getLocalDraft(userId, localMessageId, senderEmail)
        } returns GetLocalDraft.Error.ResolveUserAddressError.left()
    }

    private fun expectedLocalMessageBody(messageId: MessageId, expectedMessageBody: MessageWithBody?) {
        coEvery { messageRepository.getLocalMessageWithBody(userId, messageId) } returns expectedMessageBody
    }

    private fun expectedDraftSaving(expectedMessageBody: MessageWithBody, successful: Boolean) {
        coEvery { saveDraft(expectedMessageBody, userId) } returns successful
    }

    private fun expectAttachmentSavingSuccessful(expectedMessageId: MessageId) {
        coEvery {
            attachmentRepository.saveAttachment(userId, expectedMessageId, localAttachmentId, uri)
        } returns Unit.right()
    }

    private fun expectAttachmentSavingFailed(expectedMessageId: MessageId, uri: Uri) {
        coEvery {
            attachmentRepository.saveAttachment(userId, expectedMessageId, localAttachmentId, uri)
        } returns DataError.Local.FailedToStoreFile.left()
    }

    private fun expectAttachmentStateSavingSuccess(messageId: MessageId) {
        coEvery {
            attachmentStateRepository.createOrUpdateLocalState(userId, messageId, localAttachmentId)
        } returns Unit.right()
    }

    private fun expectGetFileSizeFromUriSuccess(expectedUri: Uri = uri) {
        coEvery { attachmentRepository.getFileSizeFromUri(expectedUri) } returns 1L.right()
    }

    private fun expectGetFileSizeFromUriFailed() {
        coEvery { attachmentRepository.getFileSizeFromUri(uri) } returns DataError.Local.NoDataCached.left()
    }

    private fun expectGetFileSizeExceedsLimit() {
        coEvery { attachmentRepository.getFileSizeFromUri(uri) } returns (30 * 1000 * 1000L).right()
    }
}
