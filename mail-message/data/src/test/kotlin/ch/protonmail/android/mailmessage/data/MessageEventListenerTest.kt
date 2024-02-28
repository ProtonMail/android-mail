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

import arrow.core.right
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import ch.protonmail.android.mailmessage.data.usecase.GetMessageIdsInDraftState
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.label.data.local.LabelDatabase
import org.junit.Test

class MessageEventListenerTest {

    private val messageResourceList =
        listOf(
            getMessageResource(MessageSample.RemoteDraft.id),
            getMessageResource(MessageSample.AugWeatherForecast.id),
            getMessageResource(MessageSample.SepWeatherForecast.id),
            getMessageResource(MessageSample.OctWeatherForecast.id)
        )
    private val userId = UserId("userId")
    private val messageList = messageResourceList.map { it.toMessage(userId) }

    private val db = mockk<LabelDatabase>()
    private val localDataSource = mockk<MessageLocalDataSource>()
    private val repository = mockk<MessageRepository>()
    private val excludeDraftMessagesAlreadyInOutbox = mockk<ExcludeDraftMessagesAlreadyInOutbox>()
    private val getMessageIdsInDraftState = mockk<GetMessageIdsInDraftState>()
    private val config = EventManagerConfig.Core(userId)

    private val messageEventListener = MessageEventListener(
        db,
        localDataSource,
        repository,
        excludeDraftMessagesAlreadyInOutbox,
        getMessageIdsInDraftState
    )

    private val messagesWithoutDrafts =
        listOf(MessageSample.AugWeatherForecast, MessageSample.SepWeatherForecast, MessageSample.OctWeatherForecast)

    @Test
    fun `should exclude drafts from outbox and upsert messages on create`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(messagesWithoutDrafts) } returns Unit.right()

        // When
        messageEventListener.onCreate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should not call upsert when all messages are draft and excluded`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) } returns emptyList()
        coEvery { localDataSource.upsertMessages(messagesWithoutDrafts) } returns Unit.right()

        // When
        messageEventListener.onCreate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) }
        coVerify(exactly = 0) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should exclude drafts from outbox and upsert messages on update`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(messagesWithoutDrafts) } returns Unit.right()

        // When
        messageEventListener.onUpdate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should exclude drafts from outbox and upsert messages on partial update`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(messagesWithoutDrafts) } returns Unit.right()

        // When
        messageEventListener.onPartial(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, messageList) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should delete messages`() = runTest {
        // Given
        val messagesToDelete = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.SepWeatherForecast)
        coEvery { localDataSource.deleteMessages(userId, messagesToDelete) } returns Unit

        // When
        messageEventListener.onDelete(config, messagesToDelete.map { it.id })

        // Then
        coVerify(exactly = 1) { localDataSource.deleteMessages(userId, messagesToDelete) }
    }

    @Test
    fun `should exclude draft state messages when reset all invokes`() = runTest {
        // Given
        val pageKey = PageKey()
        val messagesExceptDrafts = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.SepWeatherForecast)
        coEvery { getMessageIdsInDraftState.invoke(userId) } returns messagesExceptDrafts
        coEvery { localDataSource.deleteAllMessagesExcept(userId, messagesExceptDrafts) } returns Unit
        coEvery { repository.getRemoteMessages(userId, pageKey) } returns listOf(MessageSample.Invoice).right()

        // When
        messageEventListener.onResetAll(config)

        // Then
        coVerify(exactly = 1) { getMessageIdsInDraftState.invoke(userId) }
        coVerify(exactly = 1) { localDataSource.deleteAllMessagesExcept(userId, messagesExceptDrafts) }
        coVerify(exactly = 1) { repository.getRemoteMessages(userId, pageKey) }
    }
}
