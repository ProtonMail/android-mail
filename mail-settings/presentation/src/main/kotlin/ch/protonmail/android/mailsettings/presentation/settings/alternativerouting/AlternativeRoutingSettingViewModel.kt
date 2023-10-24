/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.alternativerouting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAlternativeRoutingSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveAlternativeRoutingSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class AlternativeRoutingSettingViewModel @Inject constructor(
    observeAlternativeRoutingSetting: ObserveAlternativeRoutingSetting,
    private val saveAlternativeRoutingSetting: SaveAlternativeRoutingSetting
) : ViewModel() {

    private val alternativeRoutingSettingErrorFlow: MutableStateFlow<Effect<Unit>> = MutableStateFlow(Effect.empty())

    val state: Flow<AlternativeRoutingSettingState> = combine(
        observeAlternativeRoutingSetting(),
        alternativeRoutingSettingErrorFlow
    ) { alternativeRoutingPreferenceEither, alternativeRoutingSettingErrorEffect ->
        alternativeRoutingPreferenceEither.fold(
            ifLeft = {
                AlternativeRoutingSettingState.Data(
                    isEnabled = null,
                    alternativeRoutingSettingErrorEffect = Effect.of(Unit)
                )
            },
            ifRight = { alternativeRoutingPreference ->
                AlternativeRoutingSettingState.Data(
                    isEnabled = alternativeRoutingPreference.isEnabled,
                    alternativeRoutingSettingErrorEffect = alternativeRoutingSettingErrorEffect
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = AlternativeRoutingSettingState.Loading
    )

    fun saveAlternativeRoutingPreference(alternativeRoutingPreference: Boolean) = viewModelScope.launch {
        saveAlternativeRoutingSetting(alternativeRoutingPreference)
            .tapLeft { alternativeRoutingSettingErrorFlow.emit(Effect.of(Unit)) }
    }
}
