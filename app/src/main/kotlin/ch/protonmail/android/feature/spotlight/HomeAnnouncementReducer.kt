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

package ch.protonmail.android.feature.spotlight

import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeOnboardingPageDismissed
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeSpotlightConnected
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeSpotlightDismissed
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ContentSearchAnswered
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.HomeReadinessChanged
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.Shown
import ch.protonmail.android.feature.spotlight.HomeAnnouncementEvent.AnnouncementLoaded
import ch.protonmail.android.feature.spotlight.HomeAnnouncementEvent.AskDue
import ch.protonmail.android.feature.spotlight.HomeAnnouncementEvent.AskStarted
import ch.protonmail.android.mailcommon.presentation.Effect
import javax.inject.Inject

class HomeAnnouncementReducer @Inject constructor() {

    fun newStateFrom(state: HomeAnnouncementState, operation: HomeAnnouncementOperation): HomeAnnouncementState =
        when (operation) {
            is AskDue -> state.onAskDue(operation)
            is AskStarted -> state.copy(isAskDue = false, isAsking = true)
            is AnnouncementLoaded -> state.onAnnouncementLoaded(operation)
            is HomeReadinessChanged -> state.onHomeReadinessChanged(operation.isReady)
            is Shown -> state.onShown(operation)
            is ContentSearchAnswered,
            is ByoeSpotlightConnected,
            is ByoeSpotlightDismissed,
            is ByoeOnboardingPageDismissed -> state
        }

    // A repeat, e.g. after a rotation or back from the Gmail connection, is not a new showing.
    private fun HomeAnnouncementState.onShown(action: Shown): HomeAnnouncementState {
        if (pending != action.announcement || userId == null) return this
        return copy(pending = null, shown = ShownAnnouncement(userId, action.announcement))
    }

    // Another account's announcement must not show for this one.
    private fun HomeAnnouncementState.onAskDue(event: AskDue) = if (event.userId == userId) {
        copy(isAskDue = true)
    } else {
        copy(userId = event.userId, isAskDue = true, isAsking = false, pending = null)
    }

    private fun HomeAnnouncementState.onAnnouncementLoaded(event: AnnouncementLoaded): HomeAnnouncementState {
        if (event.userId != userId) return this
        val announcement = event.announcement ?: return copy(isAsking = false)
        // Home moved on during the ask. The answer is not kept for later, when it may no longer be due: the SDK is
        // asked again once Home can show an announcement.
        if (!isHomeReady) return copy(isAsking = false, isAskDue = true)
        return copy(isAsking = false, pending = announcement, navigateTo = Effect.of(announcement))
    }

    // Back on the mailbox with an announcement that never appeared: the SDK says whether it is still due.
    private fun HomeAnnouncementState.onHomeReadinessChanged(isReady: Boolean): HomeAnnouncementState {
        val becameReady = isReady && !isHomeReady
        return if (becameReady && pending != null) {
            copy(isHomeReady = true, pending = null, isAskDue = true)
        } else {
            copy(isHomeReady = isReady)
        }
    }
}
