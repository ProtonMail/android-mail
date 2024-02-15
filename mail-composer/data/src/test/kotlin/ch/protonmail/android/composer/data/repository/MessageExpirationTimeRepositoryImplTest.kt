package ch.protonmail.android.composer.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.composer.data.local.MessageExpirationTimeLocalDataSource
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class MessageExpirationTimeRepositoryImplTest {

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val messageExpirationTimeLocalDataSource = mockk<MessageExpirationTimeLocalDataSource>()

    private val messageExpirationTimeRepository = MessageExpirationTimeRepositoryImpl(
        messageExpirationTimeLocalDataSource
    )

    @Test
    fun `should call method from local data source when saving message password`() = runTest {
        // Given
        val expiresIn = 1.days
        val messageExpirationTime = MessageExpirationTime(userId, messageId, expiresIn)
        coEvery { messageExpirationTimeLocalDataSource.save(messageExpirationTime) } returns Unit.right()

        // When
        val actual = messageExpirationTimeRepository.saveMessageExpirationTime(messageExpirationTime)

        // Then
        coVerify {
            messageExpirationTimeLocalDataSource.save(messageExpirationTime)
        }
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should call method from local data source when observing message password`() = runTest {
        // Given
        val expiresIn = 1.days
        val messageExpirationTime = MessageExpirationTime(userId, messageId, expiresIn)
        coEvery {
            messageExpirationTimeLocalDataSource.observe(userId, messageId)
        } returns flowOf(messageExpirationTime)

        // When
        messageExpirationTimeRepository.observeMessageExpirationTime(userId, messageId).test {
            // Then
            coVerify {
                messageExpirationTimeLocalDataSource.observe(userId, messageId)
            }
            assertEquals(messageExpirationTime, awaitItem())
            awaitComplete()
        }
    }
}
