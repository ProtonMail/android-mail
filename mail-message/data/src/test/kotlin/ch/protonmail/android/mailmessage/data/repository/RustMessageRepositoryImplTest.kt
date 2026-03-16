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

package ch.protonmail.android.mailmessage.data.repository

import java.io.File
import app.cash.turbine.test
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.repository.RustConversationCursorImpl
import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoRepository
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.mailmessage.data.local.RustMessageDataSource
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessage
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailmessage.data.mapper.toRemoteMessageId
import ch.protonmail.android.mailmessage.domain.model.SenderImage
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import ch.protonmail.android.testdata.message.rust.LocalMessageTestData
import ch.protonmail.android.testdata.message.rust.RemoteMessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.proton_mail_uniffi.Id
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RustMessageRepositoryImplTest {

    private val rustMessageDataSource = mockk<RustMessageDataSource>()
    private val undoRepository = mockk<UndoRepository>()
    private val labelId = LabelIdSample.RustLabel1
    private val userId = UserId("userId")
    private val repository = RustMessageRepositoryImpl(rustMessageDataSource, undoRepository)

    @Test
    fun `getLocalMessages should return list of messages`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val pageKey = PageKey.DefaultPageKey(labelId = SystemLabelId.Archive.labelId)

        val expectedMessages = listOf(
            LocalMessageTestData.AugWeatherForecast,
            LocalMessageTestData.SepWeatherForecast,
            LocalMessageTestData.OctWeatherForecast
        )
        coEvery { rustMessageDataSource.getMessages(userId, any()) } returns expectedMessages.right()

        // When
        val result = repository.getMessages(userId, pageKey)

        // Then
        coVerify { rustMessageDataSource.getMessages(userId, any()) }
        assertEquals(expectedMessages.size, result.getOrNull()!!.size)
        result.getOrNull()!!.forEachIndexed { index, message ->
            assertEquals(expectedMessages[index].subject, message.subject)
        }
    }

    @Test
    fun `observe message should return the local message`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast.toMessageId()
        val expectedMessage = LocalMessageTestData.AugWeatherForecast.toMessage()
        coEvery {
            rustMessageDataSource.observeMessage(userId, messageId.toLocalMessageId())
        } returns flowOf(LocalMessageTestData.AugWeatherForecast.right())

        // When
        repository.observeMessage(userId, messageId).test {
            val result = awaitItem().getOrElse { null }

            // Then
            assertEquals(expectedMessage, result)
            coVerify { rustMessageDataSource.observeMessage(userId, messageId.toLocalMessageId()) }

            awaitComplete()
        }
    }

    @Test
    fun `observe cached message from a remote messageId should return the corresponding local message`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = RemoteMessageIdSample.AugWeatherForecast.toRemoteMessageId()
        val expectedMessage = LocalMessageTestData.AugWeatherForecast.toMessage()
        coEvery {
            rustMessageDataSource.getMessage(userId, messageId)
        } returns LocalMessageTestData.AugWeatherForecast.right()

        // When
        repository.observeMessage(userId, messageId.toRemoteMessageId()).test {
            val result = awaitItem().getOrElse { null }

            // Then
            assertEquals(expectedMessage, result)
            coVerify { rustMessageDataSource.getMessage(userId, messageId) }

            awaitComplete()
        }
    }

    @Test
    fun `observe message should return DataError when no message not found`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast.toMessageId()

        coEvery { rustMessageDataSource.observeMessage(userId, messageId.toLocalMessageId()) } returns
            flowOf(DataError.Local.NoDataCached.left())

        // When
        repository.observeMessage(userId, messageId).test {
            val result = awaitItem()

            // Then
            coVerify { rustMessageDataSource.observeMessage(userId, messageId.toLocalMessageId()) }
            assert(result.isLeft())
            assertEquals(DataError.Local.NoDataCached, result.swap().getOrElse { null })
            awaitComplete()
        }
    }

    @Test
    fun `observe message from a remote messageId should return DataError when no message not found`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = RemoteMessageIdSample.AugWeatherForecast
        coEvery { rustMessageDataSource.getMessage(userId, messageId.toRemoteMessageId()) } returns
            DataError.Local.NoDataCached.left()

        // When
        repository.observeMessage(userId, messageId).test {
            val result = awaitItem()

            // Then
            coVerify { rustMessageDataSource.getMessage(userId, messageId.toRemoteMessageId()) }
            assert(result.isLeft())
            assertEquals(DataError.Local.NoDataCached, result.swap().getOrElse { null })
            awaitComplete()
        }
    }

    @Test
    fun `when getConversationCursor returns a cursor with the first messsageId`() = runTest {
        // Given
        val conversationCursor = mockk<ConversationCursor> {
            every { previousPage() } returns CursorResult.Cursor(ConversationId("200"))
            coEvery { nextPage() } returns CursorResult.Cursor(ConversationId("300"))
        }
        val firstPage = Id(100.toULong())
        coEvery {
            rustMessageDataSource.getConversationCursor(
                firstPage = firstPage,
                userId = userId,
                labelId = labelId
            )
        } returns conversationCursor.right()


        // When
        val result = repository.getConversationCursor(
            firstPage = CursorId(ConversationId("100"), null),
            userId = userId,
            labelId = labelId
        )

        // Then
        assertTrue(result.isRight())
        assertTrue(result.getOrNull() is RustConversationCursorImpl)
        assertEquals("100", (result.getOrNull()?.current as? CursorResult.Cursor)?.conversationId?.id)
    }

    @Test
    fun `markRead should mark conversations as read`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery { rustMessageDataSource.markRead(userId, any()) } returns Unit.right()

        // When
        val result = repository.markRead(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.markRead(userId, messageIds.map { it.toLocalMessageId() }) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `markUnread should mark conversations as unread`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery { rustMessageDataSource.markUnread(userId, any()) } returns Unit.right()

        // When
        val result = repository.markUnread(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.markUnread(userId, messageIds.map { it.toLocalMessageId() }) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `getSenderImage should return sender image when available`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val address = "test@example.com"
        val bimi = "bimiSelector"
        val imagePath = "image.png"
        val expectedSenderImage = SenderImage(File(imagePath))

        coEvery { rustMessageDataSource.getSenderImage(userId, address, bimi) } returns imagePath

        // When
        val result = repository.getSenderImage(userId, address, bimi)

        // Then
        coVerify { rustMessageDataSource.getSenderImage(userId, address, bimi) }
        assertEquals(expectedSenderImage, result)
    }

    @Test
    fun `getSenderImage should return null when sender image is not available`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val address = "test@example.com"
        val bimi = "bimiSelector"

        coEvery { rustMessageDataSource.getSenderImage(userId, address, bimi) } returns null

        // When
        val result = repository.getSenderImage(userId, address, bimi)

        // Then
        coVerify { rustMessageDataSource.getSenderImage(userId, address, bimi) }
        assertNull(result)
    }

    @Test
    fun `should star messages when dataSource call is successful`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery {
            rustMessageDataSource.starMessages(userId, messageIds.map { it.toLocalMessageId() })
        } returns Unit.right()

        // When
        val result = repository.starMessages(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.starMessages(userId, messageIds.map { it.toLocalMessageId() }) }
        assert(result.isRight())
    }

    @Test
    fun `star action should fail and return error when dataSource call fails`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery {
            rustMessageDataSource.starMessages(
                userId,
                messageIds.map {
                    it.toLocalMessageId()
                }
            )
        } returns DataError.Local.CryptoError.left()

        // When
        val result = repository.starMessages(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.starMessages(userId, messageIds.map { it.toLocalMessageId() }) }
        assert(result.isLeft())
        assertEquals(DataError.Local.CryptoError, result.swap().getOrElse { null })
    }

    @Test
    fun `should unstar messages when dataSource call is successful`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery {
            rustMessageDataSource.unStarMessages(userId, messageIds.map { it.toLocalMessageId() })
        } returns Unit.right()

        // When
        val result = repository.unStarMessages(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.unStarMessages(userId, messageIds.map { it.toLocalMessageId() }) }
        assert(result.isRight())
    }

    @Test
    fun `unstar action should fail and return error when dataSource call fails`() = runTest {
        // Given
        val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
        coEvery {
            rustMessageDataSource.unStarMessages(userId, messageIds.map { it.toLocalMessageId() })
        } returns DataError.Local.CryptoError.left()

        // When
        val result = repository.unStarMessages(userId, messageIds)

        // Then
        coVerify { rustMessageDataSource.unStarMessages(userId, messageIds.map { it.toLocalMessageId() }) }
        assert(result.isLeft())
        assertEquals(DataError.Local.CryptoError, result.swap().getOrElse { null })
    }

    @Test
    fun `label should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
            val selectedLabelIds = listOf(LabelIdSample.RustLabel1, LabelIdSample.RustLabel2)
            val partiallySelectedLabelIds = listOf(LabelIdSample.RustLabel3)
            val shouldArchive = false
            val expectedUndoableOperation = mockk<UndoableOperation>()

            coEvery {
                rustMessageDataSource.labelMessages(
                    userId,
                    messageIds.map { it.toLocalMessageId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
            } returns expectedUndoableOperation.right()

            every { undoRepository.setLastOperation(expectedUndoableOperation) } just runs

            // When
            val result = repository.labelAs(
                userId,
                messageIds,
                selectedLabelIds,
                partiallySelectedLabelIds,
                shouldArchive
            )

            // Then
            coVerify {
                rustMessageDataSource.labelMessages(
                    userId,
                    messageIds.map { it.toLocalMessageId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
            }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `label should call rust data source and return error when failing`() {
        runTest {
            // Given
            val messageIds = listOf(LocalMessageIdSample.AugWeatherForecast.toMessageId())
            val selectedLabelIds = listOf(LabelIdSample.RustLabel1, LabelIdSample.RustLabel2)
            val partiallySelectedLabelIds = listOf(LabelIdSample.RustLabel3)
            val shouldArchive = false
            val expectedError = DataError.Local.CryptoError

            coEvery {
                rustMessageDataSource.labelMessages(
                    userId,
                    messageIds.map { it.toLocalMessageId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
            } returns expectedError.left()

            // When
            val result = repository.labelAs(
                userId,
                messageIds,
                selectedLabelIds,
                partiallySelectedLabelIds,
                shouldArchive
            )

            // Then
            assertEquals(expectedError.left(), result)
        }
    }

    @Test
    fun `mark message as legitimate should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val messageId = LocalMessageIdSample.AugWeatherForecast

            coEvery { rustMessageDataSource.markMessageAsLegitimate(userId, messageId) } returns Unit.right()

            // When
            val result = repository.markMessageAsLegitimate(userId, messageId.toMessageId())

            // Then
            coVerify { rustMessageDataSource.markMessageAsLegitimate(userId, messageId) }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `mark message as legitimate should call rust data source and return error when failing`() {
        runTest {
            // Given
            val messageId = LocalMessageIdSample.AugWeatherForecast
            val expectedError = DataError.Local.CryptoError

            coEvery { rustMessageDataSource.markMessageAsLegitimate(userId, messageId) } returns expectedError.left()

            // When
            val result = repository.markMessageAsLegitimate(userId, messageId.toMessageId())

            // Then
            assertEquals(expectedError.left(), result)
        }
    }

    @Test
    fun `unblock sender should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val email = "abc@pm.me"

            coEvery { rustMessageDataSource.unblockSender(userId, email) } returns Unit.right()

            // When
            val result = repository.unblockSender(userId, email)

            // Then
            coVerify { rustMessageDataSource.unblockSender(userId, email) }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `unblock sender should call rust data source and return error when failing`() {
        runTest {
            // Given
            val email = "abc@pm.me"
            val expectedError = DataError.Local.CryptoError

            coEvery { rustMessageDataSource.unblockSender(userId, email) } returns expectedError.left()

            // When
            val result = repository.unblockSender(userId, email)

            // Then
            assertEquals(expectedError.left(), result)
        }
    }

    @Test
    fun `report phishing should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val messageId = LocalMessageIdSample.AugWeatherForecast

            coEvery { rustMessageDataSource.reportPhishing(userId, messageId) } returns Unit.right()

            // When
            val result = repository.reportPhishing(userId, messageId.toMessageId())

            // Then
            coVerify { rustMessageDataSource.reportPhishing(userId, messageId) }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `report phishing should call rust data source and return error when failing`() {
        runTest {
            // Given
            val messageId = LocalMessageIdSample.AugWeatherForecast
            val expectedError = DataError.Local.CryptoError

            coEvery { rustMessageDataSource.reportPhishing(userId, messageId) } returns expectedError.left()

            // When
            val result = repository.reportPhishing(userId, messageId.toMessageId())

            // Then
            assertEquals(expectedError.left(), result)
        }
    }

    @Test
    fun `delete all messages in location should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val labelId = LabelIdSample.Trash

            coEvery {
                rustMessageDataSource.deleteAllMessagesInLocation(userId, labelId.toLocalLabelId())
            } returns Unit.right()

            // When
            val result = repository.deleteAllMessagesInLocation(userId, labelId)

            // Then
            coVerify { rustMessageDataSource.deleteAllMessagesInLocation(userId, labelId.toLocalLabelId()) }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `delete all messages in location should call rust data source and return error when failing`() {
        runTest {
            // Given
            val labelId = LabelIdSample.Trash
            val expectedError = DataError.Local.CryptoError

            coEvery {
                rustMessageDataSource.deleteAllMessagesInLocation(userId, labelId.toLocalLabelId())
            } returns expectedError.left()

            // When
            val result = repository.deleteAllMessagesInLocation(userId, labelId)

            // Then
            assertEquals(expectedError.left(), result)
        }
    }
}
