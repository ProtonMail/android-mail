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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeOnboardingPageDismissed
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeSpotlightConnected
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ByoeSpotlightDismissed
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.ContentSearchAnswered
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.HomeReadinessChanged
import ch.protonmail.android.feature.spotlight.HomeAnnouncementAction.Shown
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.AnnouncementHold
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.usecase.GetNextAnnouncement
import ch.protonmail.android.mailspotlight.domain.usecase.MarkAnnouncementResolved
import ch.protonmail.android.mailspotlight.domain.usecase.MarkAnnouncementShown
import ch.protonmail.android.mailspotlight.domain.usecase.MigrateAnnouncementState
import ch.protonmail.android.mailspotlight.domain.usecase.RefreshFeatureEntryPoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Asks the SDK which announcement to show, and records what the user did with it. The SDK decides the order and the
 * gap; Home only says when it is ready: its own prompts are done and the mailbox is on screen.
 *
 * It asks on every foreground and account switch. When Home is not ready then, or the announcements are held, it asks
 * once it can.
 */
@HiltViewModel
class HomeAnnouncementViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    appInBackgroundState: AppInBackgroundState,
    announcementHold: AnnouncementHold,
    private val migrateAnnouncementState: MigrateAnnouncementState,
    private val getNextAnnouncement: GetNextAnnouncement,
    private val markAnnouncementShown: MarkAnnouncementShown,
    private val markAnnouncementResolved: MarkAnnouncementResolved,
    private val refreshFeatureEntryPoints: RefreshFeatureEntryPoints,
    private val dismissContentSearchPrompt: DismissContentSearchPrompt,
    private val dismissByoeGuidance: DismissByoeGuidance,
    private val reducer: HomeAnnouncementReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow(HomeAnnouncementState.Initial)
    val state: StateFlow<HomeAnnouncementState> = mutableState.asStateFlow()

    init {
        observePrimaryUserId().filterNotNull().distinctUntilChanged()
            .flatMapLatest { userId ->
                appInBackgroundState.observe().distinctUntilChanged().filter { isInBackground -> !isInBackground }
                    .map { userId }
            }
            .onEach { userId ->
                reduce(HomeAnnouncementEvent.AskDue(userId))
                // Settings and the sidebar read the SDK before Home is ready to ask, so the migration runs first.
                migrateAnnouncementState(userId)
                refreshFeatureEntryPoints(userId)
            }
            .launchIn(viewModelScope)

        announcementHold.observeIsHeld()
            .onEach { isHeld -> reduce(HomeAnnouncementEvent.HoldChanged(isHeld)) }
            .launchIn(viewModelScope)
    }

    fun submit(action: HomeAnnouncementAction) {
        val previous = state.value
        reduce(action)
        val current = state.value
        val userId = current.userId ?: return
        viewModelScope.launch {
            when (action) {
                is HomeReadinessChanged -> Unit
                // Only a new showing is recorded: a repeat restarts the gap before the next announcement.
                is Shown -> if (current.shown != previous.shown) current.shown?.let {
                    markAnnouncementShown(it.userId, it.announcement)
                }
                is ContentSearchAnswered -> {
                    val owner = current.ownerOf(Announcement.ContentSearchWhatsNew, userId)
                    // Either answer is an answer to the offer of content search, so the search screen stops making
                    // it in its banner.
                    dismissContentSearchPrompt(owner, ContentSearchPrompt.SetUpOffer)
                    markAnnouncementResolved(owner, Announcement.ContentSearchWhatsNew)
                }
                is ByoeSpotlightConnected ->
                    markAnnouncementResolved(current.ownerOf(action.announcement, userId), action.announcement)
                is ByoeSpotlightDismissed -> {
                    val owner = current.ownerOf(action.announcement, userId)
                    dismissByoeGuidance(owner, ByoeSurface.Spotlight, action.method)
                    markAnnouncementResolved(owner, action.announcement)
                }
                is ByoeOnboardingPageDismissed ->
                    dismissByoeGuidance(userId, ByoeSurface.OnboardingScreen, action.method)
            }
        }
    }

    // The account the announcement was shown for, which is not the primary one after an account switch.
    private fun HomeAnnouncementState.ownerOf(announcement: Announcement, primaryUserId: UserId): UserId =
        shown?.takeIf { it.announcement == announcement }?.userId ?: primaryUserId

    private fun reduce(operation: HomeAnnouncementOperation) {
        mutableState.update { reducer.newStateFrom(it, operation) }
        val current = state.value
        if (current.shouldAsk && current.userId != null) ask(current.userId)
    }

    private fun ask(userId: UserId) {
        reduce(HomeAnnouncementEvent.AskStarted)
        viewModelScope.launch {
            migrateAnnouncementState(userId)
            reduce(HomeAnnouncementEvent.AnnouncementLoaded(userId, getNextAnnouncement(userId)))
        }
    }
}
