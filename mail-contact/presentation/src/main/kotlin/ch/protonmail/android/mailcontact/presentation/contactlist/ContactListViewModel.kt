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

package ch.protonmail.android.mailcontact.presentation.contactlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModelMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val observeContacts: ObserveContacts,
    private val observeContactGroups: ObserveContactGroups,
    private val reducer: ContactListReducer,
    private val contactListItemUiModelMapper: ContactListItemUiModelMapper,
    private val contactGroupItemUiModelMapper: ContactGroupItemUiModelMapper,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val initialState: ContactListState = ContactListState.Loading()
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<ContactListState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            flowContactListEvent(userId = primaryUserId())
                .onEach { contactListEvent -> emitNewStateFor(contactListEvent) }
                .launchIn(viewModelScope)
        }
    }

    internal fun submit(action: ContactListViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ContactListViewAction.OnOpenBottomSheet -> emitNewStateFor(ContactListEvent.OpenBottomSheet)
                    ContactListViewAction.OnDismissBottomSheet -> emitNewStateFor(ContactListEvent.DismissBottomSheet)
                    ContactListViewAction.OnNewContactClick -> emitNewStateFor(ContactListEvent.OpenContactForm)
                    ContactListViewAction.OnNewContactGroupClick -> emitNewStateFor(
                        ContactListEvent.OpenContactGroupForm
                    )
                    ContactListViewAction.OnImportContactClick -> emitNewStateFor(ContactListEvent.OpenImportContact)
                }
            }
        }
    }

    private fun flowContactListEvent(userId: UserId): Flow<ContactListEvent> {
        return combine(
            observeContacts(userId),
            observeContactGroups(userId)
        ) { contacts, contactGroups ->
            val contactList = contacts.getOrElse {
                Timber.e("Error while observing contacts")
                return@combine ContactListEvent.ErrorLoadingContactList
            }
            ContactListEvent.ContactListLoaded(
                contactList = contactListItemUiModelMapper.toContactListItemUiModel(
                    contactList
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    contactList,
                    contactGroups.getOrElse {
                        Timber.e("Error while observing contact groups")
                        return@combine ContactListEvent.ErrorLoadingContactList
                    }
                )
            )
        }
    }

    private fun emitNewStateFor(event: ContactListEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    private suspend fun primaryUserId() = primaryUserId.first()
}
