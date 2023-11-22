package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UpdateParentAttachmentStatesTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentIds = listOf(AttachmentId("1"), AttachmentId("2"))
    private val attachmentStateRepository = mockk<AttachmentStateRepository>()

    private val createOrUpdateParentAttachmentStates = CreateOrUpdateParentAttachmentStates(attachmentStateRepository)

    @Test
    fun `when update parent attachment states for an existing and a new attachment then the repository is called`() =
        runTest {
            // Given
            expectAttachmentStateIsParent(attachmentIds[0])
            expectAttachmentStateIsNull(attachmentIds[1])
            expectCreateOrUpdateStatesSucceeds()

            // When
            createOrUpdateParentAttachmentStates(userId, messageId, attachmentIds)

            // Then
            coVerify {
                attachmentStateRepository.createOrUpdateLocalStates(
                    userId,
                    messageId,
                    attachmentIds,
                    AttachmentSyncState.ExternalUploaded
                )
            }
        }

    @Test
    fun `when update parent attachment states with a local attachment is called then the local is filtered`() =
        runTest {
            // Given
            expectAttachmentStateIsParent(attachmentIds[0])
            expectAttachmentStateIsLocal(attachmentIds[1])
            expectCreateOrUpdateStatesSucceeds(listOf(attachmentIds[0]))

            // When
            createOrUpdateParentAttachmentStates(userId, messageId, attachmentIds)

            // Then
            coVerify {
                attachmentStateRepository.createOrUpdateLocalStates(
                    userId,
                    messageId,
                    listOf(attachmentIds[0]),
                    AttachmentSyncState.ExternalUploaded
                )
            }
        }

    private fun expectAttachmentStateIsParent(attachmentId: AttachmentId) {
        coEvery {
            attachmentStateRepository.getAttachmentState(userId, messageId, attachmentId)
        } returns AttachmentState(userId, messageId, attachmentId, AttachmentSyncState.External).right()
    }

    private fun expectAttachmentStateIsNull(attachmentId: AttachmentId) {
        coEvery {
            attachmentStateRepository.getAttachmentState(userId, messageId, attachmentId)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectAttachmentStateIsLocal(attachmentId: AttachmentId) {
        coEvery {
            attachmentStateRepository.getAttachmentState(userId, messageId, attachmentId)
        } returns AttachmentState(userId, messageId, attachmentId, AttachmentSyncState.Local).right()
    }

    private fun expectCreateOrUpdateStatesSucceeds(ids: List<AttachmentId> = attachmentIds) {
        coEvery {
            attachmentStateRepository.createOrUpdateLocalStates(
                userId,
                messageId,
                ids,
                AttachmentSyncState.ExternalUploaded
            )
        } returns Unit.right()
    }

}
