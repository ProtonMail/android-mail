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
import ch.protonmail.android.mailmessage.data.usecase.ExcludeMessagesInDraftState
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    private val db = mockk<LabelDatabase>()
    private val localDataSource = mockk<MessageLocalDataSource>()
    private val repository = mockk<MessageRepository>()
    private val excludeDraftMessagesAlreadyInOutbox = mockk<ExcludeDraftMessagesAlreadyInOutbox>()
    private val excludeMessagesInDraftState = mockk<ExcludeMessagesInDraftState>()
    private val config = mockk<EventManagerConfig>()

    private val messageEventListener = MessageEventListener(
        db,
        localDataSource,
        repository,
        excludeDraftMessagesAlreadyInOutbox,
        excludeMessagesInDraftState
    )

    private val messagesWithoutDrafts =
        listOf(MessageSample.AugWeatherForecast, MessageSample.SepWeatherForecast, MessageSample.OctWeatherForecast)

    @Test
    fun `should exclude drafts from outbox and upsert messages on create`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(any()) } returns Unit.right()
        every { config.userId } returns userId

        // When
        messageEventListener.onCreate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should not call upsert when all messages are draft and excluded`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) } returns emptyList()
        coEvery { localDataSource.upsertMessages(any()) } returns Unit.right()
        every { config.userId } returns userId

        // When
        messageEventListener.onCreate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) }
        coVerify(exactly = 0) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should exclude drafts from outbox and upsert messages on update`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(any()) } returns Unit.right()
        every { config.userId } returns userId

        // When
        messageEventListener.onUpdate(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should exclude drafts from outbox and upsert messages on partial update`() = runTest {
        // Given
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) } returns messagesWithoutDrafts
        coEvery { localDataSource.upsertMessages(any()) } returns Unit.right()
        every { config.userId } returns userId

        // When
        messageEventListener.onPartial(config, messageResourceList)

        // Then
        coVerify(exactly = 1) { excludeDraftMessagesAlreadyInOutbox.invoke(userId, any()) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(messagesWithoutDrafts) }
    }

    @Test
    fun `should delete messages`() = runTest {
        // Given
        val messagesToDelete = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.SepWeatherForecast)
        every { config.userId } returns userId
        coEvery { localDataSource.deleteMessages(userId, any()) } returns Unit

        // When
        messageEventListener.onDelete(config, messagesToDelete.map { it.id })

        // Then
        coVerify(exactly = 1) { localDataSource.deleteMessages(userId, messagesToDelete) }
    }

    @Test
    fun `should exclude draft state messages when reset all invokes`() = runTest {
        // Given
        val messagesExceptDrafts = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.SepWeatherForecast)
        coEvery { excludeMessagesInDraftState.invoke(any()) } returns messagesExceptDrafts
        every { config.userId } returns userId
        coEvery { localDataSource.deleteAllMessagesExcept(userId, messagesExceptDrafts) } returns Unit
        coEvery { repository.getRemoteMessages(userId, any()) } returns listOf(MessageSample.Invoice).right()

        // When
        messageEventListener.onResetAll(config)

        // Then
        coVerify(exactly = 1) { excludeMessagesInDraftState.invoke(userId) }
        coVerify(exactly = 1) { localDataSource.deleteAllMessagesExcept(userId, messagesExceptDrafts) }
        coVerify(exactly = 1) { repository.getRemoteMessages(userId, any()) }
    }
}
