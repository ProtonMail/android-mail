/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.eventloop

import ch.protonmail.android.mailsession.domain.eventloop.EventLoopErrorSignal
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import uniffi.mail_uniffi.EventError
import uniffi.mail_uniffi.EventErrorReason
import uniffi.mail_uniffi.EventLoopErrorObserver
import uniffi.mail_uniffi.EventLoopErrorObserverHandle
import uniffi.mail_uniffi.MailUserSessionObserveEventLoopErrorsResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class RustEventLoopErrorObserverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userSessionRepository: UserSessionRepository
    private lateinit var eventLoopErrorFlow: EventLoopErrorSignal
    private lateinit var observer: RustEventLoopErrorObserver

    @BeforeTest
    fun setup() {
        userSessionRepository = mockk(relaxed = true)
        eventLoopErrorFlow = mockk(relaxed = true)

        observer = RustEventLoopErrorObserver(
            userSessionRepository,
            eventLoopErrorFlow,
            mainDispatcherRule.testDispatcher
        )
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `start observes primary user id and registers observer when user session exists`() = runTest {
        // Given
        val userIdFlow = MutableSharedFlow<UserId?>()
        val mockUserSession = mockk<MailUserSessionWrapper>()
        val mockHandle = mockk<EventLoopErrorObserverHandle>(relaxed = true)
        val callbackSlot = slot<EventLoopErrorObserver>()

        every { userSessionRepository.observePrimaryUserId() } returns userIdFlow
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        every { mockUserSession.observeEventLoopErrors(capture(callbackSlot)) } returns
            MailUserSessionObserveEventLoopErrorsResult.Ok(mockHandle)

        // When
        observer.start()
        userIdFlow.emit(userId)
        advanceUntilIdle()

        // Then
        assert(callbackSlot.isCaptured)
        coVerify { userSessionRepository.getUserSession(userId) }
        confirmVerified(eventLoopErrorFlow)
    }

    @Test
    fun `start handles null user session gracefully`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        // When
        observer.start()
        advanceUntilIdle()

        // Then
        coVerifySequence {
            userSessionRepository.observePrimaryUserId()
            userSessionRepository.getUserSession(userId)
        }
        confirmVerified(eventLoopErrorFlow, userSessionRepository)
    }

    @Test
    fun `start handles error result when observing event loop`() = runTest {
        // Given
        val mockUserSession = mockk<MailUserSessionWrapper>()
        val error = EventError.Reason(EventErrorReason.REFRESH)

        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        every { mockUserSession.observeEventLoopErrors(any()) } returns
            MailUserSessionObserveEventLoopErrorsResult.Error(error)

        // When
        observer.start()
        advanceUntilIdle()

        // Then
        verify(exactly = 1) { mockUserSession.observeEventLoopErrors(any()) }
        confirmVerified(mockUserSession, eventLoopErrorFlow)
    }

    @Test
    fun `start cleans up previous handle when user changes`() = runTest {
        // Given
        val userIdFlow = MutableSharedFlow<UserId?>()
        val mockHandle1 = mockk<EventLoopErrorObserverHandle>(relaxed = true)
        val mockHandle2 = mockk<EventLoopErrorObserverHandle>(relaxed = true)

        setupMockUserSession(userId, mockHandle1)
        setupMockUserSession(secondaryUserId, mockHandle2)
        every { userSessionRepository.observePrimaryUserId() } returns userIdFlow

        // When
        observer.start()

        userIdFlow.emit(userId)
        advanceUntilIdle()

        userIdFlow.emit(secondaryUserId)
        advanceUntilIdle()

        // Then
        verify { mockHandle1.disconnect() }
        verify { mockHandle2 wasNot called }
        confirmVerified(mockHandle1, mockHandle2)
    }

    @Test
    fun `eventLoopErrorCallback submits error to flow`() = runTest {
        // Given
        val error = mockk<EventError>()
        val capturedCallback = slot<EventLoopErrorObserver>()

        setupMockUserSessionWithCallbackCapture(userId, capturedCallback)
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery { eventLoopErrorFlow.submit(any()) } just Runs

        // When
        observer.start()
        advanceUntilIdle()
        capturedCallback.captured.onEventLoopError(error)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { eventLoopErrorFlow.submit(error) }
        confirmVerified(eventLoopErrorFlow)
    }

    @Test
    fun `disconnect cancels coroutine scope and cleans up handle`() = runTest {
        // Given
        val mockHandle = mockk<EventLoopErrorObserverHandle>(relaxed = true)

        setupMockUserSession(userId, mockHandle)
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)

        observer.start()
        advanceUntilIdle()

        // When
        observer.disconnect()

        // Then
        verify { mockHandle.disconnect() }
        confirmVerified(mockHandle)
    }

    private fun setupMockUserSession(userId: UserId, handle: EventLoopErrorObserverHandle) {
        val mockUserSession = mockk<MailUserSessionWrapper>()

        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        every { mockUserSession.observeEventLoopErrors(any<EventLoopErrorObserver>()) } returns
            MailUserSessionObserveEventLoopErrorsResult.Ok(handle)
    }

    private fun setupMockUserSessionWithCallbackCapture(
        userId: UserId,
        callbackSlot: CapturingSlot<EventLoopErrorObserver>
    ) {
        val mockUserSession = mockk<MailUserSessionWrapper>()
        val mockHandle = mockk<EventLoopErrorObserverHandle>(relaxed = true)

        coEvery { userSessionRepository.getUserSession(userId) } returns mockUserSession
        every { mockUserSession.observeEventLoopErrors(capture(callbackSlot)) } returns
            MailUserSessionObserveEventLoopErrorsResult.Ok(mockHandle)
    }

    private companion object {

        val userId = UserId("user123")
        val secondaryUserId = UserId("user123-2")
    }
}
