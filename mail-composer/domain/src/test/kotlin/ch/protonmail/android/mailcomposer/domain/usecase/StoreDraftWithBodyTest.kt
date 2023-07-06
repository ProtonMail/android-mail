package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.rule.LoggingTestRule
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreDraftWithBodyTest {

    @get:Rule
    val loggingTestRule = LoggingTestRule()

    private val createEmptyDraftMock = mockk<CreateEmptyDraft>()
    private val encryptDraftBodyMock = mockk<EncryptDraftBody>()
    private val saveDraftMock = mockk<SaveDraft>()
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val resolveUserAddressMock = mockk<ResolveUserAddress>()

    private val storeDraftWithBody = StoreDraftWithBody(
        createEmptyDraftMock,
        encryptDraftBodyMock,
        saveDraftMock,
        messageRepositoryMock,
        resolveUserAddressMock
    )

    @Test
    fun `should save an existing draft with encrypted body when draft already exists`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
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
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderEmail, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should save a new draft with encrypted body when draft does not yet exist`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
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
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }
        givenSaveDraftSucceeds(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderEmail, expectedUserId)

        // Then
        coVerify { saveDraftMock(expectedSavedDraft, expectedUserId) }
        assertEquals(Unit.right(), actualEither)
    }

    @Test
    fun `should return error and not save draft when encryption fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
        val userId = UserIdSample.Primary
        val draftMessageId = MessageIdSample.build()
        expectedExistingDraft(userId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        expectedResolvedUserAddress(userId, senderEmail) { senderAddress }
        givenDraftBodyEncryptionFails()

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderEmail, userId)

        // Then
        coVerify { saveDraftMock wasNot called }
        assertEquals(StoreDraftWithBodyError.DraftBodyEncryptionError.left(), actualEither)
        loggingTestRule.assertErrorLogged("Encrypt draft $draftMessageId body to store to local DB failed")
    }

    @Test
    fun `should return error when saving fails`() = runTest {
        // Given
        val plaintextDraftBody = DraftBody("I am plaintext")
        val senderAddress = UserAddressSample.build()
        val senderEmail = SenderEmail(senderAddress.email)
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
        expectedResolvedUserAddress(expectedUserId, senderEmail) { senderAddress }
        givenSaveDraftFails(expectedSavedDraft, expectedUserId)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, plaintextDraftBody, senderEmail, expectedUserId)

        // Then
        assertEquals(StoreDraftWithBodyError.DraftSaveError.left(), actualEither)
        loggingTestRule.assertErrorLogged("Store draft $draftMessageId body to local DB failed")
    }

    @Test
    fun `should return error when resolving the draft sender address fails`() = runTest {
        // Given
        val expectedSenderEmail = SenderEmail("unresolvable@sender.email")
        val expectedUserId = UserIdSample.Primary
        val expectedDraftBody = DraftBody("I am plaintext")
        val draftMessageId = MessageIdSample.build()
        expectedExistingDraft(expectedUserId, draftMessageId) { MessageWithBodySample.EmptyDraft }
        expectResolveUserAddressFailure(expectedUserId, expectedSenderEmail)

        // When
        val actualEither = storeDraftWithBody(draftMessageId, expectedDraftBody, expectedSenderEmail, expectedUserId)

        // Then
        assertEquals(StoreDraftWithBodyError.DraftResolveUserAddressError.left(), actualEither)
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

    private fun expectedResolvedUserAddress(
        userId: UserId,
        email: SenderEmail,
        address: () -> UserAddress
    ) = address().also { coEvery { resolveUserAddressMock(userId, email) } returns it.right() }

    private fun expectResolveUserAddressFailure(userId: UserId, email: SenderEmail) {
        coEvery { resolveUserAddressMock(userId, email) } returns ResolveUserAddress.Error.UserAddressNotFound.left()
    }
}
