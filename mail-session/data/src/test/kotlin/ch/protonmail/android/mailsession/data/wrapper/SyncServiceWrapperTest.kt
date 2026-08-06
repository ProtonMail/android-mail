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

package ch.protonmail.android.mailsession.data.wrapper

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.MailBackgroundExecScope
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.SyncOrchestartorStartOutcome
import uniffi.mail_uniffi.SyncOrchestartorStartStats
import uniffi.mail_uniffi.SyncService
import uniffi.mail_uniffi.SyncServiceIsEnabledResult
import uniffi.mail_uniffi.SyncServiceStartResult
import uniffi.mail_uniffi.SyncServiceUserStatusResult
import uniffi.mail_uniffi.SyncStatus
import uniffi.mail_uniffi.VoidProtonResult
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SyncServiceWrapperTest {

    private val rustUserSession = mockk<MailUserSession>()
    private val userSession = mockk<MailUserSessionWrapper> {
        every { getRustUserSession() } returns rustUserSession
    }

    private val execScope = mockk<MailBackgroundExecScope> {
        justRun { finsihed() }
    }
    private val mailSessionWrapper = mockk<MailSessionWrapper> {
        every { newBackgroundExecutionScope() } returns execScope
    }
    private val mailSessionRepository = mockk<MailSessionRepository> {
        every { getMailSession() } returns mailSessionWrapper
    }

    private val syncService = mockk<SyncService>()

    private val wrapper = SyncServiceWrapper(syncService, mailSessionRepository)

    @Test
    fun `start returns the orchestrator stats`() = runTest {
        // Given
        coEvery { syncService.start() } returns SyncServiceStartResult.Ok(
            SyncOrchestartorStartOutcome.Started(stats)
        )

        // When
        val result = wrapper.start()

        // Then
        assertEquals(stats.right(), result)
    }

    @Test
    fun `start returns the stats when the orchestrator was already running`() = runTest {
        // Given
        coEvery { syncService.start() } returns SyncServiceStartResult.Ok(
            SyncOrchestartorStartOutcome.Ongoing(stats)
        )

        // When
        val result = wrapper.start()

        // Then
        assertEquals(stats.right(), result)
    }

    @Test
    fun `start maps the proton error to a data error`() = runTest {
        // Given
        coEvery { syncService.start() } returns SyncServiceStartResult.Error(ProtonError.Network)

        // When
        val result = wrapper.start()

        // Then
        assertEquals(DataError.Remote.NoNetwork.left(), result)
    }

    @Test
    fun `per user calls pass the rust user session as the context`() = runTest {
        // Given
        coEvery { syncService.userStatus(rustUserSession) } returns
            SyncServiceUserStatusResult.Ok(SyncStatus.ONGOING)

        // When
        val result = wrapper.userStatus(userSession)

        // Then
        assertEquals(SyncStatus.ONGOING.right(), result)
    }

    @Test
    fun `the background execution scope is closed once the call answers`() = runTest {
        // Given
        coEvery { syncService.isEnabled(rustUserSession) } returns SyncServiceIsEnabledResult.Ok(true)

        // When
        wrapper.isEnabled(userSession)

        // Then
        verify(exactly = 1) { execScope.finsihed() }
    }

    @Test
    fun `the background execution scope is closed when the call throws`() = runTest {
        // Given
        coEvery { syncService.stop() } throws IllegalStateException("boom")

        // When
        runCatching { wrapper.stop() }

        // Then
        verify(exactly = 1) { execScope.finsihed() }
    }

    @Test
    fun `a hung actor call is bounded by the timeout and reported as an error`() = runTest {
        // Given
        coEvery { syncService.isEnabled(rustUserSession) } coAnswers { awaitCancellation() }

        // When
        val result = wrapper.isEnabled(userSession)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
        verify(exactly = 1) { execScope.finsihed() }
    }

    @Test
    fun `setEnabled maps a void result to unit`() = runTest {
        // Given
        coEvery { syncService.setEnabled(rustUserSession, true) } returns VoidProtonResult.Ok

        // When
        val result = wrapper.setEnabled(userSession, enabled = true)

        // Then
        assertEquals(Unit.right(), result)
    }

    private companion object {

        val stats = SyncOrchestartorStartStats(
            completed = 1uL,
            pending = 2uL,
            disabled = 0uL,
            failed = 0uL,
            ongoing = 1uL,
            total = 4uL
        )
    }
}
