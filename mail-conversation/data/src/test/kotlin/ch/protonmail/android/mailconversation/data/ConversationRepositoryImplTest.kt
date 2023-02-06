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

import app.cash.turbine.test
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
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

class ConversationRepositoryImplTest {

    private val userId = UserIdSample.Primary
    private val contextLabelId = MailLabelId.System.Inbox.labelId

    private val conversationLocalDataSource = mockk<ConversationLocalDataSource>(relaxUnitFun = true) {
        coEvery { this@mockk.getConversations(any(), any()) } returns emptyList()
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
    fun `return remote if local page is invalid`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            ConversationWithContextTestData.conversation1
        )
        val remote = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3
        )
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns remote.right()

        // When
        val result = conversationRepository.getConversations(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
        coVerify(exactly = 1) { conversationLocalDataSource.upsertConversations(userId, pageKey, remote) }
    }

    @Test
    fun `return cached data if remote fails`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        val error = DataErrorSample.Unreachable.left()
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns error

        // When
        val result = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(local.right(), result)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
    }

    @Test
    fun `return local if valid`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        val remote = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3
        )
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns remote.right()

        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(2, conversations.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 0) { conversationRemoteDataSource.getConversations(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns emptyList()
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns
            emptyList<ConversationWithContext>().right()

        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(0, conversations.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            conversationLocalDataSource.getClippedPageKey(userId, pageKey)
            conversationRemoteDataSource.getConversations(userId, clippedPageKey)
        }
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
        conversationRepository.observeConversation(userId, conversationId).test {
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
        conversationRepository.observeConversation(userId, conversationId).test {
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
        conversationRepository.observeConversation(userId, conversationId).test {
            // Then
            coVerify { conversationLocalDataSource.upsertConversation(userId, updatedConversation) }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `add label returns conversation with label when upsert was successful`() = runTest {
        // Given
        coEvery { conversationLocalDataSource.addLabel(any(), any(), any()) } returns
            ConversationTestData.starredConversation.right()

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )
        val labelId = LabelId("10")
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)

        // When
        val actual = conversationRepository.addLabel(userId, conversationId, labelId)

        // Then
        assertEquals(ConversationTestData.starredConversation.right(), actual)
        coVerify { conversationLocalDataSource.addLabel(userId, conversationId, labelId) }
    }

    @Test
    fun `add label to conversation updates remote data source and filters only for affected messages`() = runTest {
        // Given
        coEvery { conversationLocalDataSource.addLabel(any(), any(), any()) } returns
            ConversationTestData.conversation.right()

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMsgByConversationWithStarredMsg
        )
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)

        // When
        conversationRepository.addLabel(userId, conversationId, LabelId("10"))

        // Then
        coVerify {
            conversationRemoteDataSource.addLabel(
                userId,
                conversationId,
                LabelId("10"),
                listOf(MessageId("123"), MessageId("124"))
            )
        }
    }

    @Test
    fun `add label to stored messages of conversation`() = runTest {
        // Given
        coEvery { conversationLocalDataSource.addLabel(any(), any(), any()) } returns
            ConversationTestData.starredConversation.right()

        // When
        conversationRepository.addLabel(
            userId,
            ConversationId(ConversationTestData.RAW_CONVERSATION_ID),
            LabelId("10")
        )

        // Then
        coVerify { messageLocalDataSource.upsertMessages(MessageTestData.starredMessagesByConversation) }
    }

    @Test
    fun `add label conversation even if no messages are stored`() = runTest {
        // Given
        coEvery {
            conversationLocalDataSource.addLabel(
                any(),
                any(),
                any()
            )
        } returns ConversationTestData.starredConversation.right()

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            listOf()
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        assertEquals(ConversationTestData.starredConversation.right(), actual)
    }

    @Test
    fun `add label to messages of a conversation`() = runTest {
        // Given
        coEvery {
            conversationLocalDataSource.addLabel(
                any(),
                any(),
                any()
            )
        } returns ConversationTestData.conversation.right()

        every { messageLocalDataSource.observeMessages(userId, any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )

        // When
        conversationRepository.addLabel(userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10"))

        // Then
        val expectedMessage = MessageTestData.starredMessagesByConversation
        coVerify { messageLocalDataSource.upsertMessages(expectedMessage) }
    }

    @Test
    fun `remove label returns updated conversation without label when upsert was successful`() = runTest {
        // Given
        coEvery { conversationLocalDataSource.removeLabel(any(), any(), any()) } returns
            ConversationTestData.conversation.right()

        // When
        val actual = conversationRepository.removeLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )
        // Then
        val unStaredConversation = ConversationTestData.conversation
        assertEquals(unStaredConversation.right(), actual)
    }

    @Test
    fun `remove label of messages when removing labels from conversation was successful`() = runTest {
        coEvery { conversationLocalDataSource.removeLabel(any(), any(), any()) } returns
            ConversationTestData.conversation.right()

        every { messageLocalDataSource.observeMessages(userId, any<ConversationId>()) } returns flowOf(
            MessageTestData.starredMessagesByConversation
        )
        // When
        conversationRepository.removeLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )
        // Then
        val expectedMessages = MessageTestData.unStarredMessagesByConversation
        coVerify { messageLocalDataSource.upsertMessages(expectedMessages) }
    }

    @Test
    fun `remove label from conversation updates remote data source and filters only for affected messages`() = runTest {
        // Given
        coEvery { conversationLocalDataSource.removeLabel(any(), any(), any()) } returns
            ConversationTestData.conversation.right()

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.starredMsgByConversationWithUnStarredMsg
        )
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)

        // When
        conversationRepository.removeLabel(userId, conversationId, LabelId("10"))

        // Then
        coVerify {
            conversationRemoteDataSource.removeLabel(
                userId,
                conversationId,
                LabelId("10"),
                listOf(MessageId("123"), MessageId("124"))
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
        coEvery { conversationLocalDataSource.addLabel(userId, conversationId, SystemLabelId.Trash.labelId) } returns
            trashedConversation.right()
        coEvery { messageLocalDataSource.addLabel(userId, any(), SystemLabelId.Trash.labelId) } returns
            MessageSample.AugWeatherForecast.right()

        // when
        val result = conversationRepository.move(userId, conversationId, null, SystemLabelId.Trash.labelId)

        // then
        assertEquals(trashedConversation.right(), result)
    }

    @Test
    fun `move to trash add the trash label to every message in the conversation`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationSample.WeatherForecast
        val messages = listOf(
            MessageSample.AugWeatherForecast.copy(
                labelIds = listOf(SystemLabelId.AllDrafts.labelId)
            ),
            MessageSample.SepWeatherForecast.copy(
                labelIds = listOf(SystemLabelId.AllDrafts.labelId)
            )
        )
        val trashedMessages = messages.map { message ->
            message.copy(labelIds = listOf(SystemLabelId.AllDrafts.labelId, SystemLabelId.Trash.labelId))
        }
        val messagesFlow = MutableStateFlow(messages)
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery { conversationLocalDataSource.addLabel(userId, conversationId, SystemLabelId.Trash.labelId) } returns
            ConversationSample.WeatherForecast.right()
        every { messageLocalDataSource.observeMessages(userId, conversationId) } returns messagesFlow
        coEvery { messageLocalDataSource.upsertMessages(trashedMessages) } coAnswers {
            messagesFlow.emit(trashedMessages)
        }

        // when
        conversationRepository.move(userId, conversationId, null, SystemLabelId.Trash.labelId)
        messageLocalDataSource.observeMessages(userId, conversationId).test {

            // then
            assertEquals(trashedMessages, awaitItem())
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
        coEvery { conversationLocalDataSource.addLabel(userId, conversationId, SystemLabelId.Trash.labelId) } returns
            trashedConversation.right()
        coEvery { messageLocalDataSource.addLabel(userId, any(), SystemLabelId.Trash.labelId) } returns
            MessageSample.AugWeatherForecast.right()

        // when
        val result = conversationRepository.move(
            userId,
            conversationId,
            SystemLabelId.Inbox.labelId,
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
        coEvery { conversationLocalDataSource.addLabel(userId, conversationId, SystemLabelId.Spam.labelId) } returns
            spammedConversation.right()
        coEvery { messageLocalDataSource.addLabel(userId, any(), SystemLabelId.Spam.labelId) } returns
            MessageSample.AugWeatherForecast.right()

        // when
        val result = conversationRepository.move(
            userId,
            conversationId,
            SystemLabelId.Inbox.labelId,
            SystemLabelId.Spam.labelId
        )

        // then
        assertEquals(spammedConversation.right(), result)
    }

    @Test
    fun `move to trash removes all the labels from messages, except AllMail, AllDraft and AllSent`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationSample.WeatherForecast
        val messages = listOf(
            MessageSample.AugWeatherForecast.copy(
                labelIds = listOf(
                    SystemLabelId.AllMail.labelId,
                    SystemLabelId.AllSent.labelId,
                    SystemLabelId.Inbox.labelId,
                    LabelIdSample.News
                )
            ),
            MessageSample.SepWeatherForecast.copy(
                labelIds = listOf(
                    SystemLabelId.AllDrafts.labelId,
                    SystemLabelId.AllMail.labelId,
                    SystemLabelId.Inbox.labelId,
                    LabelIdSample.News
                )
            )
        )
        val messagesWithoutLabels = listOf(
            MessageSample.AugWeatherForecast.copy(
                labelIds = listOf(
                    SystemLabelId.AllMail.labelId,
                    SystemLabelId.AllSent.labelId
                )
            ),
            MessageSample.SepWeatherForecast.copy(
                labelIds = listOf(
                    SystemLabelId.AllDrafts.labelId,
                    SystemLabelId.AllMail.labelId
                )
            )
        )
        val trashedMessages = messagesWithoutLabels.map { message ->
            message.copy(labelIds = message.labelIds + SystemLabelId.Trash.labelId)
        }
        val messagesFlow = MutableStateFlow(messages)
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery { conversationLocalDataSource.addLabel(userId, conversationId, SystemLabelId.Trash.labelId) } returns
            ConversationSample.WeatherForecast.right()
        every { messageLocalDataSource.observeMessages(userId, conversationId) } returns messagesFlow
        coEvery { messageLocalDataSource.upsertMessages(messagesWithoutLabels) } coAnswers {
            messagesFlow.emit(messagesWithoutLabels)
        }
        coEvery { messageLocalDataSource.upsertMessages(trashedMessages) } coAnswers {
            messagesFlow.emit(trashedMessages)
        }

        // when
        conversationRepository.move(userId, conversationId, SystemLabelId.Inbox.labelId, SystemLabelId.Trash.labelId)
        messageLocalDataSource.observeMessages(userId, conversationId).test {

            // then
            assertEquals(trashedMessages, awaitItem())
        }
    }

    @Test
    fun `move removes previous exclusive label and adds new label`() = runTest {
        val conversation = ConversationTestData.conversation
        val updatedMessages = MessageTestData.unStarredMessagesByConversation.map {
            it.copy(labelIds = it.labelIds + SystemLabelId.Archive.labelId)
        }

        coEvery { conversationLocalDataSource.observeConversation(userId, conversation.conversationId) } returns flowOf(
            conversation
        )
        coEvery {
            conversationLocalDataSource.removeLabel(
                userId,
                conversation.conversationId,
                SystemLabelId.Inbox.labelId
            )
        } returns conversation.copy(labels = listOf()).right()
        coEvery {
            conversationLocalDataSource.addLabel(
                userId,
                conversation.conversationId,
                SystemLabelId.Archive.labelId
            )
        } returns ConversationTestData.conversationWithArchiveLabel.right()

        val actual = conversationRepository.move(
            userId,
            conversation.conversationId,
            SystemLabelId.Inbox.labelId,
            SystemLabelId.Archive.labelId
        )

        coVerify {
            conversationLocalDataSource.addLabel(userId, conversation.conversationId, SystemLabelId.Archive.labelId)
        }

        coVerify { messageLocalDataSource.upsertMessages(updatedMessages) }
        assertEquals(ConversationTestData.conversationWithArchiveLabel.right(), actual)
    }

    @Test
    fun `move return error when conversation doesn't exist`() = runTest {
        val conversation = ConversationTestData.conversation
        coEvery { conversationLocalDataSource.observeConversation(userId, conversation.conversationId) } returns flowOf(
            null
        )
        val actual = conversationRepository.move(
            userId,
            conversation.conversationId,
            SystemLabelId.Inbox.labelId,
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
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns MessageSample.build().right()

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
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns MessageSample.build().right()

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
        coEvery { messageLocalDataSource.markUnread(userId, any()) } returns MessageSample.build().right()

        // when
        conversationRepository.markUnread(userId, conversationId, contextLabelId)

        // then
        coVerify { messageLocalDataSource.markUnread(userId, MessageIdSample.AlphaAppQAReport) }
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
}
