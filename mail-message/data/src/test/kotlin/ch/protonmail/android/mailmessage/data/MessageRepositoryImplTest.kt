/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data

import java.io.IOException
import app.cash.turbine.test
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.data.repository.MessageRepositoryImpl
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class MessageRepositoryImplTest {

    private val userId = UserId("1")

    private val remoteDataSource = mockk<MessageRemoteDataSource>(relaxUnitFun = true) {
        coEvery { getMessages(userId = any(), pageKey = any()) } returns listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000),
            getMessage(id = "4", time = 4000)
        )
    }
    private val localDataSource = mockk<MessageLocalDataSource>(relaxUnitFun = true) {
        coEvery { addLabel(userId = any(), messageId = any(), labelId = any()) } returns MessageTestData.message.right()
        coEvery { observeMessage(userId = any(), messageId = any()) } returns flowOf(MessageTestData.message)
        coEvery { getMessages(userId = any(), pageKey = any()) } returns emptyList()
        coEvery { isLocalPageValid(userId = any(), pageKey = any(), items = any()) } returns false
    }

    private val messageRepository = MessageRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource
    )

    @Test
    fun `return remote if local page is invalid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000)
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000)
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages

        // When
        val result = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(userId, pageKey, remoteMessages) }
    }

    @Test
    fun `return local if remote fail`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000)
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } throws IOException()

        // When
        val result = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(2, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
    }

    @Test
    fun `return local if valid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000)
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000)
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages

        // When
        val messages = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(2, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 0) { remoteDataSource.getMessages(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { localDataSource.getMessages(any(), any()) } returns emptyList()
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns emptyList()
        // When
        val messages = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(0, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            localDataSource.getClippedPageKey(userId, pageKey)
            remoteDataSource.getMessages(userId, clippedPageKey)
        }
    }

    @Test
    fun `observe cached message emits message when existing in cache`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val message = getMessage(userId, "1")
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(message)
        // When
        messageRepository.observeCachedMessage(userId, messageId).test {
            // Then
            assertEquals(message.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe cached message emits no data cached error when message does not exist in cache`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(null)
        // When
        messageRepository.observeCachedMessage(userId, messageId).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe cached messages for a conversation id calls the local source with correct parameters`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val conversationId = ConversationIdSample.WeatherForecast
        val messages = nonEmptyListOf(
            MessageSample.AugWeatherForecast,
            MessageSample.SepWeatherForecast
        )
        every { localDataSource.observeMessages(userId, conversationId) } returns flowOf(messages)

        // when
        messageRepository.observeCachedMessages(userId, conversationId).test {

            // then
            assertEquals(messages.right(), awaitItem())
            verify { localDataSource.observeMessages(userId, conversationId) }
            awaitComplete()
        }
    }

    @Test
    fun `observe cached messages for a conversation emits error when the list is empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val conversationId = ConversationIdSample.WeatherForecast
        every { localDataSource.observeMessages(userId, conversationId) } returns flowOf(emptyList())
        // When
        messageRepository.observeCachedMessages(userId, conversationId).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `add label emits message with label when adding label was successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.addLabel(
                userId = userId,
                messageId = messageId,
                labelId = labelId
            )
        } returns MessageTestData.starredMessage.right()
        // When
        val actual = messageRepository.addLabel(userId, messageId, labelId)
        // Then
        val starredMessage = MessageTestData.starredMessage
        coVerify { localDataSource.addLabel(userId, messageId, labelId) }
        assertEquals(starredMessage.right(), actual)
    }

    @Test
    fun `add label updates remote data source`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        // When
        messageRepository.addLabel(userId, messageId, labelId)
        // Then
        coVerify { remoteDataSource.addLabel(userId, messageId, labelId) }
    }

    @Test
    fun `add label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val labelId = LabelId("42")
        coEvery {
            localDataSource.addLabel(
                userId,
                messageId,
                labelId
            )
        } returns DataError.Local.NoDataCached.left()
        // When
        val actual = messageRepository.addLabel(userId, messageId, labelId)
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `remove label returns message without label when successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.removeLabel(userId, messageId, labelId)
        } returns MessageTestData.message.right()
        // When
        val actual = messageRepository.removeLabel(userId, messageId, LabelId("10"))
        // Then
        val unstarredMessage = MessageTestData.message
        assertEquals(unstarredMessage.right(), actual)
    }

    @Test
    fun `remove label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        coEvery {
            localDataSource.removeLabel(userId, messageId, LabelId("42"))
        } returns DataError.Local.NoDataCached.left()
        // When
        val actual = messageRepository.removeLabel(userId, messageId, LabelId("42"))
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }
}
