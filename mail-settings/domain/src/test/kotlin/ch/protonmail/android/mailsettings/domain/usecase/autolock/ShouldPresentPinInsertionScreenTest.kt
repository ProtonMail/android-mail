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

package ch.protonmail.android.mailsettings.domain.usecase.autolock

import java.time.Instant
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastForegroundMillis
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.test.assertTrue

internal class ShouldPresentPinInsertionScreenTest {

    private val appInBackgroundState = mockk<AppInBackgroundState>()
    private val isAutoLockEnabled = mockk<IsAutoLockEnabled>()
    private val hasValidPinValue = mockk<HasValidPinValue>()
    private val isAutoLockAttemptPending = mockk<HasAutoLockPendingAttempt>()
    private val getLastAppForegroundTimestamp = mockk<GetLastAppForegroundTimestamp>()
    private val getCurrentAutoLockInterval = mockk<GetCurrentAutoLockInterval>()

    private val shouldPresentPinInsertionScreen = ShouldPresentPinInsertionScreen(
        appInBackgroundState,
        isAutoLockEnabled,
        hasValidPinValue,
        isAutoLockAttemptPending,
        getLastAppForegroundTimestamp,
        getCurrentAutoLockInterval
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should not indicate to display pin screen and do nothing when the app is in the background`() = runTest {
        // Given
        expectAppInBackground()

        // When
        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertFalse(result)
        coVerify {
            isAutoLockEnabled wasNot called
            hasValidPinValue wasNot called
            isAutoLockAttemptPending wasNot called
            getLastAppForegroundTimestamp wasNot called
            getCurrentAutoLockInterval wasNot called
        }
    }

    @Test
    fun `should not indicate to display pin screen when auto lock is disabled`() = runTest {
        // Given
        expectAppInForeground()
        coEvery { isAutoLockEnabled() } returns false

        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertFalse(result)
        coVerify {
            hasValidPinValue wasNot called
            isAutoLockAttemptPending wasNot called
            getLastAppForegroundTimestamp wasNot called
            getCurrentAutoLockInterval wasNot called
        }
    }

    @Test
    fun `should not indicate to display pin screen when has no valid pin value`() = runTest {
        // Given
        expectAppInForeground()
        expectAutoLockEnabled()
        coEvery { hasValidPinValue() } returns false

        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertFalse(result)
        coVerify {
            isAutoLockAttemptPending wasNot called
            getLastAppForegroundTimestamp wasNot called
            getCurrentAutoLockInterval wasNot called
        }
    }

    @Test
    fun `should indicate to display pin screen when a pending attempt is present`() = runTest {
        // Given
        expectAppInForeground()
        expectAutoLockEnabled()
        expectValidPinValue()
        coEvery { isAutoLockAttemptPending() } returns true

        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertTrue(result)
        coVerify {
            getLastAppForegroundTimestamp wasNot called
            getCurrentAutoLockInterval wasNot called
        }
    }

    @Test
    fun `should indicate to display pin screen the saved auto lock interval has passed`() = runTest {
        // Given
        expectAppInForeground()
        expectAutoLockEnabled()
        expectValidPinValue()
        expectNoPendingAttempt()
        expectAutoLockInterval(AutoLockInterval.Immediately)
        expectCurrentTime(2L)
        expectLastForegroundTimestamp(1L)

        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `should indicate to not display pin screen the saved auto lock interval has not passed`() = runTest {
        // Given
        expectAppInForeground()
        expectAutoLockEnabled()
        expectValidPinValue()
        expectNoPendingAttempt()
        expectAutoLockInterval(AutoLockInterval.FiveMinutes)
        expectCurrentTime(10L)
        expectLastForegroundTimestamp(9L)

        val result = shouldPresentPinInsertionScreen().first()

        // Then
        assertFalse(result)
    }

    private fun expectAutoLockEnabled() {
        coEvery { isAutoLockEnabled() } returns true
    }

    private fun expectValidPinValue() {
        coEvery { hasValidPinValue() } returns true
    }

    private fun expectNoPendingAttempt() {
        coEvery { isAutoLockAttemptPending() } returns false
    }

    private fun expectAutoLockInterval(autoLockInterval: AutoLockInterval) {
        coEvery { getCurrentAutoLockInterval() } returns autoLockInterval.right()
    }

    private fun expectLastForegroundTimestamp(timestamp: Long) {
        coEvery { getLastAppForegroundTimestamp.invoke() } returns AutoLockLastForegroundMillis(timestamp)
    }

    private fun expectCurrentTime(timestamp: Long) {
        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { toEpochMilli() } returns timestamp }
    }

    private fun expectAppInForeground() {
        every { appInBackgroundState.observe() } returns flowOf(false)
    }

    private fun expectAppInBackground() {
        every { appInBackgroundState.observe() } returns flowOf(true)
    }
}
