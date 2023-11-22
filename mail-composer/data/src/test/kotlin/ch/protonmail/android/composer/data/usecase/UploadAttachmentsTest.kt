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

import java.io.File
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.composer.data.remote.UploadAttachmentModel
import ch.protonmail.android.composer.data.remote.response.UploadAttachmentResponse
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.AttachmentStateSample
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailmessage.data.sample.AttachmentResourceSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.exception.CryptoException
import kotlin.test.Test
import kotlin.test.assertEquals

class UploadAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val senderAddress = UserAddressSample.PrimaryAddress
    private val messageId = MessageIdSample.RemoteDraft
    private val localDraft = MessageWithBodySample.RemoteDraft
    private val localStoredAttachment = File.createTempFile("attachment", "txt")
    private val encryptedAttachmentResult = EncryptedAttachmentResult(
        keyPacket = "key_packet".toByteArray(),
        encryptedAttachment = File.createTempFile("encrypted_attachment", "txt"),
        signature = "signature".toByteArray()
    )
    private val expectedMessageAttachment = buildMessageAttachment()
    private val expectedRemoteAttachment = AttachmentResourceSample.build("text/plain")
    private val expectedRemoteMessageAttachment = expectedRemoteAttachment.toMessageAttachment()
    private val uploadExpectedAttachmentResult = UploadAttachmentResponse(
        code = 1000,
        attachment = expectedRemoteAttachment
    )
    private val expectedUploadAttachmentModel = UploadAttachmentModel(
        messageId = messageId,
        fileName = localStoredAttachment.name,
        mimeType = expectedMessageAttachment.mimeType,
        keyPacket = encryptedAttachmentResult.keyPacket,
        attachment = encryptedAttachmentResult.encryptedAttachment,
        signature = encryptedAttachmentResult.signature
    )

    private val attachmentRepository = mockk<AttachmentRepository>()
    private val attachmentStateRepository = mockk<AttachmentStateRepository>()
    private val attachmentRemoteDataSource = mockk<AttachmentRemoteDataSource>()
    private val findLocalDraft = mockk<FindLocalDraft>()
    private val encryptAndSignAttachment = mockk<EncryptAndSignAttachment>()
    private val resolveUserAddress = mockk<ResolveUserAddress>()
    private val fakeTransactor = FakeTransactor()

    private val uploadAttachments by lazy {
        UploadAttachments(
            attachmentRepository,
            attachmentStateRepository,
            attachmentRemoteDataSource,
            findLocalDraft,
            encryptAndSignAttachment,
            resolveUserAddress,
            fakeTransactor
        )
    }

    @Test
    fun `when upload attachment is successful then the attachment state gets updated with the remote id`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        expectGetAllAttachmentStatesSuccessful()
        expectResolveUserAddressSuccessful()
        expectReadingFileFromStorageSuccessful()
        expectLoadingAttachmentMetadataSuccessful()
        expectEncryptingAndSigningSuccessful()
        expectUploadSuccessful()
        expectUpdateAttachmentStateSuccessful()
        expectUpdateMessageAttachmentSuccessful()

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        coVerifyOrder {
            attachmentRepository.updateMessageAttachment(
                userId,
                messageId,
                AttachmentStateSample.LocalAttachmentState.attachmentId,
                expectedRemoteMessageAttachment
            )
            attachmentStateRepository.setAttachmentToUploadState(
                userId,
                messageId,
                AttachmentId(uploadExpectedAttachmentResult.attachment.id)
            )
        }
    }

    @Test
    fun `when upload attachment fails due to missing local draft then draft not found error is returned`() = runTest {
        // Given
        coEvery { findLocalDraft(userId, messageId) } returns null

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(AttachmentUploadError.DraftNotFound.left(), actual)
        verify { attachmentStateRepository wasNot Called }
        verify { attachmentRemoteDataSource wasNot Called }
        verify { attachmentRepository wasNot Called }
        verify { encryptAndSignAttachment wasNot Called }
        verify { resolveUserAddress wasNot Called }
    }

    @Test
    fun `when no attachment states are stored then Unit is returned`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        coEvery {
            attachmentStateRepository.getAllAttachmentStatesForMessage(userId, messageId)
        } returns emptyList()

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        verify { attachmentRemoteDataSource wasNot Called }
        verify { attachmentRepository wasNot Called }
        verify { encryptAndSignAttachment wasNot Called }
        verify { resolveUserAddress wasNot Called }
    }

    @Test
    fun `when upload attachment fails due to missing sender address then Sender address not found error is returned`() =
        runTest {
            // Given
            expectFindLocalDraftSuccessful()
            expectGetAllAttachmentStatesSuccessful()
            expectResolveUserAddressFailed()

            // When
            val actual = uploadAttachments(userId, messageId)

            // Then
            assertEquals(AttachmentUploadError.SenderAddressNotFound.left(), actual)
            verify { attachmentRemoteDataSource wasNot Called }
            verify { attachmentRepository wasNot Called }
            verify { encryptAndSignAttachment wasNot Called }
        }

    @Test
    fun `when upload attachment fails due to missing attachment file then attachment file not found error returned`() =
        runTest {
            // Given
            expectFindLocalDraftSuccessful()
            expectGetAllAttachmentStatesSuccessful()
            expectResolveUserAddressSuccessful()
            expectReadingFileFromStorageFailed()

            // When
            val actual = uploadAttachments(userId, messageId)

            // Then
            assertEquals(AttachmentUploadError.AttachmentFileNotFound.left(), actual)
            verify { attachmentRemoteDataSource wasNot Called }
            verify { encryptAndSignAttachment wasNot Called }
        }

    @Test
    fun `when upload attachment fails due to missing attachment metadata then attachment info not found is returned`() =
        runTest {
            // Given
            expectFindLocalDraftSuccessful()
            expectGetAllAttachmentStatesSuccessful()
            expectResolveUserAddressSuccessful()
            expectReadingFileFromStorageSuccessful()
            expectLoadingAttachmentMetadataFailed()

            // When
            val actual = uploadAttachments(userId, messageId)

            // Then
            assertEquals(AttachmentUploadError.AttachmentInfoNotFound.left(), actual)
            verify { attachmentRemoteDataSource wasNot Called }
            verify { encryptAndSignAttachment wasNot Called }
        }

    @Test
    fun `when upload attachment fails due to encryption error then Unit is returned`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        expectGetAllAttachmentStatesSuccessful()
        expectResolveUserAddressSuccessful()
        expectReadingFileFromStorageSuccessful()
        expectLoadingAttachmentMetadataSuccessful()
        expectEncryptingAndSigningFailed()

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(AttachmentUploadError.FailedToEncryptAttachment.left(), actual)
        verify { attachmentRemoteDataSource wasNot Called }
    }

    @Test
    fun `when the attachment state is Uploaded then the attachment is not uploaded`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        expectGetAllAttachmentStatesSuccessful(
            listOf(AttachmentStateSample.LocalAttachmentState.copy(state = AttachmentSyncState.Uploaded))
        )

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        verify { attachmentRemoteDataSource wasNot Called }
        verify { attachmentRepository wasNot Called }
        verify { encryptAndSignAttachment wasNot Called }
        verify { resolveUserAddress wasNot Called }
    }

    @Test
    fun `when the attachment state is ParentUploaded then the attachment is not uploaded`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        expectGetAllAttachmentStatesSuccessful(
            listOf(AttachmentStateSample.LocalAttachmentState.copy(state = AttachmentSyncState.ExternalUploaded))
        )

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        verify { attachmentRemoteDataSource wasNot Called }
        verify { attachmentRepository wasNot Called }
        verify { encryptAndSignAttachment wasNot Called }
        verify { resolveUserAddress wasNot Called }
    }

    @Test
    fun `when the attachment state is Parent then the attachment is not uploaded`() = runTest {
        // Given
        expectFindLocalDraftSuccessful()
        expectGetAllAttachmentStatesSuccessful(
            listOf(AttachmentStateSample.LocalAttachmentState.copy(state = AttachmentSyncState.External))
        )

        // When
        val actual = uploadAttachments(userId, messageId)

        // Then
        assertEquals(Unit.right(), actual)
        verify { attachmentRemoteDataSource wasNot Called }
        verify { attachmentRepository wasNot Called }
        verify { encryptAndSignAttachment wasNot Called }
        verify { resolveUserAddress wasNot Called }
    }

    private fun expectFindLocalDraftSuccessful() {
        coEvery { findLocalDraft(userId, messageId) } returns localDraft
    }

    private fun expectGetAllAttachmentStatesSuccessful(
        states: List<AttachmentState> = listOf(AttachmentStateSample.LocalAttachmentState)
    ) {
        coEvery {
            attachmentStateRepository.getAllAttachmentStatesForMessage(userId, messageId)
        } returns states
    }

    private fun expectResolveUserAddressSuccessful() {
        coEvery {
            resolveUserAddress(userId, localDraft.message.addressId)
        } returns senderAddress.right()
    }

    private fun expectResolveUserAddressFailed() {
        coEvery {
            resolveUserAddress(userId, localDraft.message.addressId)
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }

    private fun expectReadingFileFromStorageSuccessful(
        attachmentId: AttachmentId = AttachmentStateSample.LocalAttachmentState.attachmentId,
        attachment: File = localStoredAttachment
    ) {
        coEvery {
            attachmentRepository.readFileFromStorage(
                userId,
                messageId,
                attachmentId
            )
        } returns attachment.right()
    }

    private fun expectReadingFileFromStorageFailed(
        attachmentId: AttachmentId = AttachmentStateSample.LocalAttachmentState.attachmentId
    ) {
        coEvery {
            attachmentRepository.readFileFromStorage(
                userId,
                messageId,
                attachmentId
            )
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectLoadingAttachmentMetadataSuccessful(
        attachmentId: AttachmentId = AttachmentStateSample.LocalAttachmentState.attachmentId,
        messageAttachment: MessageAttachment = expectedMessageAttachment
    ) {
        coEvery {
            attachmentRepository.getAttachmentInfo(
                userId,
                messageId,
                attachmentId
            )
        } returns messageAttachment.right()
    }

    private fun expectLoadingAttachmentMetadataFailed() {
        coEvery {
            attachmentRepository.getAttachmentInfo(
                userId,
                messageId,
                AttachmentStateSample.LocalAttachmentState.attachmentId
            )
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectEncryptingAndSigningSuccessful() {
        coEvery {
            encryptAndSignAttachment(
                senderAddress,
                localStoredAttachment
            )
        } returns encryptedAttachmentResult.right()
    }

    private fun expectEncryptingAndSigningFailed(file: File = localStoredAttachment) {
        coEvery {
            encryptAndSignAttachment(
                senderAddress,
                file
            )
        } returns AttachmentEncryptionError.FailedToEncryptAttachment(CryptoException("Failed")).left()
    }

    private fun expectUploadSuccessful() {
        coEvery {
            attachmentRemoteDataSource.uploadAttachment(userId, expectedUploadAttachmentModel)
        } returns uploadExpectedAttachmentResult.right()
    }

    private fun expectUpdateAttachmentStateSuccessful() {
        coEvery {
            attachmentStateRepository.setAttachmentToUploadState(
                userId,
                messageId,
                AttachmentId(uploadExpectedAttachmentResult.attachment.id)
            )
        } returns Unit.right()
    }

    private fun expectUpdateMessageAttachmentSuccessful() {
        coEvery {
            attachmentRepository.updateMessageAttachment(
                userId,
                messageId,
                AttachmentStateSample.LocalAttachmentState.attachmentId,
                expectedRemoteMessageAttachment
            )
        } returns Unit.right()
    }

    private fun buildMessageAttachment(attachmentId: AttachmentId = AttachmentId("attachment_id")) = MessageAttachment(
        attachmentId = attachmentId,
        name = localStoredAttachment.name,
        size = localStoredAttachment.length(),
        mimeType = "text/plain",
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )
}
