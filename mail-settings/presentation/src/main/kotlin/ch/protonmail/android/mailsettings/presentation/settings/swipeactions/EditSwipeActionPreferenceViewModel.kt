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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsettings.domain.usecase.UpdateSwipeActionPreference
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class EditSwipeActionPreferenceViewModel @Inject constructor(
    private val editSwipeActionPreferenceUiModelMapper: EditSwipeActionPreferenceUiModelMapper,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference,
    savedStateHandle: SavedStateHandle,
    private val updateSwipeActionPreference: UpdateSwipeActionPreference,
    private val areAdditionalSwipeActionsEnabled: AreAdditionalSwipeActionsEnabled
) : ViewModel() {

    private val swipeActionDirection = run {
        val directionString = requireNotNull(savedStateHandle.get<String>(SWIPE_DIRECTION_KEY)) {
            "Cannot get '$SWIPE_DIRECTION_KEY' parameter from savedStateHandle"
        }
        SwipeActionDirection(directionString)
    }

    val initial = EditSwipeActionPreferenceState.Loading

    val state = observeState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = initial
    )

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                is Action.UpdateSwipeAction -> onUpdateSwipeAction(action.direction, action.swipeAction)
            }.exhaustive
        }
    }

    private suspend fun onUpdateSwipeAction(direction: SwipeActionDirection, swipeAction: SwipeAction) {
        val userId = observePrimaryUserId().first()
            ?: return
        updateSwipeActionPreference(userId, direction, swipeAction)
    }

    private fun observeState(): Flow<EditSwipeActionPreferenceState> = observePrimaryUserId().flatMapLatest { userId ->
        if (userId == null) {
            return@flatMapLatest flowOf(EditSwipeActionPreferenceState.NotLoggedIn)
        }

        observeSwipeActionsPreference(userId).map { preferences ->
            val uiModels = editSwipeActionPreferenceUiModelMapper.toUiModels(
                swipeActionsPreference = preferences,
                swipeActionDirection = swipeActionDirection,
                areAdditionalSwipeActionsEnabled = areAdditionalSwipeActionsEnabled(null)
            )
            EditSwipeActionPreferenceState.Data(uiModels)
        }
    }

    sealed interface Action {

        data class UpdateSwipeAction(val direction: SwipeActionDirection, val swipeAction: SwipeAction) : Action
    }
}
