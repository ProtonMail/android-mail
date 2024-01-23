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
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyAddressField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.emptyDefaultOtherField
import ch.protonmail.android.mailcontact.presentation.model.emptyEmailField
import ch.protonmail.android.mailcontact.presentation.model.emptyNoteField
import ch.protonmail.android.mailcontact.presentation.model.emptyTelephoneField
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
    private val contactFormUiModelMapper: ContactFormUiModelMapper,
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
                val decryptedContact = observeDecryptedContact(
                    userId = primaryUserId(),
                    contactId = ContactId(contactId)
                ).first().getOrElse {
                    Timber.e("Error while getting contact")
                    return@launch emitNewStateFor(ContactFormEvent.LoadContactError)
                }
                emitNewStateFor(
                    ContactFormEvent.EditContact(
                        contactFormUiModelMapper.toContactFormUiModel(decryptedContact)
                    )
                )
            }
        } ?: run {
            emitNewStateFor(
                ContactFormEvent.NewContact(emptyContactFormUiModel)
            )
        }
    }

    internal fun submit(action: ContactFormViewAction) {
        viewModelScope.launch {
            actionMutex.withLock {
                when (action) {
                    is ContactFormOperation.AffectingData -> {
                        val stateValue = state.value
                        if (stateValue !is ContactFormState.Data) return@launch
                        emitNewStateFor(
                            ContactFormEvent.UpdateContactFormUiModel(
                                handleUpdateDataAction(action, stateValue.contact)
                            )
                        )
                    }
                    is ContactFormOperation.AffectingNavigation -> {
                        when (action) {
                            ContactFormViewAction.OnCloseContactFormClick -> {
                                emitNewStateFor(ContactFormEvent.CloseContactForm)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleUpdateDataAction(
        action: ContactFormOperation.AffectingData,
        contact: ContactFormUiModel
    ): ContactFormUiModel {
        return when (action) {
            ContactFormViewAction.OnAddEmailClick -> {
                contact.copy(emails = contact.emails.plus(emptyEmailField))
            }
            ContactFormViewAction.OnAddTelephoneClick -> {
                contact.copy(telephones = contact.telephones.plus(emptyTelephoneField))
            }
            ContactFormViewAction.OnAddAddressClick -> {
                contact.copy(addresses = contact.addresses.plus(emptyAddressField))
            }
            ContactFormViewAction.OnAddNoteClick -> {
                contact.copy(notes = contact.notes.plus(emptyNoteField))
            }
            ContactFormViewAction.OnAddOtherClick -> {
                contact.copy(others = contact.others.plus(emptyDefaultOtherField))
            }
        }
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ContactFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
