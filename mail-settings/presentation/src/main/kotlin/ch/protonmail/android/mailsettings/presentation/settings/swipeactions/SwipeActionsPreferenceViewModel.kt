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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceState.Loading
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceState.NotLoggedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import javax.inject.Inject

@HiltViewModel
class SwipeActionsPreferenceViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference,
    private val swipeActionPreferenceUiModelMapper: SwipeActionPreferenceUiModelMapper
) : ViewModel() {

    val initialState = Loading

    val state: StateFlow<SwipeActionsPreferenceState> =
        observeState().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = initialState
        )

    private fun observeState(): Flow<SwipeActionsPreferenceState> =
        accountManager.getPrimaryUserId().flatMapLatest { userId ->
            if (userId == null) flowOf(NotLoggedIn)
            else observeSwipeActionsPreference(userId).mapToState()
        }

    private fun Flow<SwipeActionsPreference>.mapToState() =
        map { SwipeActionsPreferenceState.Data(swipeActionPreferenceUiModelMapper.toUiModel(it)) }
}
