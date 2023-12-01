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

import arrow.core.raise.either
import ch.protonmail.android.initializer.outbox.OutboxObserver
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.usecase.DeleteSentMessagesFromOutbox
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.repository.OutboxRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import kotlin.test.Test

@ExperimentalCoroutinesApi
class OutboxObserverTest {

    private val userId = UserIdSample.Primary
    private val outboxMessages = listOf(
        MessageIdSample.Invoice,
        MessageIdSample.SepWeatherForecast
    )

    private val accountManager = mockk<AccountManager>()
    private val messageRepository = mockk<MessageRepository>()
    private val outboxRepository = mockk<OutboxRepository>()
    private val deleteSentMessagesFromOutbox = mockk<DeleteSentMessagesFromOutbox>()
    private val scopeProvider = TestCoroutineScopeProvider()

    private val outboxObserver = OutboxObserver(
        scopeProvider,
        accountManager,
        messageRepository,
        outboxRepository,
        deleteSentMessagesFromOutbox
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
    fun `should not observe messages when outbox messages are empty`() = runTest {
        // Given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { outboxRepository.observeAll(userId) } returns flowOf(emptyList())

        // When
        outboxObserver.start()

        // Then
        coVerify(exactly = 0) { outboxRepository.observeAll(userId) }
    }

    @Test
    fun `should not delete sent outbox messages when there are no corresponding repository messages`() = runTest {
        // Given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)
        coEvery { outboxRepository.observeAll(userId) } returns flowOf(outboxMessages)
        coEvery { messageRepository.observeCachedMessages(userId, outboxMessages) } returns
            flowOf(either { emptyList() })

        // When
        outboxObserver.start()

        // Then
        coVerify(exactly = 0) { deleteSentMessagesFromOutbox(userId, any()) }
    }
}
