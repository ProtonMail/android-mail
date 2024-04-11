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

package ch.protonmail.android.mailcommon.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.UndoLastOperation
import ch.protonmail.android.mailcommon.presentation.Effect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UndoOperationViewModel @Inject constructor(
    private val undoLastOperation: UndoLastOperation
) : ViewModel() {

    private val mutableState = MutableStateFlow(Initial)
    val state = mutableState.asStateFlow()

    fun submitUndo() = viewModelScope.launch {
        undoLastOperation().fold(
            ifLeft = { mutableState.emit(state.value.copy(undoFailed = Effect.of(Unit))) },
            ifRight = { mutableState.emit(state.value.copy(undoSucceeded = Effect.of(Unit))) }
        )
    }

    data class State(
        val undoSucceeded: Effect<Unit>,
        val undoFailed: Effect<Unit>
    )

    companion object {
        private val Initial = State(Effect.empty(), Effect.empty())
    }
}
