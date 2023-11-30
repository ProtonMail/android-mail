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
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.usecase.CreateLabel
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.domain.usecase.IsLabelLimitReached
import ch.protonmail.android.maillabel.domain.usecase.IsLabelNameAllowed
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.equalsNoCase
import javax.inject.Inject

@HiltViewModel
class LabelFormViewModel @Inject constructor(
    private val getLabel: GetLabel,
    private val createLabel: CreateLabel,
    private val updateLabel: UpdateLabel,
    private val deleteLabel: DeleteLabel,
    private val getLabelColors: GetLabelColors,
    private val isLabelNameAllowed: IsLabelNameAllowed,
    private val isLabelLimitReached: IsLabelLimitReached,
    private val reducer: LabelFormReducer,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialState: LabelFormState = LabelFormState.Loading()
    private val mutableState = MutableStateFlow<LabelFormState>(LabelFormState.Loading())
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<LabelFormState> = mutableState

    init {
        val labelId = savedStateHandle.get<String>(LabelFormScreen.LabelIdKey)
        viewModelScope.launch {
            val colors = getLabelColors().map {
                it.getColorFromHexString()
            }
            if (labelId != null) {
                getLabel(userId = primaryUserId(), labelId = LabelId(labelId)).getOrNull()?.let { label ->
                    emitNewStateFor(
                        LabelFormEvent.LabelLoaded(
                            labelId = label.labelId,
                            name = label.name,
                            color = label.color,
                            colorList = colors
                        )
                    )
                }
            } else {
                emitNewStateFor(
                    LabelFormEvent.LabelLoaded(
                        labelId = null,
                        name = "",
                        color = colors.random().getHexStringFromColor(),
                        colorList = colors
                    )
                )
            }
        }
    }

    internal fun submit(action: LabelFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is LabelFormViewAction.LabelColorChanged -> emitNewStateFor(
                        LabelFormEvent.UpdateLabelColor(action.color.getHexStringFromColor())
                    )
                    is LabelFormViewAction.LabelNameChanged -> emitNewStateFor(
                        LabelFormEvent.UpdateLabelName(action.name)
                    )
                    LabelFormViewAction.OnCloseLabelFormClick -> emitNewStateFor(LabelFormEvent.CloseLabelForm)
                    LabelFormViewAction.OnDeleteClick -> handleOnDeleteClick()
                    LabelFormViewAction.OnSaveClick -> handleOnSaveClick()
                }
            }
        }
    }

    private suspend fun handleOnSaveClick() {
        when (val currentState = state.value) {
            is LabelFormState.Data.Create -> {
                createLabel(currentState.name, currentState.color)
            }
            is LabelFormState.Data.Update -> {
                editLabel(currentState.labelId, currentState.name, currentState.color)
            }
            is LabelFormState.Loading -> {}
        }
    }

    private suspend fun handleOnDeleteClick() {
        val currentState = state.value
        if (currentState is LabelFormState.Data.Update) {
            deleteLabel(currentState.labelId)
        }
    }

    @SuppressWarnings("ReturnCount")
    private suspend fun createLabel(name: String, color: String) {
        val isLabelLimitReached = isLabelLimitReached(primaryUserId()).getOrElse {
            return emitNewStateFor(LabelFormEvent.SaveLabelError)
        }
        if (isLabelLimitReached) return emitNewStateFor(LabelFormEvent.LabelLimitReached)

        val isLabelNameAllowed = isLabelNameAllowed(primaryUserId(), name).getOrElse {
            return emitNewStateFor(LabelFormEvent.SaveLabelError)
        }
        if (!isLabelNameAllowed) return emitNewStateFor(LabelFormEvent.LabelAlreadyExists)

        createLabel(primaryUserId(), name, color)
        emitNewStateFor(LabelFormEvent.LabelCreated)
    }

    private suspend fun editLabel(
        labelId: LabelId,
        name: String,
        color: String
    ) {
        getLabel(primaryUserId(), labelId).getOrNull()?.let { label ->
            if (!name.equalsNoCase(label.name)) {
                val isLabelNameAllowed = isLabelNameAllowed(primaryUserId(), name).getOrElse {
                    return emitNewStateFor(LabelFormEvent.SaveLabelError)
                }
                if (!isLabelNameAllowed) return emitNewStateFor(LabelFormEvent.LabelAlreadyExists)
            }
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

    private fun emitNewStateFor(event: LabelFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
