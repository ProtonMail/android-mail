package ch.protonmail.android.mailmessage.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveMessagesTest {

    private val repository = mockk<MessageRepository>()

    private val observeMessage = ObserveMessages(repository)

    @Test
    fun `returns local data error when message does not exist in repository`() = runTest {
        // Given
        val messageIds = listOf(MessageId(MessageTestData.RAW_MESSAGE_ID))
        val error = DataError.Local.NoDataCached
        every { repository.observeCachedMessages(UserIdTestData.userId, messageIds) } returns flowOf(error.left())

        // When
        observeMessage(UserIdTestData.userId, messageIds).test {
            // Then
            assertEquals(error.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns message when it exists in repository`() = runTest {
        // Given
        val messageIds = listOf(MessageId(MessageTestData.RAW_MESSAGE_ID))
        val messages = listOf(MessageTestData.message)
        every { repository.observeCachedMessages(UserIdTestData.userId, messageIds) } returns flowOf(messages.right())

        // When
        observeMessage(UserIdTestData.userId, messageIds).test {
            // Then
            assertEquals(messages.right(), awaitItem())
            awaitComplete()
        }
    }

}
