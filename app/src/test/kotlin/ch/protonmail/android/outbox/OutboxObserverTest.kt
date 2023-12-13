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

package ch.protonmail.android.outbox

import ch.protonmail.android.initializer.outbox.OutboxObserver
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploadTracker
import ch.protonmail.android.mailmessage.data.usecase.DeleteSentMessagesFromOutbox
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test

@ExperimentalCoroutinesApi
class OutboxObserverTest {

    private val userId = UserIdSample.Primary
    private val unsentDraftItem = DraftState(
        userId = userId, apiMessageId = MessageId("unsentItem01"),
        messageId = MessageIdSample.AugWeatherForecast, state = DraftSyncState.Synchronized,
        action = DraftAction.Compose,
        sendingError = null,
        sendingStatusConfirmed = false
    )
    private val sentDraftItem = DraftState(
        userId = userId, apiMessageId = MessageId("sentItem01"),
        messageId = MessageIdSample.Invoice, state = DraftSyncState.Sent,
        action = DraftAction.Compose,
        sendingError = null,
        sendingStatusConfirmed = false
    )

    private val accountManager = mockk<AccountManager>()
    private val outboxRepository = mockk<OutboxRepository>()
    private val deleteSentMessagesFromOutbox = mockk<DeleteSentMessagesFromOutbox>()
    private val draftUploadTracker = mockk<DraftUploadTracker>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val outboxObserver = OutboxObserver(
        scopeProvider,
        accountManager,
        outboxRepository,
        deleteSentMessagesFromOutbox,
        draftUploadTracker
    )

    @Test
    fun `should not observe messages when userId is null`() = runTest {
        // Given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(null)

        // When
        outboxObserver.start()

        // Then
        coVerify(exactly = 0) { outboxRepository.observeAll(any()) }
    }

    @Test
    fun `should not call delete sent outbox messages when there are no outbox messages`() = runTest {
        // Given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { outboxRepository.observeAll(any()) } returns flowOf(emptyList())

        // When
        outboxObserver.start()

        // Then
        coVerify(exactly = 0) { deleteSentMessagesFromOutbox(userId, any()) }
        verify(exactly = 0) { draftUploadTracker.notifySentMessages(any()) }
    }

    @Test
    fun `should call delete for sent outbox messages and notify draft upload tracker`() = runTest {
        // Given
        val outboxDraftItems = flowOf(listOf(unsentDraftItem, sentDraftItem))
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { outboxRepository.observeAll(userId) } returns outboxDraftItems
        every { draftUploadTracker.notifySentMessages(any()) } returns Unit

        // When
        outboxObserver.start()

        // Then
        verify(exactly = 1) { draftUploadTracker.notifySentMessages(setOf(sentDraftItem.messageId)) }
        coVerify(exactly = 1) { deleteSentMessagesFromOutbox(userId, listOf(sentDraftItem)) }
        coVerify(exactly = 0) { deleteSentMessagesFromOutbox(userId, listOf(unsentDraftItem)) }
    }
}
