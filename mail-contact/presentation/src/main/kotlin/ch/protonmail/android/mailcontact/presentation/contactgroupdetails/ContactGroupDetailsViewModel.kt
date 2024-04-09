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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.DeleteContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsUiModelMapper
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
class ContactGroupDetailsViewModel @Inject constructor(
    private val observeContactGroup: ObserveContactGroup,
    private val reducer: ContactGroupDetailsReducer,
    private val contactGroupDetailsUiModelMapper: ContactGroupDetailsUiModelMapper,
    private val savedStateHandle: SavedStateHandle,
    private val deleteContactGroup: DeleteContactGroup,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactGroupDetailsState> = mutableState

    init {
        extractLabelId()?.let { labelId ->
            viewModelScope.launch {
                flowContactGroupDetailsEvent(userId = primaryUserId(), labelId = LabelId(labelId))
                    .onEach { contactGroupDetailsEvent -> emitNewStateFor(contactGroupDetailsEvent) }
                    .launchIn(viewModelScope)
            }
        } ?: run {
            Timber.e("Error labelId was null in ContactGroupDetailsViewModel init")
            emitNewStateFor(ContactGroupDetailsEvent.LoadContactGroupError)
        }
    }

    internal fun submit(action: ContactGroupDetailsViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ContactGroupDetailsViewAction.OnCloseClick -> emitNewStateFor(
                        ContactGroupDetailsEvent.CloseContactGroupDetails
                    )
                    ContactGroupDetailsViewAction.OnEmailClick -> handleOnEmailClick()
                    ContactGroupDetailsViewAction.OnDeleteClick -> handleOnDeleteClick()
                    ContactGroupDetailsViewAction.OnDeleteConfirmedClick -> handleOnDeleteConfirmedClick()
                    ContactGroupDetailsViewAction.OnDeleteDismissedClick -> handleOnDeleteDismissedClick()
                }
            }
        }
    }

    private fun handleOnEmailClick() {
        val currentState = state.value
        if (currentState !is ContactGroupDetailsState.Data) return
        emitNewStateFor(
            ContactGroupDetailsEvent.ComposeEmail(
                currentState.contactGroup.members.map { it.email }
            )
        )
    }

    private fun handleOnDeleteClick() {
        emitNewStateFor(
            ContactGroupDetailsEvent.ShowDeleteDialog
        )
    }

    private fun handleOnDeleteConfirmedClick() {
        val currentState = state.value
        if (currentState !is ContactGroupDetailsState.Data) return

        viewModelScope.launch {
            deleteContactGroup(
                userId = primaryUserId(),
                labelId = currentState.contactGroup.id
            ).getOrElse {
                return@launch emitNewStateFor(ContactGroupDetailsEvent.DeletingError)
            }

            emitNewStateFor(ContactGroupDetailsEvent.DeletingSuccess)
        }
    }

    private fun handleOnDeleteDismissedClick() {
        val currentState = state.value
        if (currentState !is ContactGroupDetailsState.Data) return

        emitNewStateFor(ContactGroupDetailsEvent.DismissDeleteDialog)
    }

    private fun flowContactGroupDetailsEvent(userId: UserId, labelId: LabelId): Flow<ContactGroupDetailsEvent> {
        return observeContactGroup(userId, labelId).map { contactGroup ->
            ContactGroupDetailsEvent.ContactGroupLoaded(
                contactGroupDetailsUiModelMapper.toContactGroupDetailsUiModel(
                    contactGroup.getOrElse {
                        Timber.e("Error while observing contact group by id")
                        return@map ContactGroupDetailsEvent.LoadContactGroupError
                    }
                )
            )
        }
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun extractLabelId() = savedStateHandle.get<String>(ContactGroupDetailsScreen.ContactGroupDetailsLabelIdKey)

    private fun emitNewStateFor(event: ContactGroupDetailsEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ContactGroupDetailsState = ContactGroupDetailsState.Loading()
    }
}
