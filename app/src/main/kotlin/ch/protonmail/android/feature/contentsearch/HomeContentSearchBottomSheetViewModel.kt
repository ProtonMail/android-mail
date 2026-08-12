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

package ch.protonmail.android.feature.contentsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.domain.usecase.HasShownContentSearchBottomSheet
import ch.protonmail.android.mailcontentsearch.domain.usecase.MarkContentSearchBottomSheetShown
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ShouldShowContentSearchBottomSheet
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeContentSearchBottomSheetViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val shouldShowContentSearchBottomSheet: ShouldShowContentSearchBottomSheet,
    private val hasShownContentSearchBottomSheet: HasShownContentSearchBottomSheet,
    private val markContentSearchBottomSheetShown: MarkContentSearchBottomSheetShown
) : ViewModel() {

    // Bumped after the sheet is marked shown so the flow re-reads hasShown() and stops emitting Show.
    // Without this the state stays Show for the whole session and the interstitial re-fires on every
    // configuration change (rotation). See ET-6707.
    private val refreshTrigger = MutableStateFlow(0)

    val state: StateFlow<ContentSearchBottomSheetState> =
        combine(observePrimaryUserId().filterNotNull(), refreshTrigger) { userId, _ -> userId }
            .flatMapLatest { userId ->
                flow {
                    if (!isContentSearchFeatureEnabled(userId) || hasShownContentSearchBottomSheet()) {
                        emit(ContentSearchBottomSheetState.Hide)
                        return@flow
                    }

                    emitAll(
                        observeContentSearchEnabled(userId).map { enabled ->
                            if (enabled && shouldShowContentSearchBottomSheet(userId)) {
                                ContentSearchBottomSheetState.Show
                            } else {
                                ContentSearchBottomSheetState.Hide
                            }
                        }
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = ContentSearchBottomSheetState.Loading
            )

    fun markShown() {
        viewModelScope.launch {
            markContentSearchBottomSheetShown()
            refreshTrigger.update { it + 1 }
        }
    }
}
