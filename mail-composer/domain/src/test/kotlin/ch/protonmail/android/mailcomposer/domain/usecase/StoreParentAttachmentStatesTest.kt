package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreParentAttachmentStatesTest {

    private val userId = UserIdSample.Primary
    private val messageId = MessageIdSample.Invoice
    private val attachmentIds = listOf(AttachmentId("1"), AttachmentId("2"))
    private val attachmentStateRepository = mockk<AttachmentStateRepository>()

    private val storeParentAttachmentStates = StoreParentAttachmentStates(attachmentStateRepository)

    @Test
    fun `when store parent attachments is called then the attachment state repository is called`() = runTest {
        // Given
        expectCreateOrUpdateStatesSucceeds(expectedSyncState = AttachmentSyncState.External)

        // When
        val actual = storeParentAttachmentStates(userId, messageId, attachmentIds, AttachmentSyncState.External)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `when store parent attachment fails then data error is returned`() = runTest {
        // Given
        expectCreateOrUpdateStatesFails()

        // When
        val actual = storeParentAttachmentStates(userId, messageId, attachmentIds, AttachmentSyncState.External)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
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

    private fun expectCreateOrUpdateStatesFails() {
        coEvery {
            attachmentStateRepository.createOrUpdateLocalStates(
                userId,
                messageId,
                attachmentIds,
                AttachmentSyncState.External
            )
        } returns DataError.Local.Unknown.left()
    }
}
