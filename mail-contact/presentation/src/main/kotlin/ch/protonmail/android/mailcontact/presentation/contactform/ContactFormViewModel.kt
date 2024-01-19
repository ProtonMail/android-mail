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

package ch.protonmail.android.mailcontact.presentation.contactform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.toContactFormUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.contact.domain.entity.ContactId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    private val observeDecryptedContact: ObserveDecryptedContact,
    private val reducer: ContactFormReducer,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialState: ContactFormState = ContactFormState.Loading()
    private val mutableState = MutableStateFlow<ContactFormState>(ContactFormState.Loading())
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val actionMutex = Mutex()

    val state: StateFlow<ContactFormState> = mutableState

    init {
        savedStateHandle.get<String>(ContactFormScreen.ContactFormContactIdKey)?.let { contactId ->
            viewModelScope.launch {
                val contact = observeDecryptedContact(
                    userId = primaryUserId(),
                    contactId = ContactId(contactId)
                ).first().getOrElse {
                    Timber.e("Error while getting contact")
                    return@launch emitNewStateFor(ContactFormEvent.LoadContactError)
                }
                emitNewStateFor(
                    ContactFormEvent.EditContact(
                        contact.toContactFormUiModel()
                    )
                )
            }
        } ?: run {
            emitNewStateFor(
                ContactFormEvent.NewContact(
                    ContactFormUiModel(id = null)
                )
            )
        }
    }

    internal fun submit(action: ContactFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    ContactFormViewAction.OnCloseContactFormClick -> emitNewStateFor(ContactFormEvent.CloseContactForm)
                }
            }
        }
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ContactFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
