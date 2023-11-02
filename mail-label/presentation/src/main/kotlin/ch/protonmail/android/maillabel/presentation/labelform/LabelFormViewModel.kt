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
import ch.protonmail.android.test.idlingresources.ComposerIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.accountmanager.domain.AccountManager
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
    private val accountManager: AccountManager,
    private val composerIdlingResource: ComposerIdlingResource,
    private val reducer: LabelFormReducer,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val actionMutex = Mutex()
    private val mutableState = MutableStateFlow<LabelFormState>(LabelFormState.Loading)
    private val primaryUserId = observePrimaryUserId().filterNotNull()

    val state: StateFlow<LabelFormState> = mutableState

    init {
        val labelId = savedStateHandle.get<String>(LabelFormScreen.LabelIdKey)
        viewModelScope.launch {
            if (labelId != null) {
                getLabel(userId = primaryUserId(), labelId = LabelId(labelId)).getOrNull()?.let { label ->
                    mutableState.emit(LabelFormState.EditLabel(label))
                }
            } else {
                mutableState.emit(LabelFormState.CreateLabel(createNewLabel()))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        composerIdlingResource.clear()
    }

    internal fun submit(action: LabelFormAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                composerIdlingResource.increment()
                when (action) {
                    is LabelFormAction.LabelColorChanged -> emitNewStateFor(action)
                    is LabelFormAction.LabelNameChanged -> emitNewStateFor(action)
                    LabelFormAction.OnCloseLabelForm -> TODO()
                    LabelFormAction.OnSaveClick -> {
                        when (val currentState = state.value) {
                            is LabelFormState.CreateLabel -> createLabel(currentState.newLabel)
                            is LabelFormState.EditLabel -> editLabel(currentState.label)
                            LabelFormState.Loading -> {}
                        }
                    }
                    LabelFormAction.OnDeleteClick -> {
                        when (val currentState = state.value) {
                            is LabelFormState.CreateLabel -> {}
                            is LabelFormState.EditLabel -> deleteLabel(currentState.label.labelId)
                            LabelFormState.Loading -> {}
                        }
                    }
                }
                composerIdlingResource.decrement()
            }
        }
    }

    private suspend fun createLabel(newLabel: NewLabel) {
        // TODO Handle create error
        createLabel(primaryUserId(), newLabel)
        emitNewStateFor(LabelFormEvent.LabelCreated)
    }

    private suspend fun editLabel(label: Label) {
        // TODO Handle update error
        updateLabel(primaryUserId(), label)
        emitNewStateFor(LabelFormEvent.LabelUpdated)
    }

    private suspend fun deleteLabel(labelId: LabelId) {
        // TODO Handle update error
        deleteLabel(primaryUserId(), labelId)
        emitNewStateFor(LabelFormEvent.LabelDeleted)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun createNewLabel(): NewLabel {
        return NewLabel(
            parentId = null,
            name = "",
            type = LabelType.MessageLabel,
            color = getLabelColors().random().getHexStringFromColor(),
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
