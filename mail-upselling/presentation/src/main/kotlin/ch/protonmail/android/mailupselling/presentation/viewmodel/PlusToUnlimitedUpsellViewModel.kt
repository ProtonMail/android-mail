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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlusToUnlimitedEligibility
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveActiveUpsellTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlusToUnlimitedUpsellViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    observePlusToUnlimitedEligibility: ObservePlusToUnlimitedEligibility,
    resolveActiveUpsellTheme: ResolveActiveUpsellTheme
) : ViewModel() {

    val theme: StateFlow<UpsellContentTheme?> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            observePlusToUnlimitedEligibility(userId).map { eligible ->
                if (eligible) resolveActiveUpsellTheme(userId) else null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = null
        )
}
