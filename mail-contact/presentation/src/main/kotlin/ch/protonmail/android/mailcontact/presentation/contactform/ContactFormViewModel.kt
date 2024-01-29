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
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section
import ch.protonmail.android.mailcontact.presentation.model.emptyAddressField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.emptyEmailField
import ch.protonmail.android.mailcontact.presentation.model.emptyNoteField
import ch.protonmail.android.mailcontact.presentation.model.emptyRandomOtherField
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
                    Timber.e("Error while getting contact in init")
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
                    is ContactFormViewAction.OnAddItemClick -> handleAddItem(action)
                    is ContactFormViewAction.OnRemoveItemClick -> handleRemoveItem(action)
                    is ContactFormViewAction.OnUpdateItem -> handleUpdateItem(action)
                    is ContactFormViewAction.OnUpdateDisplayName -> handleDisplayName(action)
                    is ContactFormViewAction.OnUpdateFirstName -> handleFirstName(action)
                    is ContactFormViewAction.OnUpdateLastName -> handleLastName(action)
                    ContactFormViewAction.OnCloseContactFormClick -> emitNewStateFor(ContactFormEvent.CloseContactForm)
                    ContactFormViewAction.OnSaveClick -> handleSave()
                }
            }
        }
    }

    private fun handleAddItem(action: ContactFormViewAction.OnAddItemClick) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                when (action.section) {
                    Section.Emails -> {
                        // We need to make a new list here through `toMutableList` so that it triggers recomposition
                        val newEmails = contact.emails.toMutableList()
                        newEmails.add(emptyEmailField)
                        contact.copy(emails = newEmails)
                    }
                    Section.Telephones -> {
                        val newTelephones = contact.telephones.toMutableList()
                        newTelephones.add(emptyTelephoneField)
                        contact.copy(telephones = newTelephones)
                    }
                    Section.Addresses -> {
                        val newAddresses = contact.addresses.toMutableList()
                        newAddresses.add(emptyAddressField)
                        contact.copy(addresses = newAddresses)
                    }
                    Section.Notes -> {
                        val newNotes = contact.notes.toMutableList()
                        newNotes.add(emptyNoteField)
                        contact.copy(notes = newNotes)
                    }
                    Section.Others -> {
                        val newOthers = contact.others.toMutableList()
                        newOthers.add(emptyRandomOtherField())
                        contact.copy(others = newOthers)
                    }
                }
            )
        )
    }

    private fun handleRemoveItem(action: ContactFormViewAction.OnRemoveItemClick) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                when (action.section) {
                    Section.Emails -> {
                        // We need to make a new list here through `toMutableList` so that it triggers recomposition
                        val newEmails = contact.emails.toMutableList()
                        newEmails.removeAt(action.index)
                        contact.copy(emails = newEmails)
                    }
                    Section.Telephones -> {
                        val newTelephones = contact.telephones.toMutableList()
                        newTelephones.removeAt(action.index)
                        contact.copy(telephones = newTelephones)
                    }
                    Section.Addresses -> {
                        val newAddresses = contact.addresses.toMutableList()
                        newAddresses.removeAt(action.index)
                        contact.copy(addresses = newAddresses)
                    }
                    Section.Notes -> {
                        val newNotes = contact.notes.toMutableList()
                        newNotes.removeAt(action.index)
                        contact.copy(notes = newNotes)
                    }
                    Section.Others -> {
                        val newOthers = contact.others.toMutableList()
                        newOthers.removeAt(action.index)
                        contact.copy(others = newOthers)
                    }
                }
            )
        )
    }

    private fun handleUpdateItem(action: ContactFormViewAction.OnUpdateItem) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                when (action.section) {
                    Section.Emails -> {
                        // We don't want to trigger recomposition here so we just update the item at specified index
                        //  without making a new list.
                        val mutableEmails = contact.emails.apply {
                            this[action.index] = action.newValue as InputField.SingleTyped
                        }
                        contact.copy(emails = mutableEmails)
                    }
                    Section.Telephones -> {
                        val mutableTelephones = contact.telephones.apply {
                            this[action.index] = action.newValue as InputField.SingleTyped
                        }
                        contact.copy(telephones = mutableTelephones)
                    }
                    Section.Addresses -> {
                        val mutableAddresses = contact.addresses.apply {
                            this[action.index] = action.newValue as InputField.Address
                        }
                        contact.copy(addresses = mutableAddresses)
                    }
                    Section.Notes -> {
                        val mutableNotes = contact.notes.apply {
                            this[action.index] = action.newValue as InputField.Note
                        }
                        contact.copy(notes = mutableNotes)
                    }
                    Section.Others -> {
                        val mutableOthers = contact.others.apply {
                            this[action.index] = action.newValue
                        }
                        contact.copy(others = mutableOthers)
                    }
                }
            )
        )
    }

    private fun handleDisplayName(action: ContactFormViewAction.OnUpdateDisplayName) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                contact.copy(displayName = action.displayName)
            )
        )
    }

    private fun handleFirstName(action: ContactFormViewAction.OnUpdateFirstName) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                contact.copy(firstName = action.firstName)
            )
        )
    }

    private fun handleLastName(action: ContactFormViewAction.OnUpdateLastName) {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        val contact = stateValue.contact
        emitNewStateFor(
            ContactFormEvent.UpdateContactForm(
                contact.copy(lastName = action.lastName)
            )
        )
    }

    private suspend fun handleSave() {
        val stateValue = state.value
        if (stateValue !is ContactFormState.Data) return
        when (stateValue) {
            is ContactFormState.Data.Create -> handleCreateContact(stateValue.contact)
            is ContactFormState.Data.Update -> handleUpdateContact(stateValue.contact)
        }
    }

    private fun handleCreateContact(contact: ContactFormUiModel) {
        contactFormUiModelMapper.toDecryptedContact(
            contact = contact
        )
        emitNewStateFor(ContactFormEvent.CreatingContact)

        // Call save UC with mapping result as param here

        emitNewStateFor(ContactFormEvent.ContactCreated)
    }

    private suspend fun handleUpdateContact(contact: ContactFormUiModel) {
        val contactId = contact.id ?: run {
            return emitNewStateFor(ContactFormEvent.SaveContactError)
        }
        val decryptedContact = observeDecryptedContact(
            userId = primaryUserId(),
            contactId = contactId
        ).first().getOrElse {
            Timber.e("Error while getting contact in handleSave")
            return emitNewStateFor(ContactFormEvent.SaveContactError)
        }
        contactFormUiModelMapper.toDecryptedContact(
            contact = contact,
            contactGroups = decryptedContact.contactGroups,
            photos = decryptedContact.photos,
            logos = decryptedContact.logos
        )

        // Call save UC with mapping result as param here

        emitNewStateFor(ContactFormEvent.ContactUpdated)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ContactFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
