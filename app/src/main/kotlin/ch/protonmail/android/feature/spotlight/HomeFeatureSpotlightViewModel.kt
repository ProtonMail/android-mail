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
import ch.protonmail.android.mailsession.domain.usecase.IsCategoryViewEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailspotlight.domain.usecase.IsRecentAppInstall
import ch.protonmail.android.mailspotlight.domain.usecase.MarkFeatureSpotlightSeen
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveFeatureSpotlightDisplay
import ch.protonmail.android.mailspotlight.domain.usecase.ObserveIsBusinessUser
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeFeatureSpotlightViewModel @Inject constructor(
    observeFeatureSpotlightDisplay: ObserveFeatureSpotlightDisplay,
    @IsFeatureSpotlightEnabled private val isEnabled: FeatureFlag<Boolean>,
    // Temporarily couple the 2 FFs as the new feature spotlight depends on the Category View impl in 7.10+
    private val isCategoryViewEnabled: IsCategoryViewEnabled,
    private val isRecentAppInstall: IsRecentAppInstall,
    private val markFeatureSpotlightSeen: MarkFeatureSpotlightSeen,
    private val observeIsBusinessUser: ObserveIsBusinessUser,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    // Re-evaluate on primary user change: this is currently needed only for category view.
    val state: StateFlow<FeatureSpotlightState> = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            flow {
                val featureFlagEnabled = isEnabled.get()
                val categoryViewEnabled = isCategoryViewEnabled(userId)
                val recentInstall = isRecentAppInstall()
                Timber.d(
                    "Spotlight evaluating userId=${userId.id} featureFlagEnabled=$featureFlagEnabled " +
                        "categoryViewEnabled=$categoryViewEnabled recentInstall=$recentInstall"
                )
                when {
                    !featureFlagEnabled -> emit(FeatureSpotlightState.Hide)
                    !categoryViewEnabled -> emit(FeatureSpotlightState.Hide)
                    recentInstall -> {
                        markFeatureSpotlightSeen(userId)
                        emit(FeatureSpotlightState.Hide)
                    }
                    else -> {
                        emitAll(
                            observeFeatureSpotlightDisplay(userId).map { preferenceEither ->
                                preferenceEither.fold(
                                    ifLeft = {
                                        Timber.d("Spotlight Hide: preference read failed error=$it")
                                        FeatureSpotlightState.Hide
                                    },
                                    // Resolve the user type only once the spotlight is eligible, so we
                                    // don't spin up the observation when it's hidden.
                                    ifRight = { preference ->
                                        if (preference.show) {
                                            Timber.d("Spotlight Show: onboarding eligible for userId=${userId.id}")
                                            FeatureSpotlightState.Show(resolveUserType())
                                        } else {
                                            Timber.d(
                                                "Spotlight Hide: already seen for userId=${userId.id}"
                                            )
                                            FeatureSpotlightState.Hide
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = FeatureSpotlightState.Loading
        )

    private suspend fun resolveUserType(): SpotlightUserType = observeIsBusinessUser().first().fold(
        ifLeft = {
            Timber.d("resolveUserType: unable to resolve user type, fall back to B2C")
            SpotlightUserType.B2C
        },
        ifRight = { isBusiness -> if (isBusiness) SpotlightUserType.B2B else SpotlightUserType.B2C }
    )

}
