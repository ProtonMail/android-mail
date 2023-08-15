package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveDraftTest {

    private val messageRepositoryMock = mockk<MessageRepository>(relaxUnitFun = true)
    private val saveDraft = SaveDraft(messageRepositoryMock)

    @Test
    fun `should upsert message with body when saving the draft`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft
        givenRepositorySucceeds(expectedMessageWithBody, expectedUserId)

        // When
        val draftSaved = saveDraft(expectedMessageWithBody, expectedUserId)

        // Then
        assertTrue(draftSaved)
    }

    @Test
    fun `should return false when saving a draft fails`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft
        givenRepositoryFails(expectedMessageWithBody, expectedUserId)

        // When
        val draftSaved = saveDraft(expectedMessageWithBody, expectedUserId)

        // Then
        assertFalse(draftSaved)
    }

    private fun givenRepositoryFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { messageRepositoryMock.upsertMessageWithBody(userId, messageWithBody) } returns false
    }

    private fun givenRepositorySucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { messageRepositoryMock.upsertMessageWithBody(userId, messageWithBody) } returns true
    }
}
