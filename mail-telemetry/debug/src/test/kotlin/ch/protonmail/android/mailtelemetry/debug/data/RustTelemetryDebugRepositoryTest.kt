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

package ch.protonmail.android.mailtelemetry.debug.data

import arrow.core.right
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugAccount
import ch.protonmail.android.mailtelemetry.debug.domain.TelemetryDebugSwitches
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionIsCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionIsTelemetryDebugSendingHaltedResult
import uniffi.mail_uniffi.MailUserSessionStartCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionStopCapturingTelemetryDebugEventsResult
import uniffi.mail_uniffi.MailUserSessionTelemetryDebugEventsResult
import uniffi.mail_uniffi.TelemetryDebugEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RustTelemetryDebugRepositoryTest {

    private val rustSession = mockk<MailUserSession>()
    private val userSessionRepository = mockk<UserSessionRepository>()
    private val executeWithUserSession = mockk<ExecuteWithUserSession>()

    private val repository = RustTelemetryDebugRepository(userSessionRepository, executeWithUserSession)

    @Test
    fun `should track the primary account`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryAccount() } returns flowOf(
            Account(
                userId = UserId("user"),
                name = "Test User",
                state = AccountState.Ready,
                primaryAddress = "test@proton.me"
            )
        )

        // When
        val actual = repository.observeTrackedAccount().first()

        // Then
        assertEquals(TelemetryDebugAccount(name = "Test User", email = "test@proton.me"), actual)
    }

    @Test
    fun `should track no account when no user is logged in`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryAccount() } returns flowOf(null)

        // When
        val actual = repository.observeTrackedAccount().first()

        // Then
        assertNull(actual)
    }

    @Test
    fun `should return captured events newest first`() = runTest {
        // Given
        expectPrimaryUserSession()
        coEvery { rustSession.telemetryDebugEvents() } returns
            MailUserSessionTelemetryDebugEventsResult.Ok(listOf(rustEvent("1"), rustEvent("2")))

        // When
        val actual = repository.getEvents()

        // Then
        assertEquals(listOf("2", "1"), actual.getOrNull()?.map { it.id })
    }

    @Test
    fun `should map the rust event fields onto the domain model`() = runTest {
        // Given
        expectPrimaryUserSession()
        coEvery { rustSession.telemetryDebugEvents() } returns
            MailUserSessionTelemetryDebugEventsResult.Ok(listOf(rustEvent("id")))

        // When
        val actual = repository.getEvents().getOrNull()?.first()

        // Then
        assertEquals("mail.any", actual?.measurementGroup)
        assertEquals("event_name", actual?.name)
        assertEquals(mapOf("count" to 1.0), actual?.values)
        assertEquals(mapOf("origin" to "test"), actual?.dimensions)
    }

    @Test
    fun `should return an error when no user is logged in`() = runTest {
        // Given
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(null)

        // When
        val actual = repository.getEvents()

        // Then
        assertTrue(actual.isLeft())
    }

    @Test
    fun `should return both switches as reported by the sdk`() = runTest {
        // Given
        expectPrimaryUserSession()
        coEvery { rustSession.isCapturingTelemetryDebugEvents() } returns
            MailUserSessionIsCapturingTelemetryDebugEventsResult.Ok(true)
        coEvery { rustSession.isTelemetryDebugSendingHalted() } returns
            MailUserSessionIsTelemetryDebugSendingHaltedResult.Ok(false)

        // When
        val actual = repository.getSwitches()

        // Then
        assertEquals(TelemetryDebugSwitches(capturing = true, sendingHalted = false), actual.getOrNull())
    }

    @Test
    fun `should start and stop capturing according to the requested value`() = runTest {
        // Given
        expectPrimaryUserSession()
        coEvery { rustSession.startCapturingTelemetryDebugEvents() } returns
            MailUserSessionStartCapturingTelemetryDebugEventsResult.Ok
        coEvery { rustSession.stopCapturingTelemetryDebugEvents() } returns
            MailUserSessionStopCapturingTelemetryDebugEventsResult.Ok

        // When
        repository.setCapturing(enabled = true)
        repository.setCapturing(enabled = false)

        // Then
        coVerify(exactly = 1) { rustSession.startCapturingTelemetryDebugEvents() }
        coVerify(exactly = 1) { rustSession.stopCapturingTelemetryDebugEvents() }
    }

    private fun expectPrimaryUserSession() {
        val wrapper = mockk<MailUserSessionWrapper> {
            every { getRustUserSession() } returns rustSession
        }
        every { userSessionRepository.observePrimaryUserId() } returns flowOf(userId)
        coEvery { executeWithUserSession(userId, any<suspend (MailUserSessionWrapper) -> Any>()) } coAnswers {
            val action = secondArg<suspend (MailUserSessionWrapper) -> Any>()
            action(wrapper).right()
        }
    }

    private fun rustEvent(id: String) = TelemetryDebugEvent(
        id = id,
        measurementGroup = "mail.any",
        event = "event_name",
        values = mapOf("count" to 1.0),
        dimensions = mapOf("origin" to "test")
    )

    private companion object {

        val userId = UserId("user-id")
    }
}
