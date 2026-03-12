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

import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackSignupEvent
import ch.protonmail.android.mailevents.domain.usecase.TrackSubscriptionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountEventObserver @Inject constructor(
    private val broadcaster: AccountEventBroadcaster,
    private val trackSignupEvent: TrackSignupEvent,
    private val trackSubscriptionEvent: TrackSubscriptionEvent,
    private val eventsRepository: EventsRepository
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            broadcaster.events.collect { event ->
                when (event) {
                    is AccountEvent.SignupCompleted -> {
                        trackSignupEvent().onLeft { error ->
                            Timber.e("Failed to track signup event: $error")
                        }
                    }

                    is AccountEvent.PurchaseCompleted -> {
                        trackSubscriptionEvent(
                            contentList = listOf(event.productId),
                            price = event.amount.toDouble() / MICROS_PER_UNIT,
                            currency = event.currency,
                            cycle = event.cycle,
                            transactionId = event.orderId
                        ).onLeft { error ->
                            Timber.e("Failed to track purchase completed event: $error")
                        }
                    }

                    is AccountEvent.SubscriptionScreenShown -> {
                        eventsRepository.sendEvent(AppEvent.SubscriptionManualShown).onLeft { error ->
                            Timber.e("Failed to track subscription manual event: $error")
                        }
                    }
                }
            }
        }
    }

    private companion object {

        const val MICROS_PER_UNIT = 1_000_000.0
    }
}
