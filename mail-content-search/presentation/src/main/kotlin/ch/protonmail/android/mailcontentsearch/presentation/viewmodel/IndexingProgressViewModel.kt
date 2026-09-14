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
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchIndexingStatus
import ch.protonmail.android.mailcontentsearch.presentation.model.IndexingProgressState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.math.floor

/**
 * The indexing progress the search screen shows for the primary account.
 *
 * The banner is only for an account whose backfill is actually running. Content search switched off
 * by the user, unavailable on this build, or unavailable on a 32-bit device all read as nothing to
 * show. Failures are not reported here: the settings screen owns them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IndexingProgressViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val observeContentSearchIndexingStatus: ObserveContentSearchIndexingStatus
) : ViewModel() {

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
        val isVisible = this is IndexingProgressState.Visible
        return when (indexingState) {
            null -> IndexingProgressState.Hidden

            ContentIndexingState.Initializing -> IndexingProgressState.Visible(percentage = 0)

            is ContentIndexingState.Running -> IndexingProgressState.Visible(indexingState.percentage.toBanner())

            ContentIndexingState.Completed -> if (isVisible) FullyIndexed else IndexingProgressState.Hidden

            // Paused, idle, cancelled, failed: nothing to report that the settings screen does not say
            // better. A banner already on screen freezes at its last percentage rather than vanishing.
            is ContentIndexingState.WaitingForUnmeteredConnection,
            is ContentIndexingState.Paused,
            ContentIndexingState.Idle,
            ContentIndexingState.Cancelled,
            ContentIndexingState.Failed -> this
        }
    }

    private fun Double.toBanner(): Int = floor(this).toInt().coerceIn(0, FullyIndexed.percentage - 1)

    private companion object {

        val FullyIndexed = IndexingProgressState.Visible(percentage = 100)
    }
}
