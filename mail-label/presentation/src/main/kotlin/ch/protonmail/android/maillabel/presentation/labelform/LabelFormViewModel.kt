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

package ch.protonmail.android.maillabel.presentation.labelform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.usecase.CreateLabel
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.maillabel.presentation.getLabelColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

@HiltViewModel
class LabelFormViewModel @Inject constructor(
    private val getLabel: GetLabel,
    private val createLabel: CreateLabel,
    private val updateLabel: UpdateLabel,
    private val deleteLabel: DeleteLabel,
    private val reducer: LabelFormReducer,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialState: LabelFormState = LabelFormState.Loading
    private val mutableState = MutableStateFlow<LabelFormState>(LabelFormState.Loading)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<LabelFormState> = mutableState

    init {
        val labelId = savedStateHandle.get<String>(LabelFormScreen.LabelIdKey)
        viewModelScope.launch {
            if (labelId != null) {
                getLabel(userId = primaryUserId(), labelId = LabelId(labelId)).getOrNull()?.let { label ->
                    emitNewStateFor(
                        LabelFormEvent.LabelLoaded(
                            labelId = label.labelId,
                            name = label.name,
                            color = label.color
                        )
                    )
                }
            } else {
                emitNewStateFor(
                    LabelFormEvent.LabelLoaded(
                        labelId = null,
                        name = "",
                        color = getLabelColors().random().getHexStringFromColor()
                    )
                )
            }
        }
    }

    internal fun submit(action: LabelFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is LabelFormViewAction.LabelColorChanged -> emitNewStateFor(action)
                    is LabelFormViewAction.LabelNameChanged -> emitNewStateFor(action)
                    LabelFormViewAction.OnCloseLabelForm -> emitNewStateFor(action)
                    LabelFormViewAction.OnDeleteClick -> handleOnDeleteClick()
                    LabelFormViewAction.OnSaveClick -> handleOnSaveClick()
                }
            }
        }
    }

    private suspend fun handleOnSaveClick() {
        when (val currentState = state.value) {
            is LabelFormState.Create -> {
                createLabel(currentState.name, currentState.color)
            }
            is LabelFormState.Update -> {
                editLabel(currentState.labelId, currentState.name, currentState.color)
            }
            LabelFormState.Loading -> {}
        }
    }

    private suspend fun handleOnDeleteClick() {
        val currentState = state.value
        if (currentState is LabelFormState.Update) {
            deleteLabel(currentState.labelId)
        }
    }

    private suspend fun createLabel(name: String, color: String) {
        createLabel(primaryUserId(), name, color)
        emitNewStateFor(LabelFormEvent.LabelCreated)
    }

    private suspend fun editLabel(labelId: LabelId, name: String, color: String) {
        getLabel(primaryUserId(), labelId).getOrNull()?.let { label ->
            updateLabel(
                primaryUserId(),
                label.copy(
                    name = name,
                    color = color
                )
            )
        }
        emitNewStateFor(LabelFormEvent.LabelUpdated)
    }

    private suspend fun deleteLabel(labelId: LabelId) {
        deleteLabel(primaryUserId(), labelId)
        emitNewStateFor(LabelFormEvent.LabelDeleted)
    }

    private suspend fun primaryUserId() = primaryUserId.first()


    private fun emitNewStateFor(operation: LabelFormOperation) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, operation)
    }
}
