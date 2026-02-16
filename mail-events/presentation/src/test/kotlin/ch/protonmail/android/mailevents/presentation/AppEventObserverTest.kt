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

import arrow.core.right
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackFirstMessageSentEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class AppEventObserverTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val broadcastFlow = MutableSharedFlow<AppEvent>()
    private val broadcaster = mockk<AppEventBroadcaster> {
        coEvery { events } returns broadcastFlow
    }
    private val trackFirstMessageSentEvent = mockk<TrackFirstMessageSentEvent>()
    private val eventsRepository = mockk<EventsRepository>()

    private val observer = AppEventObserver(
        broadcaster = broadcaster,
        trackFirstMessageSentEvent = trackFirstMessageSentEvent,
        eventsRepository = eventsRepository
    )

    @Test
    fun `should track first message sent event when MessageSent is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { trackFirstMessageSentEvent() } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(AppEvent.MessageSent)

        // Then
        coVerify(exactly = 1) { trackFirstMessageSentEvent() }
    }

    @Test
    fun `should send event when SubscriptionPaywallShown is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(AppEvent.SubscriptionPaywallShown)

        // Then
        coVerify(exactly = 1) { eventsRepository.sendEvent(AppEvent.SubscriptionPaywallShown) }
    }

    @Test
    fun `should send event when SubscriptionOnboardingShown is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(AppEvent.SubscriptionOnboardingShown)

        // Then
        coVerify(exactly = 1) { eventsRepository.sendEvent(AppEvent.SubscriptionOnboardingShown) }
    }

    @Test
    fun `should send event when OfferReceived is emitted`() = runTest(testDispatcher) {
        // Given
        val event = AppEvent.OfferReceived("intro_price")
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(event)

        // Then
        coVerify(exactly = 1) { eventsRepository.sendEvent(event) }
    }

    @Test
    fun `should send event when OfferClicked is emitted`() = runTest(testDispatcher) {
        // Given
        val event = AppEvent.OfferClicked("black_friday_wave1")
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(event)

        // Then
        coVerify(exactly = 1) { eventsRepository.sendEvent(event) }
    }
}
