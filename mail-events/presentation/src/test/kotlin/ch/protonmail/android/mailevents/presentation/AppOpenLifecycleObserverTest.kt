/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailevents.presentation

import androidx.lifecycle.LifecycleOwner
import arrow.core.right
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackAppOpenEvent
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.time.Clock

internal class AppOpenLifecycleObserverTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val trackAppOpenEvent = mockk<TrackAppOpenEvent>()
    private val eventsRepository = mockk<EventsRepository>(relaxUnitFun = true)
    private val lifecycleOwner = mockk<LifecycleOwner>()
    private val testScope = TestScope(dispatcherRule.testDispatcher)

    private fun createObserver() = AppOpenLifecycleObserver(
        trackAppOpenEvent = trackAppOpenEvent,
        eventsRepository = eventsRepository,
        scope = testScope
    )

    @Test
    fun `should track app open event with isNewSession false on first onStart`() = testScope.runTest {
        // Given
        coEvery { eventsRepository.getLastAppOpenTimestamp() } returns null
        coEvery { eventsRepository.saveLastAppOpenTimestamp(any()) } returns Unit.right()
        coEvery { trackAppOpenEvent(isNewSession = false) } returns Unit.right()
        val observer = createObserver()

        // When
        observer.onStart(lifecycleOwner)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { trackAppOpenEvent(isNewSession = false) }
        coVerify(exactly = 1) { eventsRepository.saveLastAppOpenTimestamp(any()) }
    }

    @Test
    fun `should not track within throttle window when process is alive`() = testScope.runTest {
        // Given
        coEvery { eventsRepository.getLastAppOpenTimestamp() } returns null
        coEvery { eventsRepository.saveLastAppOpenTimestamp(any()) } returns Unit.right()
        coEvery { trackAppOpenEvent(isNewSession = false) } returns Unit.right()
        val observer = createObserver()

        // When
        observer.onStart(lifecycleOwner)
        advanceUntilIdle()
        observer.onStart(lifecycleOwner)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { trackAppOpenEvent(isNewSession = false) }
    }

    @Test
    fun `should track with isNewSession true after throttle duration`() = testScope.runTest {
        val oldTimestamp = Clock.System.now().toEpochMilliseconds() - AppOpenLifecycleObserver.THROTTLE_DURATION_MS
        coEvery { eventsRepository.getLastAppOpenTimestamp() } returns oldTimestamp
        coEvery { eventsRepository.saveLastAppOpenTimestamp(any()) } returns Unit.right()
        coEvery { trackAppOpenEvent(isNewSession = true) } returns Unit.right()
        val observer = createObserver()

        observer.setLastTrackedMsForTesting(oldTimestamp)

        observer.onStart(lifecycleOwner)
        advanceUntilIdle()

        coVerify(exactly = 1) { trackAppOpenEvent(isNewSession = true) }
        coVerify(exactly = 1) { eventsRepository.saveLastAppOpenTimestamp(any()) }
    }

    @Test
    fun `should track with isNewSession true on process death within throttle window`() = testScope.runTest {
        // Given
        val recentTimestamp = Clock.System.now().toEpochMilliseconds() - 10 * 60 * 1000 // 10 minutes ago
        coEvery { eventsRepository.getLastAppOpenTimestamp() } returns recentTimestamp
        coEvery { eventsRepository.saveLastAppOpenTimestamp(any()) } returns Unit.right()
        coEvery { trackAppOpenEvent(isNewSession = true) } returns Unit.right()
        val observer = createObserver()

        // When
        observer.onStart(lifecycleOwner)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { trackAppOpenEvent(isNewSession = true) }
        coVerify(exactly = 1) { eventsRepository.saveLastAppOpenTimestamp(any()) }
    }
}
