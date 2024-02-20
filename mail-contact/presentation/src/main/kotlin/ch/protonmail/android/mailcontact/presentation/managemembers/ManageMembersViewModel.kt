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

package ch.protonmail.android.mailcontact.presentation.managemembers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModelMapper
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ManageMembersViewModel @Inject constructor(
    private val observeContacts: ObserveContacts,
    private val reducer: ManageMembersReducer,
    private val manageMembersUiModelMapper: ManageMembersUiModelMapper,
    private val savedStateHandle: SavedStateHandle,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ManageMembersState> = mutableState

    init {
        viewModelScope.launch {
            flowManageMembersEvent(
                userId = primaryUserId(),
                selectedContactEmailIds = extractSelectedContactEmailIds() ?: emptyList()
            ).onEach { manageMembersEvent -> emitNewStateFor(manageMembersEvent) }.launchIn(viewModelScope)
        }
    }

    internal fun submit(action: ManageMembersViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ManageMembersViewAction.OnCloseClick -> emitNewStateFor(ManageMembersEvent.Close)
                }
            }
        }
    }

    private fun flowManageMembersEvent(
        userId: UserId,
        selectedContactEmailIds: List<String>
    ): Flow<ManageMembersEvent> {
        return observeContacts(userId).map { contacts ->
            ManageMembersEvent.MembersLoaded(
                manageMembersUiModelMapper.toManageMembersUiModelList(
                    contacts = contacts.getOrElse {
                        Timber.e("Error while observing contacts")
                        return@map ManageMembersEvent.LoadMembersError
                    },
                    selectedContactEmailIds = selectedContactEmailIds
                )
            )
        }
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun extractSelectedContactEmailIds() = savedStateHandle.get<List<String>>(
        ManageMembersScreen.ManageMembersSelectedContactEmailIdsKey
    )

    private fun emitNewStateFor(event: ManageMembersEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ManageMembersState = ManageMembersState.Loading()
    }
}
