package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class DeleteAttachmentTest {

    private val userId = UserId("userId")
    private val senderEmail = SenderEmail("senderEmail")
    private val messageId = MessageIdSample.MessageWithAttachments
    private val attachmentId = AttachmentId("attachmentId")

    private val getLocalDraft: GetLocalDraft = mockk()
    private val attachmentRepository: AttachmentRepository = mockk()

    private val deleteAttachment = DeleteAttachment(getLocalDraft, attachmentRepository)

    @Test
    fun `deleteAttachment should call attachment repository`() = runTest {
        // Given
        expectGetLocalDraftSucceeds()
        expectComposerDeleteAttachmentSucceeds()

        // When
        deleteAttachment(userId, senderEmail, messageId, attachmentId)

        // Then
        coVerifyOrder {
            attachmentRepository.deleteAttachment(userId, messageId, attachmentId)
        }
    }

    @Test
    fun `deleteAttachment should return draft not found error when draft doesn't exist`() = runTest {
        // Given
        val expected = AttachmentDeleteError.DraftNotFound.left()
        expectGetLocalDraftFails()

        // When
        val actual = deleteAttachment(userId, senderEmail, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
        coVerify { attachmentRepository wasNot Called }
    }

    @Test
    fun `deleteAttachment should return failed to delete file error when file deletion failed`() = runTest {
        // Given
        val expected = AttachmentDeleteError.FailedToDeleteFile.left()
        expectGetLocalDraftSucceeds()
        expectComposerAttachmentDeleteFails(DataError.Local.FailedToDeleteFile.left())

        // When
        val actual = deleteAttachment(userId, senderEmail, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `deleteAttachment should return unknown error when unknown error is returned from the repo failed`() = runTest {
        // Given
        val expected = AttachmentDeleteError.Unknown.left()
        expectGetLocalDraftSucceeds()
        expectComposerAttachmentDeleteFails(DataError.Local.Unknown.left())

        // When
        val actual = deleteAttachment(userId, senderEmail, messageId, attachmentId)

        // Then
        assertEquals(expected, actual)
    }

    private fun expectComposerDeleteAttachmentSucceeds() {
        coEvery { attachmentRepository.deleteAttachment(userId, messageId, attachmentId) } returns Unit.right()
    }

    private fun expectGetLocalDraftSucceeds() {
        coEvery {
            getLocalDraft(userId, messageId, senderEmail)
        } returns MessageWithBodySample.MessageWithAttachments.right()
    }

    private fun expectGetLocalDraftFails() {
        coEvery {
            getLocalDraft(userId, messageId, senderEmail)
        } returns GetLocalDraft.Error.ResolveUserAddressError.left()
    }

    private fun expectComposerAttachmentDeleteFails(error: Either<DataError.Local, Nothing>) {
        coEvery { attachmentRepository.deleteAttachment(userId, messageId, attachmentId) } returns error
    }
}
