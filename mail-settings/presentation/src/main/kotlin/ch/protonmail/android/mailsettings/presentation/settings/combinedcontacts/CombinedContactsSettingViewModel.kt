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

package ch.protonmail.android.mailsettings.presentation.settings.combinedcontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCombinedContactsSetting
import ch.protonmail.android.mailsettings.domain.usecase.SaveCombinedContactsSetting
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
class CombinedContactsSettingViewModel @Inject constructor(
    observeCombinedContactsSetting: ObserveCombinedContactsSetting,
    private val saveCombinedContactsSetting: SaveCombinedContactsSetting
) : ViewModel() {

    private val combinedContactsSettingErrorFlow: MutableStateFlow<Effect<Unit>> = MutableStateFlow(Effect.empty())

    val state: Flow<CombinedContactsSettingState> = combine(
        observeCombinedContactsSetting(),
        combinedContactsSettingErrorFlow
    ) { combinedContactsPreferenceEither, combinedContactsSettingErrorEffect ->
        combinedContactsPreferenceEither.fold(
            ifLeft = {
                CombinedContactsSettingState.Data(
                    isEnabled = null,
                    combinedContactsSettingErrorEffect = Effect.of(Unit)
                )
            },
            ifRight = { combinedContactsPreference ->
                CombinedContactsSettingState.Data(
                    isEnabled = combinedContactsPreference.isEnabled,
                    combinedContactsSettingErrorEffect = combinedContactsSettingErrorEffect
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = CombinedContactsSettingState.Loading
    )

    fun saveCombinedContactsPreference(combinedContactsPreference: Boolean) = viewModelScope.launch {
        saveCombinedContactsSetting(combinedContactsPreference)
            .tapLeft { combinedContactsSettingErrorFlow.emit(Effect.of(Unit)) }
    }
}
