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

package ch.protonmail.android.composer.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toMessageSendingStatus
import ch.protonmail.android.composer.data.usecase.CreateRustDraftSendWatcher
import ch.protonmail.android.composer.data.usecase.RustDeleteDraftSendResult
import ch.protonmail.android.composer.data.usecase.RustMarkDraftSendResultAsSeen
import ch.protonmail.android.composer.data.usecase.RustQueryUnseenDraftSendResults
import ch.protonmail.android.mailcommon.data.mapper.LocalDraftSendResult
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import uniffi.mail_uniffi.DraftSendFailure
import uniffi.mail_uniffi.DraftSendResultCallback
import uniffi.mail_uniffi.DraftSendResultOrigin
import uniffi.mail_uniffi.DraftSendResultWatcher
import uniffi.mail_uniffi.DraftSendStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.mail_uniffi.ProtonError as LocalProtonError

@OptIn(ExperimentalCoroutinesApi::class)
class RustSendingStatusDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userSessionRepository: UserSessionRepository = mockk()
    private val createRustDraftSendWatcher: CreateRustDraftSendWatcher = mockk()
    private val rustQueryUnseenDraftSendResults: RustQueryUnseenDraftSendResults = mockk()
    private val rustDeleteDraftSendResult: RustDeleteDraftSendResult = mockk()
    private val rustMarkDraftSendResultAsSeen: RustMarkDraftSendResultAsSeen = mockk()

    private val dataSource = RustSendingStatusDataSourceImpl(
        userSessionRepository,
        createRustDraftSendWatcher,
        rustQueryUnseenDraftSendResults,
        rustDeleteDraftSendResult,
        rustMarkDraftSendResultAsSeen
    )

    // Mock required objects
    private val draftSendResultWatcher: DraftSendResultWatcher = mockk(relaxed = true)

    private val testLocalMessageId = LocalMessageIdSample.AugWeatherForecast
    private val testMessageId = testLocalMessageId.toMessageId()
    private val testUserId = UserIdSample.Primary
    private val testSession: MailUserSessionWrapper = mockk()

    @Test
    fun `observeMessageSendingStatus returns empty flow when session is null`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns null

        // When
        val result = dataSource.observeMessageSendingStatus(testUserId).toList()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeMessageSendingStatus returns empty flow when watcher creation fails`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { createRustDraftSendWatcher(testSession, any()) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.observeMessageSendingStatus(testUserId).toList()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeMessageSendingStatus emits message sending status when watcher is created successfully`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession

        val testDraftSendResult = LocalDraftSendResult(
            messageId = LocalMessageIdSample.AugWeatherForecast,
            timestamp = System.currentTimeMillis().toULong(),
            error = DraftSendStatus.Failure(DraftSendFailure.Other(LocalProtonError.Network)),
            origin = DraftSendResultOrigin.SEND
        )

        val callbackSlot = slot<DraftSendResultCallback>()
        coEvery {
            createRustDraftSendWatcher(testSession, capture(callbackSlot))
        } answers {
            callbackSlot.captured.onNewSendResult(listOf(testDraftSendResult))
            draftSendResultWatcher.right()
        }
        val collectedStatuses = mutableListOf<MessageSendingStatus>()

        // When
        val flowJob = launch(mainDispatcherRule.testDispatcher) {
            dataSource.observeMessageSendingStatus(testUserId).collect { status ->
                collectedStatuses.add(status)
            }
        }

        // Then
        assertTrue(callbackSlot.isCaptured)
        advanceUntilIdle()
        assertEquals(1, collectedStatuses.size)
        assertEquals(testDraftSendResult.toMessageSendingStatus(), collectedStatuses.first())

        flowJob.cancel()
    }

    @Test
    fun `queryUnseenMessageSendingStatuses returns error when session is null`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns null

        // When
        val result = dataSource.queryUnseenMessageSendingStatuses(testUserId)

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `queryUnseenMessageSendingStatuses returns error when querying fails`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { rustQueryUnseenDraftSendResults(testSession) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.queryUnseenMessageSendingStatuses(testUserId)

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `queryUnseenMessageSendingStatuses returns unseen message statuses when successful`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession

        val unseenResults = listOf(
            LocalDraftSendResult(
                messageId = testLocalMessageId,
                timestamp = System.currentTimeMillis().toULong(),
                error = DraftSendStatus.Failure(DraftSendFailure.Other(LocalProtonError.Network)),
                origin = DraftSendResultOrigin.SEND
            )
        )

        coEvery { rustQueryUnseenDraftSendResults(testSession) } returns unseenResults.right()

        // When
        val result = dataSource.queryUnseenMessageSendingStatuses(testUserId)

        // Then
        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(unseenResults.first().toMessageSendingStatus(), result.getOrNull()?.first())
    }

    @Test
    fun `deleteMessageSendingStatuses returns error when session is null`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns null

        // When
        val result = dataSource.deleteMessageSendingStatuses(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `deleteMessageSendingStatuses returns error when deletion fails`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { rustDeleteDraftSendResult(testSession, any()) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.deleteMessageSendingStatuses(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `deleteMessageSendingStatuses returns success when deletion succeeds`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { rustDeleteDraftSendResult(testSession, any()) } returns Unit.right()

        // When
        val result = dataSource.deleteMessageSendingStatuses(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isRight())
    }

    @Test
    fun `markMessageSendingStatusesAsSeen returns error when session is null`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns null

        // When
        val result = dataSource.markMessageSendingStatusesAsSeen(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `markMessageSendingStatusesAsSeen returns error when marking fails`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { rustMarkDraftSendResultAsSeen(testSession, any()) } returns DataError.Local.CryptoError.left()

        // When
        val result = dataSource.markMessageSendingStatusesAsSeen(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isLeft())
    }

    @Test
    fun `markMessageSendingStatusesAsSeen returns success when marking succeeds`() = runTest {
        // Given
        coEvery { userSessionRepository.getUserSession(testUserId) } returns testSession
        coEvery { rustMarkDraftSendResultAsSeen(testSession, any()) } returns Unit.right()

        // When
        val result = dataSource.markMessageSendingStatusesAsSeen(testUserId, listOf(testMessageId))

        // Then
        assertTrue(result.isRight())
    }
}

