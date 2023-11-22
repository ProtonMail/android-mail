package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.AttachmentStateSample
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StoreExternalAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentIds = listOf(AttachmentId("1"), AttachmentId("2"))
    private val attachmentStateRepository = mockk<AttachmentStateRepository>()
    private val messageRepository = mockk<MessageRepository>()

    private val storeExternalAttachments by lazy {
        StoreExternalAttachments(messageRepository, attachmentStateRepository)
    }

    @Test
    fun `when store parent attachment is called without attachments then only the not yet stored states are stored`() =
        runTest {
            // Given
            expectGetMessageWithBodySucceeds()
            expectGetAllAttachmentStatesForMessageSucceeds()
            val expectedAttachmentList = listOf(AttachmentId("embeddedImageId"))
            expectCreateOrUpdateStatesSucceeds(expectedAttachmentList, AttachmentSyncState.ExternalUploaded)

            // When
            storeExternalAttachments(userId, messageId)

            // Then
            coVerify {
                attachmentStateRepository.createOrUpdateLocalStates(
                    userId,
                    messageId,
                    expectedAttachmentList,
                    AttachmentSyncState.ExternalUploaded
                )
            }
        }

    private fun expectGetMessageWithBodySucceeds() {
        coEvery {
            messageRepository.getMessageWithBody(userId, messageId)
        } returns MessageWithBodySample.MessageWithAttachments.right()
    }

    private fun expectGetAllAttachmentStatesForMessageSucceeds() {
        coEvery {
            attachmentStateRepository.getAllAttachmentStatesForMessage(userId, messageId)
        } returns listOf(
            AttachmentStateSample.build(userId, messageId, AttachmentId("document"))
        )
    }

    private fun expectCreateOrUpdateStatesSucceeds(
        ids: List<AttachmentId> = attachmentIds,
        expectedSyncState: AttachmentSyncState
    ) {
        coEvery {
            attachmentStateRepository.createOrUpdateLocalStates(
                userId,
                messageId,
                ids,
                expectedSyncState
            )
        } returns Unit.right()
    }

}
