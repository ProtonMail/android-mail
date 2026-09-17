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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchPromptDismissed
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

/**
 * The indexing progress the search screen shows for the primary account.
 *
 * The banner is only for an account whose backfill is actually running. Content search switched off
 * by the user, unavailable on this build, or unavailable on a 32-bit device all read as nothing to
 * show.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IndexingProgressViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val observeContentSearchIndexingStatus: ObserveContentSearchIndexingStatus,
    private val observeContentSearchPromptDismissed: ObserveContentSearchPromptDismissed,
    private val dismissContentSearchPrompt: DismissContentSearchPrompt
) : ViewModel() {

    private val mutableIndexingComplete = MutableStateFlow<Boolean?>(null)

    /**
     * Whether to tell the primary account that its indexing is done, or null until that is known.
     *
     * Read once, when the screen opens, and never again while it is open. Indexing that finishes
     * under the user's eyes leaves the progress banner at 100%, and this is shown on the next visit
     * instead of appearing on its own.
     *
     * The screen waits for the answer before it focuses the search field. Reading null as "nothing
     * to tell" would raise the keyboard, and then the note would come up behind it.
     */
    val showIndexingComplete: StateFlow<Boolean?> = mutableIndexingComplete.asStateFlow()

    /**
     * Kept eagerly subscribed for the ViewModel's whole life, so the banner's latch survives the
     * screen going to the background and back.
     */
    val state: StateFlow<IndexingProgressState> = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId -> observeBannerFor(userId) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, IndexingProgressState.Hidden)

    init {
        viewModelScope.launch {
            mutableIndexingComplete.value = readNoteToShow() ?: false
        }
    }

    /**
     * Whether to show the note, or null if the read does not finish in time.
     *
     * Bounded as a whole, not one source at a time. Neither the primary account nor the indexing
     * status is guaranteed to arrive, and waiting on either would leave [showIndexingComplete]
     * null. The screen holds the search field's focus back until that has an answer, so the
     * keyboard would never come up.
     *
     * Giving up costs nothing. Nothing is recorded as answered, so the note is offered again on the
     * next visit.
     */
    private suspend fun readNoteToShow(): Boolean? = withTimeoutOrNull(READ_TIMEOUT_MILLIS.milliseconds) {
        hasIndexingToAnnounce(observePrimaryUserId().filterNotNull().first())
    }

    /**
     * Whether this account has finished indexing without being told yet.
     *
     * The cheap answers come first, and each one that rules the note out ends the read. Only an
     * account that has content search on and has not been told needs its indexing status.
     */
    private suspend fun hasIndexingToAnnounce(userId: UserId): Boolean {
        if (!isContentSearchFeatureEnabled()) return false
        if (!observeContentSearchEnabled(userId).first()) return false
        if (observeContentSearchPromptDismissed(userId, ContentSearchPrompt.IndexingComplete).first()) return false

        return observeContentSearchIndexingStatus(userId).firstOrNull() == ContentIndexingState.Completed
    }

    /** Takes the note away for good: the account is told once and never again. */
    fun dismissIndexingComplete() {
        mutableIndexingComplete.value = false
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            dismissContentSearchPrompt(userId, ContentSearchPrompt.IndexingComplete)
        }
    }

    /**
     * The banner for one account, latched so it survives that account finishing its indexing.
     *
     * The latch belongs to this per-account flow and not to [state]. Kept above, a banner left over
     * from the previous account would carry into the next one, and an account that is already
     * indexed would be announced as still indexing.
     */
    private fun observeBannerFor(userId: UserId): Flow<IndexingProgressState> = observeContentSearchEnabled(userId)
        .distinctUntilChanged()
        .flatMapLatest { enabled ->
            if (enabled && isContentSearchFeatureEnabled()) {
                observeContentSearchIndexingStatus(userId)
            } else {
                flowOf(null)
            }
        }
        .scan<ContentIndexingState?, IndexingProgressState>(IndexingProgressState.Hidden) { current, indexingState ->
            current.nextFrom(indexingState)
        }

    /**
     * The next banner state for an indexing state, where null means content search is off for this
     * account.
     *
     * Once shown, the banner stays for as long as this account is the primary one, even after it is
     * fully indexed.
     */
    private fun IndexingProgressState.nextFrom(indexingState: ContentIndexingState?): IndexingProgressState {
        val lastPercentage = (this as? IndexingProgressState.Ongoing)?.percentage
        return when (indexingState) {
            null -> IndexingProgressState.Hidden

            ContentIndexingState.Initializing -> IndexingProgressState.Visible(percentage = 0)

            is ContentIndexingState.Running -> IndexingProgressState.Visible(indexingState.percentage.toBanner())

            ContentIndexingState.Completed -> if (lastPercentage != null) FullyIndexed else IndexingProgressState.Hidden

            ContentIndexingState.Failed ->
                lastPercentage?.let { IndexingProgressState.Error(it) } ?: IndexingProgressState.Hidden

            // Paused, idle, cancelled: nothing to report that the settings screen does not say better.
            // A banner already on screen freezes at its last percentage rather than vanishing.
            is ContentIndexingState.WaitingForUnmeteredConnection,
            is ContentIndexingState.Paused,
            ContentIndexingState.Idle,
            ContentIndexingState.Cancelled -> this
        }
    }

    private fun Double.toBanner(): Int = floor(this).toInt().coerceIn(0, FullyIndexed.percentage - 1)

    private companion object {

        val FullyIndexed = IndexingProgressState.Visible(percentage = 100)

        // Long enough for a healthy read, short enough that a broken one does not hold the keyboard
        // back for noticeably long.
        const val READ_TIMEOUT_MILLIS = 2_000L
    }
}
