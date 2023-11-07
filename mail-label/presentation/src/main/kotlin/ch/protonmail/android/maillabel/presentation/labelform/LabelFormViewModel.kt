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
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.NewLabel
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

    private val actionMutex = Mutex()
    private val mutableState = MutableStateFlow(LabelFormState.initial())
    private val primaryUserId = observePrimaryUserId().filterNotNull()

    val state: StateFlow<LabelFormState> = mutableState

    init {
        val labelId = savedStateHandle.get<String>(LabelFormScreen.LabelIdKey)
        viewModelScope.launch {
            if (labelId != null) {
                getLabel(userId = primaryUserId(), labelId = LabelId(labelId)).getOrNull()?.let { label ->
                    mutableState.emit(
                        state.value.copy(
                            isLoading = false,
                            isSaveEnabled = true,
                            label = label
                        )
                    )
                }
            } else {
                mutableState.emit(
                    state.value.copy(
                        isLoading = false,
                        newLabel = buildNewLabel(
                            getLabelColors().random().getHexStringFromColor()
                        )
                    )
                )
            }
        }
    }

    internal fun submit(action: LabelFormAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is LabelFormAction.LabelColorChanged -> emitNewStateFor(action)
                    is LabelFormAction.LabelNameChanged -> emitNewStateFor(action)
                    LabelFormAction.OnCloseLabelForm -> emitNewStateFor(action)
                    LabelFormAction.OnDeleteClick -> handleOnDeleteClick()
                    LabelFormAction.OnSaveClick -> handleOnSaveClick()
                }
            }
        }
    }

    private suspend fun handleOnSaveClick() {
        val currentState = state.value
        if (currentState.newLabel != null) {
            createLabel(currentState.newLabel)
        } else if (currentState.label != null) {
            editLabel(currentState.label)
        }
    }

    private suspend fun handleOnDeleteClick() {
        state.value.label?.labelId?.let {
            deleteLabel(it)
        }
    }

    private suspend fun createLabel(newLabel: NewLabel) {
        createLabel(primaryUserId(), newLabel)
        emitNewStateFor(LabelFormEvent.LabelCreated)
    }

    private suspend fun editLabel(label: Label) {
        updateLabel(primaryUserId(), label)
        emitNewStateFor(LabelFormEvent.LabelUpdated)
    }

    private suspend fun deleteLabel(labelId: LabelId) {
        deleteLabel(primaryUserId(), labelId)
        emitNewStateFor(LabelFormEvent.LabelDeleted)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun buildNewLabel(color: String): NewLabel {
        return NewLabel(
            parentId = null,
            name = "",
            type = LabelType.MessageLabel,
            color = color,
            isNotified = null,
            isExpanded = null,
            isSticky = null
        )
    }

    private fun emitNewStateFor(operation: LabelFormOperation) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, operation)
    }
}
