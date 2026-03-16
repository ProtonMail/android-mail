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

package ch.protonmail.android.mailconversation.data.repository

import app.cash.turbine.test
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
import ch.protonmail.android.mailconversation.data.local.RustConversationDataSource
import ch.protonmail.android.mailconversation.data.mapper.toConversation
import ch.protonmail.android.mailconversation.data.mapper.toConversationMessagesWithMessageToOpen
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.LabelWithSystemLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.mailmessage.data.mapper.toConversationId
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import ch.protonmail.android.mailmessage.data.model.LocalConversationWithMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.rust.LocalConversationIdSample
import ch.protonmail.android.testdata.conversation.rust.LocalConversationTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import ch.protonmail.android.testdata.message.rust.LocalMessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert
import org.junit.Test
import uniffi.proton_mail_uniffi.Id
import kotlin.test.assertEquals

internal class RustConversationRepositoryImplTest {

    private val rustConversationDataSource = mockk<RustConversationDataSource>()
    private val undoRepository = mockk<UndoRepository>()

    private val userId = UserId("test_user")
    private val labelWithSystemLabelId = LabelWithSystemLabelId(
        LabelTestData.systemLabel, systemLabelId = SystemLabelId.Archive
    )
    private val rustConversationRepository = RustConversationRepositoryImpl(
        rustConversationDataSource,
        undoRepository
    )

    @Test
    fun `getConversations should return conversations`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = labelWithSystemLabelId.label.labelId)
        val localConversations = listOf(
            LocalConversationTestData.AugConversation, LocalConversationTestData.SepConversation
        )
        val expectedConversations = localConversations.map { it.toConversation() }
        coEvery { rustConversationDataSource.getConversations(userId, any()) } returns localConversations.right()

        // When
        val result = rustConversationRepository.getConversations(userId, pageKey)

        // Then
        coVerify { rustConversationDataSource.getConversations(userId, any()) }
        assertEquals(expectedConversations.right(), result)
    }

    @Test
    fun `observeConversation should return the conversation for the given id`() = runTest {
        // Given
        val conversationId = LocalConversationIdSample.AugConversation.toConversationId()
        val localConversation = LocalConversationTestData.AugConversation
        val expected = localConversation.toConversation()
        val labelId = LabelId("2")
        val entryPoint = ConversationDetailEntryPoint.Mailbox
        val showAll = false
        coEvery {
            rustConversationDataSource.observeConversationWithMessages(
                userId,
                any(),
                labelId.toLocalLabelId(),
                entryPoint,
                showAll
            )
        } returns flowOf(
            LocalConversationWithMessages(conversation = localConversation, messages = mockk()).right()
        )

        // When
        rustConversationRepository.observeConversation(userId, conversationId, labelId, entryPoint, showAll).test {
            val result = awaitItem().getOrNull()

            // Then
            assertEquals(expected, result)
            coVerify {
                rustConversationDataSource.observeConversationWithMessages(
                    userId,
                    any(),
                    labelId.toLocalLabelId(),
                    entryPoint,
                    showAll
                )
            }

            awaitComplete()
        }

    }

    @Test
    fun `observeConversation should return error when rust provides no conversation `() = runTest {
        // Given
        val conversationId = LocalConversationIdSample.AugConversation.toConversationId()
        val labelId = LabelId("2")
        val entryPoint = ConversationDetailEntryPoint.Mailbox
        val showAll = false

        coEvery {
            rustConversationDataSource.observeConversationWithMessages(
                userId,
                any(),
                labelId.toLocalLabelId(),
                entryPoint,
                showAll
            )
        } returns
            flowOf(ConversationError.NullValueReturned.left())

        // When
        rustConversationRepository.observeConversation(userId, conversationId, labelId, entryPoint, showAll).test {
            val result = awaitItem().getOrNull()

            // Then
            assertEquals(null, result)
            coVerify {
                rustConversationDataSource.observeConversationWithMessages(
                    userId,
                    any(),
                    labelId.toLocalLabelId(),
                    entryPoint,
                    showAll
                )
            }

            awaitComplete()
        }
    }

    @Test
    fun `observeConversationMessages should return list of messages`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val conversationId = LocalConversationIdSample.AugConversation.toConversationId()
        val localMessages = listOf(
            LocalMessageTestData.AugWeatherForecast,
            LocalMessageTestData.SepWeatherForecast,
            LocalMessageTestData.OctWeatherForecast
        )
        val localConversationMessages = LocalConversationMessages(
            messageIdToOpen = LocalMessageIdSample.AugWeatherForecast,
            messages = localMessages
        )
        val expectedConversationMessages = localConversationMessages.toConversationMessagesWithMessageToOpen()
        val labelId = LabelId("2")
        val entryPoint = ConversationDetailEntryPoint.Mailbox
        val showAll = false

        coEvery {
            rustConversationDataSource.observeConversationWithMessages(
                userId,
                conversationId.toLocalConversationId(),
                labelId.toLocalLabelId(),
                entryPoint,
                showAll
            )
        } returns flowOf(
            LocalConversationWithMessages(conversation = mockk(), messages = localConversationMessages).right()
        )

        // When
        rustConversationRepository.observeConversationMessages(userId, conversationId, labelId, entryPoint, showAll)
            .test {
                val result = awaitItem()

                // Then
                assertEquals(expectedConversationMessages, result)
                coVerify {
                    rustConversationDataSource.observeConversationWithMessages(
                        userId,
                        conversationId.toLocalConversationId(),
                        labelId.toLocalLabelId(),
                        entryPoint,
                        showAll
                    )
                }
                awaitComplete()
            }
    }

    @Test
    fun `when getConversationCursor returns a cursor with the first conversationId`() = runTest {
        // Given
        val conversationCursor = mockk<ConversationCursor> {
            every { previousPage() } returns CursorResult.Cursor(ConversationId("200"))
            coEvery { nextPage() } returns CursorResult.Cursor(ConversationId("300"))
        }
        val firstPage = Id(100.toULong())
        val labelId = LabelId("2")
        coEvery {
            rustConversationDataSource.getConversationCursor(
                firstPage = firstPage,
                userId = userId,
                labelId = labelId
            )
        } returns conversationCursor.right()


        // When
        val result = rustConversationRepository.getConversationCursor(
            firstPage = CursorId(ConversationId("100"), null),
            userId = userId,
            labelId = labelId
        )

        // Then
        Assert.assertTrue(result.isRight())
        Assert.assertTrue(result.getOrNull() is RustConversationCursorImpl)
        Assert.assertEquals(
            ConversationId("100"),
            (result.getOrNull()?.current as? CursorResult.Cursor)?.conversationId
        )
    }

    @Test
    fun `when unreadFilterEnabled is true then getConversationCursor returns a cursor for unread messages`() = runTest {
        // Given
        val conversationCursor = mockk<ConversationCursor> {
            every { previousPage() } returns CursorResult.Cursor(ConversationId("200"))
            coEvery { nextPage() } returns CursorResult.Cursor(ConversationId("300"))
        }
        val firstPage = Id(100.toULong())
        val labelId = LabelId("1")
        coEvery {
            rustConversationDataSource.getConversationCursor(
                firstPage = firstPage,
                userId = userId,
                labelId = labelId
            )
        } returns conversationCursor.right()

        // When
        rustConversationRepository.getConversationCursor(
            firstPage = CursorId(ConversationId("100"), null),
            userId = userId,
            labelId = labelId
        )

        // Then
        coVerify(exactly = 1) {
            rustConversationDataSource.getConversationCursor(
                firstPage = firstPage,
                userId = userId,
                labelId = labelId
            )
        }
    }

    @Test
    fun `markRead should mark conversations as read`() = runTest {
        // Given
        val conversationIds = listOf(LocalConversationIdSample.AugConversation.toConversationId())
        val labelId = LabelIdSample.AllMail
        coEvery { rustConversationDataSource.markRead(userId, labelId.toLocalLabelId(), any()) } just Runs

        // When
        val result = rustConversationRepository.markRead(userId, labelId, conversationIds)

        // Then
        coVerify {
            rustConversationDataSource.markRead(
                userId,
                labelId.toLocalLabelId(),
                conversationIds.map { it.toLocalConversationId() }
            )
        }
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `markUnread should mark conversations as unread`() = runTest {
        // Given
        val conversationIds = listOf(LocalConversationIdSample.AugConversation.toConversationId())
        val labelId = LabelIdSample.AllMail
        coEvery { rustConversationDataSource.markUnread(userId, labelId.toLocalLabelId(), any()) } just Runs

        // When
        val result = rustConversationRepository.markUnread(userId, labelId, conversationIds)

        // Then
        coVerify {
            rustConversationDataSource.markUnread(
                userId,
                labelId.toLocalLabelId(),
                conversationIds.map { it.toLocalConversationId() }
            )
        }
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun `should star conversations`() = runTest {
        // Given
        val conversationIds = listOf(
            LocalConversationIdSample.AugConversation.toConversationId(),
            LocalConversationIdSample.SepConversation.toConversationId()
        )
        coEvery { rustConversationDataSource.starConversations(userId, any()) } returns Unit

        // When
        val result = rustConversationRepository.star(userId, conversationIds)

        // Then
        coVerify {
            rustConversationDataSource.starConversations(
                userId,
                conversationIds.map {
                    it.toLocalConversationId()
                }
            )
        }
        assertEquals(emptyList<Conversation>().right(), result)
    }

    @Test
    fun `should unStar conversations`() = runTest {
        // Given
        val conversationIds = listOf(
            LocalConversationIdSample.AugConversation.toConversationId(),
            LocalConversationIdSample.SepConversation.toConversationId()
        )
        coEvery { rustConversationDataSource.unStarConversations(userId, any()) } returns Unit

        // When
        val result = rustConversationRepository.unStar(userId, conversationIds)

        // Then
        coVerify {
            rustConversationDataSource.unStarConversations(
                userId,
                conversationIds.map {
                    it.toLocalConversationId()
                }
            )
        }
        assertEquals(emptyList<Conversation>().right(), result)
    }

    @Test
    fun `move should call rust data source function and return empty list when successful`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId("1"), ConversationId("2"))
        val toLabelId = LabelIdSample.Trash
        val expectedUndoableOperation = mockk<UndoableOperation>()

        coEvery {
            rustConversationDataSource.moveConversations(
                userId,
                conversationIds.map { it.toLocalConversationId() },
                toLabelId.toLocalLabelId()
            )
        } returns expectedUndoableOperation.right()

        every { undoRepository.setLastOperation(expectedUndoableOperation) } just runs

        // When
        val result = rustConversationRepository.move(userId, conversationIds, toLabelId)

        // Then
        coVerify {
            rustConversationDataSource.moveConversations(
                userId,
                conversationIds.map { it.toLocalConversationId() },
                toLabelId.toLocalLabelId()
            )
            undoRepository.setLastOperation(expectedUndoableOperation)
        }
        assertEquals(emptyList<Conversation>().right(), result)
    }

    @Test
    fun `label should call rust data source and return unit when successful`() {
        runTest {
            // Given
            val conversationIds = listOf(ConversationId("1"), ConversationId("2"))
            val selectedLabelIds = listOf(LabelIdSample.RustLabel1, LabelIdSample.RustLabel2)
            val partiallySelectedLabelIds = listOf(LabelIdSample.RustLabel3)
            val shouldArchive = false
            val expectedUndoableOperation = mockk<UndoableOperation>()

            coEvery {
                rustConversationDataSource.labelConversations(
                    userId,
                    conversationIds.map { it.toLocalConversationId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
            } returns expectedUndoableOperation.right()

            every { undoRepository.setLastOperation(expectedUndoableOperation) } just runs

            // When
            val result = rustConversationRepository.labelAs(
                userId,
                conversationIds,
                selectedLabelIds,
                partiallySelectedLabelIds,
                shouldArchive
            )

            // Then
            coVerify {
                rustConversationDataSource.labelConversations(
                    userId,
                    conversationIds.map { it.toLocalConversationId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
                undoRepository.setLastOperation(expectedUndoableOperation)
            }
            assertEquals(Unit.right(), result)
        }
    }

    @Test
    fun `label should call rust data source and return error when failing`() {
        runTest {
            // Given
            val conversationIds = listOf(ConversationId("1"), ConversationId("2"))
            val selectedLabelIds = listOf(LabelIdSample.RustLabel1, LabelIdSample.RustLabel2)
            val partiallySelectedLabelIds = listOf(LabelIdSample.RustLabel3)
            val shouldArchive = false
            val expectedError = DataError.Local.CryptoError

            coEvery {
                rustConversationDataSource.labelConversations(
                    userId,
                    conversationIds.map { it.toLocalConversationId() },
                    selectedLabelIds.map { it.toLocalLabelId() },
                    partiallySelectedLabelIds.map { it.toLocalLabelId() },
                    shouldArchive
                )
            } returns expectedError.left()

            // When
            val result = rustConversationRepository.labelAs(
                userId,
                conversationIds,
                selectedLabelIds,
                partiallySelectedLabelIds,
                shouldArchive
            )

            // Then
            assertEquals(expectedError.left(), result)
        }
    }

}
