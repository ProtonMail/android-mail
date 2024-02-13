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

package ch.protonmail.android.mailcontact.presentation.contacgroupform

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyContactGroupFormUiModel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.presentation.getColorFromHexString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactGroupFormViewModel @Inject constructor(
    private val observeContactGroup: ObserveContactGroup,
    private val reducer: ContactGroupFormReducer,
    private val contactGroupFormUiModelMapper: ContactGroupFormUiModelMapper,
    private val savedStateHandle: SavedStateHandle,
    getLabelColors: GetLabelColors,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactGroupFormState> = mutableState

    init {
        val colors = getLabelColors().map {
            it.getColorFromHexString()
        }
        extractLabelId()?.let { labelId ->
            viewModelScope.launch {
                flowContactGroupFormEvent(userId = primaryUserId(), labelId = LabelId(labelId), colors = colors)
                    .onEach { contactGroupFormEvent -> emitNewStateFor(contactGroupFormEvent) }
                    .launchIn(viewModelScope)
            }
        } ?: run {
            emitNewStateFor(
                ContactGroupFormEvent.ContactGroupLoaded(
                    contactGroupFormUiModel = emptyContactGroupFormUiModel(colors)
                )
            )
        }
    }

    internal fun submit(action: ContactGroupFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ContactGroupFormViewAction.OnCloseClick -> emitNewStateFor(
                        ContactGroupFormEvent.Close
                    )
                }
            }
        }
    }

    private fun flowContactGroupFormEvent(
        userId: UserId,
        labelId: LabelId,
        colors: List<Color>
    ): Flow<ContactGroupFormEvent> {
        return observeContactGroup(userId, labelId).map { contactGroup ->
            ContactGroupFormEvent.ContactGroupLoaded(
                contactGroupFormUiModelMapper.toContactGroupFormUiModel(
                    contactGroup = contactGroup.getOrElse {
                        Timber.e("Error while observing contact group by id")
                        return@map ContactGroupFormEvent.LoadError
                    },
                    colors = colors
                )
            )
        }
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun extractLabelId() = savedStateHandle.get<String>(ContactGroupFormScreen.ContactGroupFormLabelIdKey)

    private fun emitNewStateFor(event: ContactGroupFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ContactGroupFormState = ContactGroupFormState.Loading()
    }
}
