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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModelMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ManageMembersViewModel @Inject constructor(
    private val observeContacts: ObserveContacts,
    private val reducer: ManageMembersReducer,
    private val manageMembersUiModelMapper: ManageMembersUiModelMapper,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ManageMembersState> = mutableState

    fun initViewModelWithData(selectedContactEmailIds: List<ContactEmailId>) {
        viewModelScope.launch {
            emitNewStateFor(
                getManageMembersEvent(
                    userId = primaryUserId(),
                    selectedContactEmailIds = selectedContactEmailIds
                )
            )
        }
    }

    internal fun submit(action: ManageMembersViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ManageMembersViewAction.OnCloseClick -> emitNewStateFor(ManageMembersEvent.Close)
                    ManageMembersViewAction.OnDoneClick -> handleOnDoneClick()
                    is ManageMembersViewAction.OnMemberClick -> handleOnMemberClick(action)
                    is ManageMembersViewAction.OnSearchValueChanged -> handleOnSearchValueChanged(action)
                }
            }
        }
    }

    private fun handleOnMemberClick(action: ManageMembersViewAction.OnMemberClick) {
        val stateValue = state.value
        if (stateValue !is ManageMembersState.Data) return

        val memberIndex = stateValue.members.indexOfFirst {
            it.id == action.contactEmailId
        }
        if (memberIndex < 0) return emitNewStateFor(ManageMembersEvent.ErrorUpdatingMember)

        val newMembers = stateValue.members.toMutableList()
        val currentMember = newMembers[memberIndex]
        newMembers[memberIndex] = currentMember.copy(isSelected = !currentMember.isSelected)
        emitNewStateFor(
            ManageMembersEvent.MembersLoaded(
                members = newMembers
            )
        )
    }

    private fun handleOnSearchValueChanged(action: ManageMembersViewAction.OnSearchValueChanged) {
        val stateValue = state.value
        if (stateValue !is ManageMembersState.Data) return

        val newMembers = stateValue.members.toMutableList()
        newMembers.forEachIndexed { index, member ->
            newMembers[index] = member.copy(
                isDisplayed = member.name.contains(action.searchValue, ignoreCase = true) ||
                    member.email.contains(action.searchValue, ignoreCase = true)
            )
        }
        emitNewStateFor(
            ManageMembersEvent.MembersLoaded(
                members = newMembers
            )
        )
    }

    private suspend fun getManageMembersEvent(
        userId: UserId,
        selectedContactEmailIds: List<ContactEmailId>
    ): ManageMembersEvent {
        val contacts = observeContacts(userId).firstOrNull()?.getOrNull() ?: run {
            Timber.e("Error while observing contacts")
            return ManageMembersEvent.LoadMembersError
        }
        return ManageMembersEvent.MembersLoaded(
            manageMembersUiModelMapper.toManageMembersUiModelList(
                contacts = contacts,
                selectedContactEmailIds = selectedContactEmailIds
            )
        )
    }

    private fun handleOnDoneClick() {
        val stateValue = state.value
        if (stateValue !is ManageMembersState.Data) return

        emitNewStateFor(
            ManageMembersEvent.OnDone(
                selectedContactEmailIds = stateValue.members.mapNotNull { it.takeIf { it.isSelected }?.id }
            )
        )
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ManageMembersEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ManageMembersState = ManageMembersState.Loading()
    }
}
