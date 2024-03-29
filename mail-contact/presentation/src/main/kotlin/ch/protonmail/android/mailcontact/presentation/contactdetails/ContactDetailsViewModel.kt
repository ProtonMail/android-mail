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
import ch.protonmail.android.mailcontact.domain.usecase.DeleteContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModelMapper
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
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    private val observeDecryptedContact: ObserveDecryptedContact,
    private val reducer: ContactDetailsReducer,
    private val contactDetailsUiModelMapper: ContactDetailsUiModelMapper,
    private val deleteContact: DeleteContact,
    private val savedStateHandle: SavedStateHandle,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactDetailsState> = mutableState

    init {
        extractContactId()?.let { contactId ->
            viewModelScope.launch {
                flowContactDetailsEvent(userId = primaryUserId(), contactId = ContactId(contactId))
                    .onEach { contactDetailsEvent -> emitNewStateFor(contactDetailsEvent) }
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
                    ContactDetailsViewAction.OnCloseClick -> emitNewStateFor(ContactDetailsEvent.CloseContactDetails)
                    ContactDetailsViewAction.DeleteRequested -> handleDeleteRequested()
                    ContactDetailsViewAction.DeleteConfirmed -> handleDeleteConfirmed()
                    is ContactDetailsViewAction.OnCallClick -> handleOnCallClick(action.phoneNumber)
                    is ContactDetailsViewAction.OnEmailClick -> handleOnEmailClick(action)
                    is ContactDetailsViewAction.OnLongClick -> handleOnLongClick(action)
                }
            }
        }
    }

    private fun flowContactDetailsEvent(userId: UserId, contactId: ContactId): Flow<ContactDetailsEvent> {
        return observeDecryptedContact(userId, contactId).map { decryptedContact ->
            ContactDetailsEvent.ContactLoaded(
                contactDetailsUiModelMapper.toContactDetailsUiModel(
                    decryptedContact.getOrElse {
                        Timber.e("Error while observing decrypted contact")
                        return@map ContactDetailsEvent.LoadContactError
                    }
                )
            )
        }
    }

    private fun handleDeleteRequested() {
        emitNewStateFor(ContactDetailsEvent.DeleteRequested)
    }

    private suspend fun handleDeleteConfirmed() {
        extractContactId()?.let {
            deleteContact(primaryUserId(), ContactId(it))
            emitNewStateFor(ContactDetailsEvent.DeleteConfirmed)
        }
    }

    private fun handleOnCallClick(phoneNumber: String) {
        emitNewStateFor(ContactDetailsEvent.CallPhoneNumber(phoneNumber))
    }

    private fun handleOnEmailClick(action: ContactDetailsViewAction.OnEmailClick) {
        emitNewStateFor(ContactDetailsEvent.ComposeEmail(action.email))
    }

    private fun handleOnLongClick(action: ContactDetailsViewAction.OnLongClick) {
        emitNewStateFor(ContactDetailsEvent.CopyToClipboard(action.value))
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun extractContactId() = savedStateHandle.get<String>(ContactDetailsScreen.ContactDetailsContactIdKey)

    private fun emitNewStateFor(event: ContactDetailsEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }

    companion object {

        val initialState: ContactDetailsState = ContactDetailsState.Loading()
    }
}
