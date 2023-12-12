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

package ch.protonmail.android.mailcontact.presentation.contactdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContact
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
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailcontact.presentation.model.toContactDetailsUiModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    private val observeContact: ObserveContact,
    private val reducer: ContactDetailsReducer,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialState: ContactDetailsState = ContactDetailsState.Loading()
    private val mutableState = MutableStateFlow<ContactDetailsState>(ContactDetailsState.Loading())
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactDetailsState> = mutableState

    init {
        savedStateHandle.get<String>(ContactDetailsScreen.ContactDetailsContactIdKey)?.let { contactId ->
            viewModelScope.launch {
                flowContactDetailsEvent(userId = primaryUserId(), contactId = ContactId(contactId))
                    .onEach { contactListEvent -> emitNewStateFor(contactListEvent) }
                    .launchIn(viewModelScope)
            }
        } ?: run {
            Timber.e("Error contactId was null in ContactDetailsViewModel init")
            emitNewStateFor(ContactDetailsEvent.LoadContactError)
        }
    }

    internal fun submit(action: ContactDetailsViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ContactDetailsViewAction.OnCloseContactDetailsClick -> emitNewStateFor(
                        ContactDetailsEvent.CloseContactDetails
                    )
                    ContactDetailsViewAction.OnDeleteClick -> handleOnDeleteClick()
                }
            }
        }
    }

    private fun flowContactDetailsEvent(userId: UserId, contactId: ContactId): Flow<ContactDetailsEvent> {
        return observeContact(userId, contactId).map { contact ->
            val contactDetailsUiModel = contact.getOrElse {
                Timber.e("Error while observing contact")
                return@map ContactDetailsEvent.LoadContactError
            }.toContactDetailsUiModel()
            ContactDetailsEvent.ContactLoaded(
                contactDetailsUiModel
            )
        }
    }

    private suspend fun handleOnDeleteClick() {
        // TODO Call Delete contact UC (make sure call is in worker to support offline)
        emitNewStateFor(ContactDetailsEvent.ContactDeleted)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ContactDetailsEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
