package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Test
import kotlin.test.assertEquals

class HandleDraftBodyChangeTest {

    private val createEmptyDraftMock = mockk<CreateEmptyDraft>()
    private val encryptDraftBodyMock = mockk<EncryptDraftBody>()
    private val saveDraftMock = mockk<SaveDraft>(relaxUnitFun = true)
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val handleDraftBodyChange = HandleDraftBodyChange(
        createEmptyDraftMock,
        encryptDraftBodyMock,
        saveDraftMock,
        messageRepositoryMock,
        observePrimaryUserIdMock
    )

    @Test
    fun `should save an existing draft with encrypted body when draft already exists`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val draftMessageId = MessageIdSample.build()
        val existingDraft = expectedExistingDraft(expectedUserId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        val expectedEncryptedDraftBody = expectedEncryptedDraftBody(plaintextDraftBody, senderAddress) {
            DraftBody("I am encrypted")
        }
        val expectedSavedDraft = existingDraft.copy(
            messageBody = existingDraft.messageBody.copy(
                body = expectedEncryptedDraftBody.value
            )
        )

        // When
        val actualEither = handleDraftBodyChange(draftMessageId, plaintextDraftBody, senderAddress)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should save a new draft with encrypted body when draft does not yet exist`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val draftMessageId = MessageIdSample.build()
        val newDraft = expectedNewDraft(expectedUserId, draftMessageId, senderAddress) {
            MessageWithBodySample.EmptyDraft
        }
        val expectedEncryptedDraftBody = expectedEncryptedDraftBody(plaintextDraftBody, senderAddress) {
            DraftBody("I am encrypted")
        }
        val expectedSavedDraft = newDraft.copy(
            messageBody = newDraft.messageBody.copy(
                body = expectedEncryptedDraftBody.value
            )
        )

        // When
        val actualEither = handleDraftBodyChange(draftMessageId, plaintextDraftBody, senderAddress)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should return error and not save draft when encryption fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val draftMessageId = MessageIdSample.build()
        expectedExistingDraft(expectedUserId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        expectEncryptionFailure()

        // When
        val actualEither = handleDraftBodyChange(draftMessageId, plaintextDraftBody, senderAddress)

        // Then
        coVerify { saveDraftMock wasNot called }
        assertEquals(DraftBodyEncryptionFailure.left(), actualEither)
    }

    private fun expectedUserId(userId: () -> UserId): UserId = userId().also {
        coEvery { observePrimaryUserIdMock() } returns flowOf(it)
    }

    private fun expectedExistingDraft(
        userId: UserId,
        messageId: MessageId,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getMessageWithBody(userId, messageId) } returns it.right()
    }

    private fun expectedNewDraft(
        userId: UserId,
        messageId: MessageId,
        senderAddress: UserAddress,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getMessageWithBody(userId, messageId) } returns DataError.Local.Unknown.left()
        every { createEmptyDraftMock(messageId, userId, senderAddress) } returns it
    }

    private fun expectedEncryptedDraftBody(
        plaintextDraftBody: DraftBody,
        senderAddress: UserAddress,
        expectedEncryptedDraftBody: () -> DraftBody
    ): DraftBody = expectedEncryptedDraftBody().also {
        coEvery { encryptDraftBodyMock(plaintextDraftBody, senderAddress) } returns it.right()
    }

    private fun expectEncryptionFailure() {
        coEvery { encryptDraftBodyMock(any(), any()) } returns DraftBodyEncryptionFailure.left()
    }
}
