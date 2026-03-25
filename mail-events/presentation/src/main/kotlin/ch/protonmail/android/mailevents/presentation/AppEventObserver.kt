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

import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackFirstMessageSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEventObserver @Inject constructor(
    private val broadcaster: AppEventBroadcaster,
    private val trackFirstMessageSentEvent: TrackFirstMessageSentEvent,
    private val eventsRepository: EventsRepository
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            broadcaster.events.collect { event ->
                when (event) {
                    is AppEvent.MessageSent -> {
                        trackFirstMessageSentEvent().onLeft { error ->
                            Timber.d("Failed to track first message sent event: $error")
                        }
                    }

                    is AppEvent.SubscriptionPaywallShown,
                    is AppEvent.SubscriptionOnboardingShown -> {
                        eventsRepository.sendEvent(event).onLeft { error ->
                            Timber.d("Failed to track subscription event: $error")
                        }
                    }

                    is AppEvent.OfferReceived,
                    is AppEvent.OfferClicked -> {
                        eventsRepository.sendEvent(event).onLeft { error ->
                            Timber.d("Failed to track offer event: $error")
                        }
                    }

                    is AppEvent.OnboardingCompleted -> {
                        eventsRepository.sendEvent(event).onLeft { error ->
                            Timber.d("Failed to track onboarding event: $error")
                        }
                    }

                    else -> Timber.w("Unhandled app event: $event")
                }
            }
        }
    }
}
