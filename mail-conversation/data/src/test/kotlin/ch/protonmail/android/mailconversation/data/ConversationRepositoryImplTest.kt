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

package ch.protonmail.android.mailconversation.data

import java.util.UUID
import app.cash.turbine.test
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Called
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationRepositoryImplTest {

    private val userId = UserIdSample.Primary
    private val contextLabelId = MailLabelId.System.Inbox.labelId

    private val conversationLocalDataSource = mockk<ConversationLocalDataSource>(relaxUnitFun = true) {
        coEvery { this@mockk.getConversations(any(), any<PageKey>()) } returns emptyList()
        coEvery { this@mockk.isLocalPageValid(any(), any(), any()) } returns false
    }
    private val conversationRemoteDataSource = mockk<ConversationRemoteDataSource>(relaxUnitFun = true) {
        coEvery { this@mockk.getConversations(any(), any()) } returns listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3,
            ConversationWithContextTestData.conversation4
        ).right()
    }

    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val coroutineScopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val messageLocalDataSource: MessageLocalDataSource = mockk(relaxUnitFun = true) {
        coEvery { this@mockk.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )
    }

    private val conversationRepository = ConversationRepositoryImpl(
        conversationLocalDataSource = conversationLocalDataSource,
        conversationRemoteDataSource = conversationRemoteDataSource,
        coroutineScopeProvider = coroutineScopeProvider,
        messageLocalDataSource = messageLocalDataSource
    )

    @Test
    fun `verify conversations are inserted when remote call was successful`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3
        )
        coEvery { conversationLocalDataSource.getClippedPageKey(userId, pageKey) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(userId, pageKey) } returns expected.right()

        // When
        val actual = conversationRepository.getRemoteConversations(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
        coVerify(exactly = 1) { conversationLocalDataSource.upsertConversations(userId, pageKey, expected) }
    }

    @Test
    fun `verify error is returned when remote call fails with Unreachable error`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = DataError.Remote.Http(NetworkError.Unreachable).left()
        coEvery { conversationLocalDataSource.getClippedPageKey(userId, pageKey) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(userId, pageKey) } returns expected

        // When
        val actual = conversationRepository.getRemoteConversations(userId, pageKey)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
        coVerify(exactly = 0) { conversationLocalDataSource.upsertConversations(userId, any(), any()) }
    }

    @Test
    fun `load conversations returns local data`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        coEvery { conversationLocalDataSource.getConversations(userId, pageKey) } returns expected

        // When
        val actual = conversationRepository.getLocalConversations(userId, pageKey)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { conversationLocalDataSource.getConversations(userId, pageKey) }
    }

    @Test
    fun `when clip pageKey returns valid clipped key then remote is called with clipped key`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        val expected = listOf(ConversationWithContextTestData.conversation1)
        coEvery { conversationLocalDataSource.getClippedPageKey(userId, pageKey) } returns clippedPageKey
        coEvery { conversationRemoteDataSource.getConversations(userId, clippedPageKey) } returns expected.right()

        // When
        val actual = conversationRepository.getRemoteConversations(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(expected, actual)
        coVerify(ordering = Ordering.ORDERED) {
            conversationLocalDataSource.getClippedPageKey(userId, pageKey)
            conversationRemoteDataSource.getConversations(userId, clippedPageKey)
        }
    }

    @Test
    fun `when clip pageKey returns null then empty list is returned`() = runTest {
        // Given
        val pageKey = PageKey()
        val conversations = listOf(ConversationWithContextTestData.conversation1)
        coEvery { conversationLocalDataSource.getClippedPageKey(userId, pageKey) } returns null
        coEvery { conversationRemoteDataSource.getConversations(userId, any()) } returns conversations.right()

        // When
        val actual = conversationRepository.getRemoteConversations(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(emptyList(), actual)
        coVerify { conversationLocalDataSource.getClippedPageKey(userId, pageKey) }
        coVerify { conversationRemoteDataSource wasNot Called }
    }

    @Test
    fun `observe conversation emits conversation when existing in cache and observe updates`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val conversationFlow = MutableSharedFlow<Conversation>()
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns conversationFlow
        coEvery { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) } returns
            ConversationWithMessages(conversation = conversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId, refreshData = true).test {
            // Then
            conversationFlow.emit(conversation)
            assertEquals(conversation.right(), awaitItem())

            val updatedConversation = conversation.copy(numUnread = 3)
            conversationFlow.emit(updatedConversation)
            assertEquals(updatedConversation.right(), awaitItem())
        }
    }

    @Test
    fun `observe conversation emits conversation from remote when not present in cache`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(null)
        coEvery { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) } returns
            ConversationWithMessages(conversation = conversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId, refreshData = true).test {
            // Then
            coVerify { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) }
        }
    }

    @Test
    fun `observe conversation always refreshes local cache with the conversation from remote`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val updatedConversation = conversation.copy(numUnread = 5)
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery {
            conversationRemoteDataSource.getConversationWithMessages(
                userId,
                conversationId
            )
        } returns ConversationWithMessages(conversation = updatedConversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId, refreshData = true).test {
            // Then
            coVerify { conversationLocalDataSource.upsertConversation(userId, updatedConversation) }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `add label returns conversations with label when upsert was successful`() = runTest {
        // Given
        val labelId = LabelId("10")
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        coEvery {
            conversationLocalDataSource.addLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.starredConversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = setOf(labelId)
            )
        } returns MessageTestData.unStarredMessagesByConversation.right()

        // When
        val actual = conversationRepository.addLabel(userId, conversationIds, labelId)

        // Then
        assertEquals(listOf(ConversationTestData.starredConversation).right(), actual)
        coVerify { conversationLocalDataSource.addLabels(userId, conversationIds, listOf(labelId)) }
    }

    @Test
    fun `add label to conversations updates remote data source`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelId = LabelId("10")
        coEvery {
            conversationLocalDataSource.addLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = setOf(labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.addLabel(userId, conversationIds, labelId)

        // Then
        coVerify {
            conversationRemoteDataSource.addLabels(userId, conversationIds, listOf(labelId))
        }
    }

    @Test
    fun `add label to messages of conversations`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelId = LabelId("10")
        coEvery {
            conversationLocalDataSource.addLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = setOf(labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.addLabel(userId, conversationIds, labelId)

        // Then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = setOf(labelId)
            )
        }
    }

    @Test
    fun `add multiple labels to messages of conversations`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelIds = listOf(LabelId("10"), LabelId("11"))
        coEvery {
            conversationLocalDataSource.addLabels(userId, conversationIds, labelIds)
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = labelIds.toSet()
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.addLabels(userId, conversationIds, labelIds)

        // Then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = labelIds.toSet()
            )
        }
    }

    @Test
    fun `remove label returns updated conversations without label when upsert was successful`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelId = LabelId("10")
        coEvery {
            conversationLocalDataSource.removeLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = setOf(labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        val actual = conversationRepository.removeLabel(userId, conversationIds, labelId)

        // Then
        val unStaredConversation = ConversationTestData.conversation
        assertEquals(listOf(unStaredConversation).right(), actual)
    }

    @Test
    fun `remove label of messages when removing labels from conversations was successful`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelId = LabelId("10")
        coEvery {
            conversationLocalDataSource.removeLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = setOf(labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.removeLabel(userId, conversationIds, labelId)

        // Then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = setOf(labelId)
            )
        }
    }

    @Test
    fun `remove label from conversations updates remote data source`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelId = LabelId("10")
        coEvery {
            conversationLocalDataSource.removeLabels(userId, conversationIds, listOf(labelId))
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = setOf(labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.removeLabel(userId, conversationIds, labelId)

        // Then
        coVerify {
            conversationRemoteDataSource.removeLabels(userId, conversationIds, listOf(labelId))
        }
    }

    @Test
    fun `remove labels updates remote data source and filters only for affected messages also with unknown labels`() {
        runTest {
            // Given
            val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
            val labelIds = listOf(LabelId("10"), LabelId("11"))
            coEvery {
                conversationLocalDataSource.removeLabels(userId, conversationIds, labelIds)
            } returns listOf(ConversationTestData.conversation).right()
            coEvery {
                messageLocalDataSource.relabelMessagesInConversations(
                    userId = userId,
                    conversationIds = conversationIds,
                    labelIdsToRemove = labelIds.toSet()
                )
            } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

            // When
            conversationRepository.removeLabels(userId, conversationIds, labelIds)

            // Then
            coVerify {
                conversationRemoteDataSource.removeLabels(userId, conversationIds, labelIds)
            }
        }
    }

    @Test
    fun `remove multiple labels from messages of conversations`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelIds = listOf(LabelId("10"), LabelId("11"))
        coEvery {
            conversationLocalDataSource.removeLabels(userId, conversationIds, labelIds)
        } returns listOf(ConversationTestData.conversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = labelIds.toSet()
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        conversationRepository.removeLabels(userId, conversationIds, labelIds)

        // Then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = labelIds.toSet()
            )
        }
    }

    @Test
    fun `move to trash add the trash label to the conversation`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(ConversationLabelSample.WeatherForecast.AllDrafts)
        )
        val trashedConversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.WeatherForecast.AllDrafts,
                ConversationLabelSample.WeatherForecast.Trash
            )
        )
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIds = listOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(trashedConversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        val result = conversationRepository.move(userId, conversationId, emptyList(), SystemLabelId.Trash.labelId)

        // then
        assertEquals(trashedConversation.right(), result)
    }

    @Test
    fun `move to trash add the trash label to every message in the conversation`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationSample.WeatherForecast
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIds = listOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(ConversationSample.WeatherForecast).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        conversationRepository.move(userId, conversationId, emptyList(), SystemLabelId.Trash.labelId)

        // then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        }
    }

    @Test
    fun `move to trash removes all the labels from conversation, except AllMail, AllDraft and AllSent`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast

        val conversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.WeatherForecast.AllDrafts,
                ConversationLabelSample.WeatherForecast.AllMail,
                ConversationLabelSample.WeatherForecast.AllSent,
                ConversationLabelSample.WeatherForecast.Inbox,
                ConversationLabelSample.WeatherForecast.News
            )
        )
        val conversationWithoutLabels = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.WeatherForecast.AllDrafts,
                ConversationLabelSample.WeatherForecast.AllMail,
                ConversationLabelSample.WeatherForecast.AllSent
            )
        )
        val trashedConversation = conversationWithoutLabels.copy(
            labels = conversationWithoutLabels.labels + ConversationLabelSample.WeatherForecast.Trash
        )
        val conversationFlow = MutableStateFlow(conversation)
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns conversationFlow
        coEvery { conversationLocalDataSource.upsertConversation(userId, conversationWithoutLabels) } coAnswers {
            conversationFlow.emit(conversationWithoutLabels)
        }
        coEvery {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIds = listOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(trashedConversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        val result = conversationRepository.move(
            userId,
            conversationId,
            listOf(SystemLabelId.Inbox.labelId),
            SystemLabelId.Trash.labelId
        )

        // then
        assertEquals(trashedConversation.right(), result)
    }

    @Test
    fun `move to spam removes all the labels from conversation, except AllMail, AllDraft and AllSent`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast

        val conversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.WeatherForecast.AllDrafts,
                ConversationLabelSample.WeatherForecast.AllMail,
                ConversationLabelSample.WeatherForecast.AllSent,
                ConversationLabelSample.WeatherForecast.Inbox,
                ConversationLabelSample.WeatherForecast.News
            )
        )
        val conversationWithoutLabels = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.WeatherForecast.AllDrafts,
                ConversationLabelSample.WeatherForecast.AllMail,
                ConversationLabelSample.WeatherForecast.AllSent
            )
        )
        val spammedConversation = conversationWithoutLabels.copy(
            labels = conversationWithoutLabels.labels + ConversationLabelSample.WeatherForecast.Spam
        )
        val conversationFlow = MutableStateFlow(conversation)
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns conversationFlow
        coEvery { conversationLocalDataSource.upsertConversation(userId, conversationWithoutLabels) } coAnswers {
            conversationFlow.emit(conversationWithoutLabels)
        }
        coEvery {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIds = listOf(SystemLabelId.Spam.labelId)
            )
        } returns listOf(spammedConversation).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Spam.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        val result = conversationRepository.move(
            userId,
            conversationId,
            listOf(SystemLabelId.Inbox.labelId),
            SystemLabelId.Spam.labelId
        )

        // then
        assertEquals(spammedConversation.right(), result)
    }

    @Test
    fun `move to trash removes all labels from messages, except AllMail, AllDraft and AllSent`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationSample.WeatherForecast
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIds = listOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(ConversationSample.WeatherForecast).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        conversationRepository.move(
            userId,
            conversationId,
            listOf(SystemLabelId.Inbox.labelId),
            SystemLabelId.Trash.labelId
        )

        // then
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        }
    }

    @Test
    fun `move removes previous exclusive label and adds new label`() = runTest {
        // Given
        val conversation = ConversationTestData.conversation
        val toBeRemovedLabels = listOf(SystemLabelId.Inbox.labelId)
        coEvery { conversationLocalDataSource.observeConversation(userId, conversation.conversationId) } returns flowOf(
            conversation
        )
        coEvery {
            conversationLocalDataSource.removeLabels(
                userId,
                conversation.conversationId,
                toBeRemovedLabels
            )
        } returns conversation.copy(labels = listOf()).right()
        coEvery {
            messageLocalDataSource.removeLabels(
                userId,
                any(),
                toBeRemovedLabels
            )
        } returns MessageTestData.message.right()
        coEvery {
            conversationLocalDataSource.addLabels(
                userId,
                listOf(conversation.conversationId),
                listOf(SystemLabelId.Archive.labelId)
            )
        } returns listOf(ConversationTestData.conversationWithArchiveLabel).right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversation.conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Archive.labelId)
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // When
        val actual = conversationRepository.move(
            userId,
            conversation.conversationId,
            toBeRemovedLabels,
            SystemLabelId.Archive.labelId
        )

        // Then
        coVerify {
            conversationLocalDataSource.addLabels(
                userId = userId,
                conversationIds = listOf(conversation.conversationId),
                labelIds = listOf(SystemLabelId.Archive.labelId)
            )
        }
        coVerify {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = listOf(conversation.conversationId),
                labelIdsToAdd = setOf(SystemLabelId.Archive.labelId)
            )
        }
        assertEquals(ConversationTestData.conversationWithArchiveLabel.right(), actual)
    }

    @Test
    fun `move return error when conversation doesn't exist`() = runTest {
        val conversation = ConversationTestData.conversation
        val toBeRemovedLabels = listOf(SystemLabelId.Inbox.labelId)
        coEvery { conversationLocalDataSource.observeConversation(userId, conversation.conversationId) } returns flowOf(
            null
        )

        val actual = conversationRepository.move(
            userId,
            conversation.conversationId,
            toBeRemovedLabels,
            SystemLabelId.Archive.labelId
        )
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `mark unread returns error when local data source fails`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val error = DataErrorSample.NoCache.left()
        coEvery { conversationLocalDataSource.markUnread(userId, conversationId, contextLabelId) } returns error
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns listOf(MessageSample.build()).right()

        // when
        val result = conversationRepository.markUnread(userId, conversationId, contextLabelId)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `mark unread returns updated conversation when local data source succeeds`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val updatedConversation = ConversationSample.WeatherForecast.right()
        coEvery {
            conversationLocalDataSource.markUnread(
                userId,
                conversationId,
                contextLabelId
            )
        } returns updatedConversation
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns listOf(MessageSample.build()).right()

        // when
        val result = conversationRepository.markUnread(userId, conversationId, contextLabelId)

        // then
        assertEquals(updatedConversation, result)
    }

    @Test
    fun `mark unread marks the most recent read message with the current label as unread`() = runTest {
        // given
        val conversationId = ConversationIdSample.AlphaAppFeedback
        val messages = listOf(
            MessageSample.AlphaAppArchivedFeedback.copy(unread = false),
            MessageSample.AlphaAppInfoRequest.copy(unread = false),
            MessageSample.AlphaAppQAReport.copy(unread = false),
            MessageSample.AlphaAppArchivedFeedback.copy(unread = false),
            MessageSample.AlphaAppArchivedFeedback.copy(unread = true)
        )
        coEvery { conversationLocalDataSource.markUnread(userId, conversationId, contextLabelId) } returns
            ConversationSample.WeatherForecast.right()
        every { messageLocalDataSource.observeMessages(userId, conversationId) } returns flowOf(messages)
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns listOf(MessageSample.build()).right()

        // when
        conversationRepository.markUnread(userId, conversationId, contextLabelId)

        // then
        coVerify { messageLocalDataSource.markUnread(userId, listOf(MessageIdSample.AlphaAppQAReport)) }
    }

    @Test
    fun `mark unread calls conversation remote data source`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        coEvery { conversationLocalDataSource.markUnread(userId, conversationId, contextLabelId) } returns
            ConversationSample.WeatherForecast.right()
        every { messageLocalDataSource.observeMessages(userId, conversationId) } returns flowOf(emptyList())

        // when
        conversationRepository.markUnread(userId, conversationId, contextLabelId)

        // then
        coVerify { conversationRemoteDataSource.markUnread(userId, conversationId, contextLabelId) }
    }

    @Test
    fun `relabel conversations removes and adds provided labels`() = runTest {
        // given
        val conversations = listOf(ConversationTestData.conversationWithConversationLabels)
        val conversationIds = listOf(ConversationId(ConversationTestData.RAW_CONVERSATION_ID))
        val labelsToBeRemoved = listOf(LabelId("1"))
        val labelsToBeAdded = listOf(LabelId("10"), LabelId("11"))
        coEvery {
            conversationLocalDataSource.removeLabels(userId, conversationIds, labelsToBeRemoved)
        } returns conversations.right()
        coEvery {
            conversationLocalDataSource.addLabels(userId, conversationIds, labelsToBeAdded)
        } returns conversations.right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToRemove = labelsToBeRemoved.toSet()
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()
        coEvery {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = labelsToBeAdded.toSet()
            )
        } returns MessageTestData.unStarredMsgByConversationWithStarredMsg.right()

        // when
        conversationRepository.relabel(userId, conversationIds, labelsToBeRemoved, labelsToBeAdded)

        // then
        coVerifyOrder {
            conversationLocalDataSource.removeLabels(userId, conversationIds, labelsToBeRemoved)
            conversationLocalDataSource.addLabels(userId, conversationIds, labelsToBeAdded)
        }
    }

    @Test
    fun `mark read returns error when local data source fails`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val error = DataErrorSample.NoCache.left()
        coEvery { conversationLocalDataSource.markRead(userId, conversationId, contextLabelId) } returns error

        // when
        val result = conversationRepository.markRead(userId, conversationId, contextLabelId)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `mark read returns updated conversation when local data source succeeds`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val updatedConversation = ConversationSample.WeatherForecast.right()
        coEvery {
            conversationLocalDataSource.markRead(userId, conversationId, contextLabelId)
        } returns updatedConversation

        // when
        val result = conversationRepository.markRead(userId, conversationId, contextLabelId)

        // then
        assertEquals(updatedConversation, result)
    }

    @Test
    fun `mark read calls conversation remote data source`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        coEvery { conversationLocalDataSource.markRead(userId, conversationId, contextLabelId) } returns
            ConversationSample.WeatherForecast.right()

        // when
        conversationRepository.markRead(userId, conversationId, contextLabelId)

        // then
        coVerify { conversationRemoteDataSource.markRead(userId, conversationId, contextLabelId) }
    }

    @Test
    fun `observe cache up to date does not refresh the local cache with the conversation from remote`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")

        // When
        conversationRepository.observeConversationCacheUpToDate(userId, conversationId).test {
            // Then
            coVerify(exactly = 0) {
                conversationRemoteDataSource.getConversationWithMessages(userId, conversationId)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `Should return true if the local cached conversation is read`() = runTest {
        // Given
        val conversationId = ConversationId(UUID.randomUUID().toString())
        coEvery { conversationLocalDataSource.isConversationRead(userId, conversationId) } returns true.right()

        // When
        val isConversationRead = conversationRepository.isCachedConversationRead(userId, conversationId).getOrNull()

        // Then
        assertTrue(isConversationRead!!)
    }

    @Test
    fun `Should return false if the local cached conversation is read`() = runTest {
        // Given
        val conversationId = ConversationId(UUID.randomUUID().toString())
        coEvery { conversationLocalDataSource.isConversationRead(userId, conversationId) } returns false.right()

        // When
        val isConversationRead = conversationRepository.isCachedConversationRead(userId, conversationId).getOrNull()

        // Then
        assertFalse(isConversationRead!!)
    }

    @Test
    fun `Should return error if the conversation is not cached`() = runTest {
        // Given
        val conversationId = ConversationId(UUID.randomUUID().toString())
        coEvery { conversationLocalDataSource.isConversationRead(userId, conversationId) } returns
            DataError.Local.NoDataCached.left()

        // When
        val isConversationRead = conversationRepository.isCachedConversationRead(userId, conversationId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), isConversationRead)
    }

    @Test
    fun `returns true when page is valid on local data source`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(ConversationWithContextTestData.conversation1)
        coEvery { conversationLocalDataSource.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val actual = conversationRepository.isLocalPageValid(userId, pageKey, items)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `returns false when page is not valid on local data source`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(ConversationWithContextTestData.conversation1)
        coEvery { conversationLocalDataSource.isLocalPageValid(userId, pageKey, items) } returns false

        // When
        val actual = conversationRepository.isLocalPageValid(userId, pageKey, items)

        // Then
        assertFalse(actual)
    }
}
