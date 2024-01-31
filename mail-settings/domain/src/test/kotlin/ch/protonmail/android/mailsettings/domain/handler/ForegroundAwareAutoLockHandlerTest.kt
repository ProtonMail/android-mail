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

package ch.protonmail.android.mailsettings.domain.handler

import java.time.Instant
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailsettings.domain.usecase.autolock.HasAutoLockPendingAttempt
import ch.protonmail.android.mailsettings.domain.usecase.autolock.IsAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateLastForegroundMillis
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class ForegroundAwareAutoLockHandlerTest {

    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val updateAutoLockLastForegroundMillis = mockk<UpdateLastForegroundMillis>()
    private val hasAutoLockPendingAttempt = mockk<HasAutoLockPendingAttempt>()
    private val isAutoLockEnabled = mockk<IsAutoLockEnabled>()
    private val testDispatcher = StandardTestDispatcher()
    private val scope = TestScope(testDispatcher)

    private val handler = ForegroundAwareAutoLockHandler(
        appInBackgroundState,
        updateAutoLockLastForegroundMillis,
        hasAutoLockPendingAttempt,
        isAutoLockEnabled,
        scope
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should do nothing if the app is not in the background`() = runTest {
        // Given
        expectAppInBackground(false)

        // When
        handler.handle()
        advanceUntilIdle()

        // Then
        coVerify {
            isAutoLockEnabled wasNot called
            hasAutoLockPendingAttempt wasNot called
            updateAutoLockLastForegroundMillis wasNot called
        }
    }

    @Test
    fun `should do nothing if auto lock is not enabled`() = runTest {
        // Given
        expectAppInBackground(true)
        expectAutoLockEnabled(false)

        // When
        handler.handle()
        advanceUntilIdle()

        // Then
        coVerify {
            hasAutoLockPendingAttempt wasNot called
            updateAutoLockLastForegroundMillis wasNot called
        }
    }

    @Test
    fun `should do nothing if an existing auto lock is pending`() = runTest {
        // Given
        expectAppInBackground(true)
        expectAutoLockEnabled(true)
        expectAutoLockPending(true)

        // When
        handler.handle()
        advanceUntilIdle()

        // Then
        coVerify {
            updateAutoLockLastForegroundMillis wasNot called
        }
    }

    @Test
    fun `should update the timestamp if in background, with auto lock and no pending attempts`() = runTest {
        // Given
        val expectedCurrentInstant = 1L

        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns expectedCurrentInstant }
        expectAppInBackground(true)
        expectAutoLockEnabled(true)
        expectAutoLockPending(false)
        coEvery { updateAutoLockLastForegroundMillis(any()) } returns Unit.right()

        // When
        handler.handle()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) {
            updateAutoLockLastForegroundMillis(any())
        }
    }

    private fun expectAppInBackground(value: Boolean) {
        every { appInBackgroundState.observe() } returns flowOf(value)
    }

    private suspend fun expectAutoLockEnabled(value: Boolean) {
        coEvery { isAutoLockEnabled() } returns value
    }

    private fun expectAutoLockPending(value: Boolean) {
        coEvery { hasAutoLockPendingAttempt() } returns value
    }
}
