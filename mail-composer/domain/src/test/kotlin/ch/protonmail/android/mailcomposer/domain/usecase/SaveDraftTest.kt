package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SaveDraftTest {

    private val messageRepositoryMock = mockk<MessageRepository>(relaxUnitFun = true)
    private val saveDraft = SaveDraft(messageRepositoryMock)

    @Test
    fun `should upsert message with body when saving the draft`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft

        // When
        saveDraft(expectedMessageWithBody, expectedUserId)

        // Then
        coVerify { messageRepositoryMock.upsertMessageWithBody(expectedUserId, expectedMessageWithBody) }
    }
}
