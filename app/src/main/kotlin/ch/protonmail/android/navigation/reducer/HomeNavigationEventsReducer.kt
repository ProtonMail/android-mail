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

package ch.protonmail.android.navigation.reducer

import java.net.URLEncoder
import ch.protonmail.android.mailcommon.domain.model.encode
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeNavigationEvent
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.model.NavigationEffect
import timber.log.Timber
import javax.inject.Inject

class HomeNavigationEventsReducer @Inject constructor() {

    fun reduce(state: HomeState, event: HomeNavigationEvent): HomeState {
        return when (event) {
            is HomeNavigationEvent.LauncherIntentReceived ->
                handleLauncherIntentReceived(state)

            is HomeNavigationEvent.ExternalShareIntentReceived ->
                handleExternalShareIntentReceived(state, event)

            is HomeNavigationEvent.InternalShareIntentReceived ->
                handleInternalShareIntentReceived(state, event)

            is HomeNavigationEvent.MailToIntentReceived ->
                handleMailToIntentReceived(state, event)

            is HomeNavigationEvent.InvalidShareIntentReceived ->
                handleInvalidShareIntentReceived(state)

            is HomeNavigationEvent.UnknownIntentReceived ->
                handleUnknownIntentReceived(state)
        }
    }

    private fun handleLauncherIntentReceived(state: HomeState): HomeState {
        if (state.startedFromLauncher) {
            Timber.tag("intent-navigation").d("Launcher intent received, no-op (already startedFromLauncher)")
            return state
        }

        Timber.tag("intent-navigation").d("Launcher intent detected, setting startedFromLauncher=true")
        return state.copy(startedFromLauncher = true)
    }

    private fun handleExternalShareIntentReceived(
        state: HomeState,
        intent: HomeNavigationEvent.ExternalShareIntentReceived
    ): HomeState {
        Timber.tag("intent-navigation").d("Received EXTERNAL share intent: ${intent.shareInfo}")

        val draftAction = DraftAction.PrefillForShare(intent.shareInfo.encode())
        val navigate = NavigationEffect.NavigateTo(
            Destination.Screen.ShareFileComposer(
                draftAction = draftAction,
                isExternal = true
            )
        )

        return state.copy(navigateToEffect = Effect.of(navigate))
    }

    private fun handleInternalShareIntentReceived(
        state: HomeState,
        event: HomeNavigationEvent.InternalShareIntentReceived
    ): HomeState {
        Timber.tag("intent-navigation").d("Received INTERNAL share intent: ${event.shareInfo}")

        val draftAction = DraftAction.PrefillForShare(event.shareInfo.encode())
        val navigate = NavigationEffect.NavigateTo(
            Destination.Screen.ShareFileComposer(
                draftAction = draftAction,
                isExternal = false
            )
        )

        return state.copy(navigateToEffect = Effect.of(navigate))
    }

    private fun handleMailToIntentReceived(
        state: HomeState,
        event: HomeNavigationEvent.MailToIntentReceived
    ): HomeState {
        val draftAction = DraftAction.MailTo(URLEncoder.encode(event.intent.dataString, Charsets.UTF_8.name()))
        val navigate = NavigationEffect.NavigateTo(
            Destination.Screen.MessageActionComposer(
                action = draftAction
            )
        )

        return state.copy(navigateToEffect = Effect.of(navigate))
    }

    private fun handleUnknownIntentReceived(state: HomeState): HomeState {
        Timber.tag("intent-navigation").d("Unknown intent received!")

        return state
    }

    private fun handleInvalidShareIntentReceived(state: HomeState): HomeState {
        Timber.tag("intent-navigation").w("Invalid share intent received!")

        return state
    }

}
