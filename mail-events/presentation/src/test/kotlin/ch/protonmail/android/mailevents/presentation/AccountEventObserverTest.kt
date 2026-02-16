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
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackSignupEvent
import ch.protonmail.android.mailevents.domain.usecase.TrackSubscriptionEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import kotlin.test.Test

internal class AccountEventObserverTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val broadcastFlow = MutableSharedFlow<AccountEvent>()
    private val broadcaster = mockk<AccountEventBroadcaster> {
        coEvery { events } returns broadcastFlow
    }
    private val trackSignupEvent = mockk<TrackSignupEvent>()
    private val trackSubscriptionEvent = mockk<TrackSubscriptionEvent>()
    private val eventsRepository = mockk<EventsRepository>()

    private val observer = AccountEventObserver(
        broadcaster = broadcaster,
        trackSignupEvent = trackSignupEvent,
        trackSubscriptionEvent = trackSubscriptionEvent,
        eventsRepository = eventsRepository
    )

    @Test
    fun `should track signup event when SignupCompleted is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { trackSignupEvent() } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(AccountEvent.SignupCompleted)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { trackSignupEvent() }
    }

    @Test
    fun `should track subscription event when PurchaseCompleted is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { trackSubscriptionEvent(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(
            AccountEvent.PurchaseCompleted(
                productId = "plan-1",
                planName = "plus",
                cycle = 12,
                amount = 4_999_000L,
                currency = "USD",
                orderId = "order-123"
            )
        )

        // Then
        coVerify(exactly = 1) {
            trackSubscriptionEvent(
                contentList = listOf("plan-1"),
                price = 4_999_000L.toDouble() / 1_000_000.0,
                currency = "USD",
                cycle = 12,
                couponCode = null,
                transactionId = "order-123",
                isFirstPurchase = true,
                isFreeToPaid = true
            )
        }
    }

    @Test
    fun `should send event when SubscriptionManualShown is emitted`() = runTest(testDispatcher) {
        // Given
        coEvery { eventsRepository.sendEvent(any()) } returns Unit.right()
        observer.start(backgroundScope)

        // When
        broadcastFlow.emit(AccountEvent.SubscriptionScreenShown)

        // Then
        coVerify(exactly = 1) { eventsRepository.sendEvent(AppEvent.SubscriptionManualShown) }
    }
}
