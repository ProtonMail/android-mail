package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.MessageExpirationTimeRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class SaveMessageExpirationTimeTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.NewDraftWithSubjectAndBody
    private val senderEmail = SenderEmail("sender@pm.me")
    private val expiresIn = 1.days

    private val getLocalDraft = mockk<GetLocalDraft>()
    private val messageExpirationTimeRepository = mockk<MessageExpirationTimeRepository>()
    private val messageRepository = mockk<MessageRepository>()
    private val saveDraft = mockk<SaveDraft>()
    private val transactor = FakeTransactor()

    private val saveMessageExpirationTime = SaveMessageExpirationTime(
        getLocalDraft, messageExpirationTimeRepository, messageRepository, saveDraft, transactor
    )

    @Test
    fun `should return unit when draft exists and message expiration time stored successfully`() = runTest {
        // Given
        expectDraftAlreadyExists()
        coEvery {
            messageExpirationTimeRepository.saveMessageExpirationTime(
                MessageExpirationTime(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    expiresIn
                )
            )
        } returns Unit.right()

        // When
        val actual = saveMessageExpirationTime(userId, messageId, senderEmail, expiresIn)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return unit when draft is saved and message expiration time is stored successfully`() = runTest {
        // Given
        expectDraftDoesNotExist()
        coEvery { saveDraft(MessageWithBodySample.EmptyDraft, userId) } returns true
        coEvery {
            messageExpirationTimeRepository.saveMessageExpirationTime(
                MessageExpirationTime(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    expiresIn
                )
            )
        } returns Unit.right()

        // When
        val actual = saveMessageExpirationTime(userId, messageId, senderEmail, expiresIn)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return error when getting the local draft has failed `() = runTest {
        // Given
        coEvery {
            getLocalDraft(userId, messageId, senderEmail)
        } returns GetLocalDraft.Error.ResolveUserAddressError.left()

        // When
        val actual = saveMessageExpirationTime(userId, messageId, senderEmail, expiresIn)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `should return error when saving draft has failed`() = runTest {
        // Given
        expectDraftDoesNotExist()
        coEvery { saveDraft(MessageWithBodySample.EmptyDraft, userId) } returns false

        // When
        val actual = saveMessageExpirationTime(userId, messageId, senderEmail, expiresIn)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }

    @Test
    fun `should return error when saving of message expiration time fails`() = runTest {
        // Given
        expectDraftAlreadyExists()
        coEvery {
            messageExpirationTimeRepository.saveMessageExpirationTime(
                MessageExpirationTime(
                    userId,
                    MessageWithBodySample.EmptyDraft.message.messageId,
                    expiresIn
                )
            )
        } returns DataError.Local.Unknown.left()

        // When
        val actual = saveMessageExpirationTime(userId, messageId, senderEmail, expiresIn)

        // Then
        assertEquals(DataError.Local.Unknown.left(), actual)
    }

    private fun expectDraftAlreadyExists() {
        coEvery { getLocalDraft(userId, messageId, senderEmail) } returns MessageWithBodySample.EmptyDraft.right()
        coEvery {
            messageRepository.getLocalMessageWithBody(userId, MessageWithBodySample.EmptyDraft.message.messageId)
        } returns MessageWithBodySample.EmptyDraft
    }

    private fun expectDraftDoesNotExist() {
        coEvery { getLocalDraft(userId, messageId, senderEmail) } returns MessageWithBodySample.EmptyDraft.right()
        coEvery {
            messageRepository.getLocalMessageWithBody(userId, MessageWithBodySample.EmptyDraft.message.messageId)
        } returns null
    }
}
