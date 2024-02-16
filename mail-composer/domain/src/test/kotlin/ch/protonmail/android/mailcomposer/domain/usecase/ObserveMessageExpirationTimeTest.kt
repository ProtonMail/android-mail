package ch.protonmail.android.mailcomposer.domain.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.repository.MessageExpirationTimeRepository
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.FakeTransactor
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class ObserveMessageExpirationTimeTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.NewDraftWithSubjectAndBody
    private val apiMessageId = MessageId("apiMessageId")
    private val messageExpirationTime = MessageExpirationTime(userId, messageId, 1.days)

    private val draftStateRepository = mockk<DraftStateRepository>()
    private val messageExpirationTimeRepository = mockk<MessageExpirationTimeRepository>()
    private val transactor = FakeTransactor()

    private val observeMessageExpirationTime = ObserveMessageExpirationTime(
        draftStateRepository = draftStateRepository,
        messageExpirationTimeRepository = messageExpirationTimeRepository,
        transactor = transactor
    )

    @Test
    fun `should return expiration time for message id when expiration time is emitted`() = runTest {
        // Given
        expectApiMessageIdDoesNotExist()
        coEvery {
            messageExpirationTimeRepository.observeMessageExpirationTime(userId, messageId)
        } returns flowOf(messageExpirationTime)

        // When
        observeMessageExpirationTime(userId, messageId).test {
            // Then
            assertEquals(messageExpirationTime, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return expiration time for api message id when expiration time is emitted`() = runTest {
        // Given
        expectApiMessageIdExists()
        coEvery {
            messageExpirationTimeRepository.observeMessageExpirationTime(userId, apiMessageId)
        } returns flowOf(messageExpirationTime)

        // When
        observeMessageExpirationTime(userId, messageId).test {
            // Then
            assertEquals(messageExpirationTime, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return null when message expiration time does not exist`() = runTest {
        // Given
        expectApiMessageIdDoesNotExist()
        coEvery { messageExpirationTimeRepository.observeMessageExpirationTime(userId, messageId) } returns flowOf(null)

        // When
        observeMessageExpirationTime(userId, messageId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    private fun expectApiMessageIdExists() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = apiMessageId,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }

    private fun expectApiMessageIdDoesNotExist() {
        coEvery {
            draftStateRepository.observe(userId, messageId)
        } returns flowOf(
            DraftState(
                userId = userId,
                messageId = messageId,
                apiMessageId = null,
                state = DraftSyncState.Synchronized,
                action = DraftAction.Compose,
                sendingError = null,
                sendingStatusConfirmed = false
            ).right()
        )
    }
}
