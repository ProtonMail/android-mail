package ch.protonmail.android.composer.data.local

import java.io.IOException
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.local.dao.MessageExpirationTimeDao
import ch.protonmail.android.composer.data.local.entity.toEntity
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class MessageExpirationTimeLocalDataSourceImplTest {

    val userId = UserIdTestData.userId
    val messageId = MessageIdSample.NewDraftWithSubjectAndBody

    private val messageExpirationTimeDao = mockk<MessageExpirationTimeDao>()
    private val database = mockk<DraftStateDatabase> {
        every { messageExpirationTimeDao() } returns messageExpirationTimeDao
    }

    private val messageExpirationTimeLocalDataSource = MessageExpirationTimeLocalDataSourceImpl(database)

    @Test
    fun `should return unit when saving message expiration time was successful`() = runTest {
        // Given
        val expiresIn = 1.days
        val messageExpirationTime = MessageExpirationTime(userId, messageId, expiresIn)
        coEvery { messageExpirationTimeDao.insertOrUpdate(messageExpirationTime.toEntity()) } just runs

        // When
        val actual = messageExpirationTimeLocalDataSource.save(messageExpirationTime)

        // Then
        assertEquals(Unit.right(), actual)
    }

    @Test
    fun `should return error when saving message expiration time throws an exception`() = runTest {
        // Given
        val expiresIn = 1.days
        val messageExpirationTime = MessageExpirationTime(userId, messageId, expiresIn)
        coEvery { messageExpirationTimeDao.insertOrUpdate(messageExpirationTime.toEntity()) } throws IOException()

        // When
        val actual = messageExpirationTimeLocalDataSource.save(messageExpirationTime)

        // Then
        assertEquals(DataError.Local.DbWriteFailed.left(), actual)
    }

    @Test
    fun `should return message expiration time when observing and expiration time exists`() = runTest {
        // Given
        val expiresIn = 1.days
        val messageExpirationTime = MessageExpirationTime(userId, messageId, expiresIn)
        coEvery { messageExpirationTimeDao.observe(userId, messageId) } returns flowOf(messageExpirationTime.toEntity())

        // When
        messageExpirationTimeLocalDataSource.observe(userId, messageId).test {
            // Then
            assertEquals(messageExpirationTime, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should return null when observing and expiration time does not exist`() = runTest {
        // Given
        coEvery { messageExpirationTimeDao.observe(userId, messageId) } returns flowOf(null)

        // When
        messageExpirationTimeLocalDataSource.observe(userId, messageId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
