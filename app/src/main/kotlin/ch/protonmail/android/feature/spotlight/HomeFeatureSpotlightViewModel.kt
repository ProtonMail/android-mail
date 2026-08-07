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

package ch.protonmail.android.feature.spotlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsFeatureSpotlightEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.usecase.IsRecentAppInstall
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeFeatureSpotlightViewModel @Inject constructor(
    observeFeatureSpotlightDisplay: ObserveFeatureSpotlightDisplay,
    @IsFeatureSpotlightEnabled private val isEnabled: FeatureFlag<Boolean>,
    // The spotlight showcases content search, so there is nothing to show when the feature is unavailable.
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val isRecentAppInstall: IsRecentAppInstall,
    private val markFeatureSpotlightSeen: MarkFeatureSpotlightSeen,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    // Re-evaluate on primary user change, so the spotlight is only considered once a user is signed in.
    val state: StateFlow<FeatureSpotlightState> = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            flow {
                if (!isEnabled.get() || !isContentSearchFeatureEnabled(userId)) {
                    emit(FeatureSpotlightState.Hide)
                } else if (isRecentAppInstall()) {
                    markFeatureSpotlightSeen()
                    emit(FeatureSpotlightState.Hide)
                } else {
                    emitAll(
                        observeFeatureSpotlightDisplay().map { preferenceEither ->
                            preferenceEither.fold(
                                ifLeft = { FeatureSpotlightState.Hide },
                                ifRight = { preference ->
                                    if (preference.show) {
                                        FeatureSpotlightState.Show
                                    } else {
                                        FeatureSpotlightState.Hide
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = FeatureSpotlightState.Loading
        )
}
