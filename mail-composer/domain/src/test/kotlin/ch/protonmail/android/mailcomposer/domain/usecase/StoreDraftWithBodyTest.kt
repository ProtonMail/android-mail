package ch.protonmail.android.mailcomposer.domain.usecase

import android.util.Log
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.TestTree
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import kotlin.test.Test
import kotlin.test.BeforeTest
import timber.log.Timber
import kotlin.test.assertEquals

class StoreDraftWithBodyTest {

    private val testTree = TestTree()

    private val createEmptyDraftMock = mockk<CreateEmptyDraft>()
    private val encryptDraftBodyMock = mockk<EncryptDraftBody>()
    private val saveDraftMock = mockk<SaveDraft>()
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val storeDraftWithBody = StoreDraftWithBody(
        createEmptyDraftMock,
        encryptDraftBodyMock,
        saveDraftMock,
        messageRepositoryMock
    )

    @BeforeTest
    fun setUp() {
        Timber.plant(testTree)
    }

    @Test
    fun `should save an existing draft with encrypted body when draft already exists`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = UserIdSample.Primary
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
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderAddress, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should save a new draft with encrypted body when draft does not yet exist`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = UserIdSample.Primary
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
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderAddress, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should return error and not save draft when encryption fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        expectedExistingDraft(userId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        givenDraftBodyEncryptionFails()

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderAddress, userId)

        // Then
        coVerify { saveDraftMock wasNot called }
        assertEquals(StoreDraftWithBodyError.DraftBodyEncryptionError.left(), actualEither)
        assertErrorLogged("Encrypt draft $draftMessageId body to store to local DB failed")
    }

    @Test
    fun `should return error when saving fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val expectedUserId = UserIdSample.Primary
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
        givenSaveDraftFails(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderAddress, expectedUserId)

        // Then
        assertEquals(StoreDraftWithBodyError.DraftSaveError.left(), actualEither)
        assertErrorLogged("Store draft $draftMessageId body to local DB failed")
    }

    private fun assertErrorLogged(message: String) {
        val expectedLog = TestTree.Log(Log.ERROR, null, message, null)
        assertEquals(expectedLog, testTree.logs.lastOrNull())
    }

    private fun expectedExistingDraft(
        userId: UserId,
        messageId: MessageId,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getLocalMessageWithBody(userId, messageId) } returns it
    }

    private fun expectedNewDraft(
        userId: UserId,
        messageId: MessageId,
        senderAddress: UserAddress,
        existingDraft: () -> MessageWithBody
    ): MessageWithBody = existingDraft().also {
        coEvery { messageRepositoryMock.getLocalMessageWithBody(userId, messageId) } returns null
        every { createEmptyDraftMock(messageId, userId, senderAddress) } returns it
    }

    private fun expectedEncryptedDraftBody(
        plaintextDraftBody: DraftBody,
        senderAddress: UserAddress,
        expectedEncryptedDraftBody: () -> DraftBody
    ): DraftBody = expectedEncryptedDraftBody().also {
        coEvery { encryptDraftBodyMock(plaintextDraftBody, senderAddress) } returns it.right()
    }

    private fun givenDraftBodyEncryptionFails() {
        coEvery { encryptDraftBodyMock(any(), any()) } returns Unit.left()
    }

    private fun givenSaveDraftSucceeds(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns true
    }

    private fun givenSaveDraftFails(messageWithBody: MessageWithBody, userId: UserId) {
        coEvery { saveDraftMock(messageWithBody, userId) } returns false
    }
}
