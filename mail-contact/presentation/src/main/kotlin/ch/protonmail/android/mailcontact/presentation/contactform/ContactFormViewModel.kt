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
import ch.protonmail.android.mailcommon.domain.model.BasicContactInfo
import ch.protonmail.android.mailcommon.domain.model.decode
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.CreateContact
import ch.protonmail.android.mailcontact.domain.usecase.EditContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section
import ch.protonmail.android.mailcontact.presentation.model.emptyAddressField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModelWithInitialFields
import ch.protonmail.android.mailcontact.presentation.model.emptyEmailField
import ch.protonmail.android.mailcontact.presentation.model.emptyNoteField
import ch.protonmail.android.mailcontact.presentation.model.emptyRandomOtherField
import ch.protonmail.android.mailcontact.presentation.model.emptyTelephoneField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.presentation.utils.InputValidationResult
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    private val observeDecryptedContact: ObserveDecryptedContact,
    private val reducer: ContactFormReducer,
    private val contactFormUiModelMapper: ContactFormUiModelMapper,
    private val createContact: CreateContact,
    private val editContact: EditContact,
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
                ).firstOrNull()?.getOrNull() ?: run {
                    Timber.e("Error while getting contact in init")
                    return@launch emitNewStateFor(ContactFormEvent.LoadContactError)
                }
                emitNewStateFor(
                    ContactFormEvent.ContactLoaded(
                        contactFormUiModel = contactFormUiModelMapper.toContactFormUiModel(decryptedContact)
                    )
                )
            }
        } ?: run {
            var contactData = emptyContactFormUiModelWithInitialFields()

            savedStateHandle.get<String>(ContactFormScreen.ContactFormBasicContactInfoKey)
                ?.deserialize<BasicContactInfo>()
                ?.let { basicContactInfo ->
                    val decodedInfo = basicContactInfo.decode()
                    val email = emptyEmailField(contactData.incrementalUniqueFieldId.toString())
                    contactData = contactData.copy(
                        displayName = decodedInfo.contactName ?: "",
                        emails = listOf(email.copy(value = decodedInfo.contactEmail)),
                        incrementalUniqueFieldId = contactData.incrementalUniqueFieldId.plus(1)
                    )
                }

            emitNewStateFor(
                ContactFormEvent.ContactLoaded(
                    contactFormUiModel = contactData
                )
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
                    Section.Emails -> contact.copy(
                        emails = contact.emails.toMutableList().apply {
                            this.add(emptyEmailField(contact.incrementalUniqueFieldId.toString()))
                        },
                        incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                    )
                    Section.Telephones -> contact.copy(
                        telephones = contact.telephones.toMutableList().apply {
                            this.add(emptyTelephoneField(contact.incrementalUniqueFieldId.toString()))
                        },
                        incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                    )
                    Section.Addresses -> contact.copy(
                        addresses = contact.addresses.toMutableList().apply {
                            this.add(emptyAddressField(contact.incrementalUniqueFieldId.toString()))
                        },
                        incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                    )
                    Section.Notes -> contact.copy(
                        notes = contact.notes.toMutableList().apply {
                            this.add(emptyNoteField(contact.incrementalUniqueFieldId.toString()))
                        },
                        incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                    )
                    Section.Others -> contact.copy(
                        others = contact.others.toMutableList().apply {
                            this.add(emptyRandomOtherField(contact.incrementalUniqueFieldId.toString()))
                        },
                        incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                    )
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
                    Section.Emails -> contact.copy(
                        emails = contact.emails.toMutableList().apply {
                            this.removeIf { it.fieldId == action.fieldId }
                        }
                    )
                    Section.Telephones -> contact.copy(
                        telephones = contact.telephones.toMutableList().apply {
                            this.removeIf { it.fieldId == action.fieldId }
                        }
                    )
                    Section.Addresses -> contact.copy(
                        addresses = contact.addresses.toMutableList().apply {
                            this.removeIf { it.fieldId == action.fieldId }
                        }
                    )
                    Section.Notes -> contact.copy(
                        notes = contact.notes.toMutableList().apply {
                            this.removeIf { it.fieldId == action.fieldId }
                        }
                    )
                    Section.Others -> contact.copy(
                        others = contact.others.toMutableList().apply {
                            this.removeIf { it.fieldId == action.fieldId }
                        }
                    )
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
                    Section.Emails -> contact.copy(
                        emails = updateEmail(contact.emails, action.newValue as InputField.SingleTyped)
                    )
                    Section.Telephones -> contact.copy(
                        telephones = updateTelephone(contact.telephones, action.newValue as InputField.SingleTyped)
                    )
                    Section.Addresses -> contact.copy(
                        addresses = updateAddress(contact.addresses, action.newValue as InputField.Address)
                    )
                    Section.Notes -> contact.copy(
                        notes = updateNote(contact.notes, action.newValue as InputField.Note)
                    )
                    Section.Others -> contact.copy(
                        others = updateOther(contact.others, action.newValue)
                    )
                }
            )
        )
    }

    private fun updateEmail(
        emails: List<InputField.SingleTyped>,
        newValue: InputField.SingleTyped
    ): List<InputField.SingleTyped> {
        val index = emails.indexOfFirst { it.fieldId == newValue.fieldId }
        if (index == -1) return emails // This happens when the item is removed
        return emails.toMutableList().apply {
            this[index] = newValue
        }
    }

    private fun updateTelephone(
        telephones: List<InputField.SingleTyped>,
        newValue: InputField.SingleTyped
    ): List<InputField.SingleTyped> {
        val index = telephones.indexOfFirst { it.fieldId == newValue.fieldId }
        if (index == -1) return telephones // This happens when the item is removed
        return telephones.toMutableList().apply {
            this[index] = newValue
        }
    }

    private fun updateAddress(
        addresses: List<InputField.Address>,
        newValue: InputField.Address
    ): List<InputField.Address> {
        val index = addresses.indexOfFirst { it.fieldId == newValue.fieldId }
        if (index == -1) return addresses // This happens when the item is removed
        return addresses.toMutableList().apply {
            this[index] = newValue
        }
    }

    private fun updateNote(notes: List<InputField.Note>, newValue: InputField.Note): List<InputField.Note> {
        val index = notes.indexOfFirst { it.fieldId == newValue.fieldId }
        if (index == -1) return notes // This happens when the item is removed
        return notes.toMutableList().apply {
            this[index] = newValue
        }
    }

    private fun updateOther(others: List<InputField>, newValue: InputField): List<InputField> {
        val index = others.indexOfFirst { it.fieldId == newValue.fieldId }
        if (index == -1) return others // This happens when the item is removed
        return others.toMutableList().apply {
            this[index] = newValue
        }
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

        val containsInvalidEmail = stateValue.contact.emails.any {
            it.value.isNotBlank() && validateEmail(it.value).not()
        }
        if (containsInvalidEmail) return emitNewStateFor(ContactFormEvent.InvalidEmailError)

        emitNewStateFor(ContactFormEvent.SavingContact)

        if (stateValue.contact.id != null) handleUpdateContact(stateValue.contact)
        else handleCreateContact(stateValue.contact)
    }

    private suspend fun handleCreateContact(contact: ContactFormUiModel) {
        val decryptedContact = contactFormUiModelMapper.toDecryptedContact(
            contact = contact,
            contactGroupLabels = listOf(),
            photos = listOf(),
            logos = listOf()
        )

        createContact(
            userId = primaryUserId(),
            decryptedContact = decryptedContact
        ).getOrElse {
            return if (it is CreateContact.CreateContactErrors.MaximumNumberOfContactsReached) {
                emitNewStateFor(ContactFormEvent.SaveContactError.ContactLimitReached)
            } else emitNewStateFor(ContactFormEvent.SaveContactError.Generic)
        }

        emitNewStateFor(ContactFormEvent.ContactCreated)
    }

    private suspend fun handleUpdateContact(contact: ContactFormUiModel) {
        val contactId = contact.id ?: run {
            return emitNewStateFor(ContactFormEvent.SaveContactError.Generic)
        }
        val decryptedContact = observeDecryptedContact(
            userId = primaryUserId(),
            contactId = contactId
        ).firstOrNull()?.getOrNull() ?: run {
            Timber.e("Error while getting contact in handleUpdateContact")
            return emitNewStateFor(ContactFormEvent.SaveContactError.Generic)
        }
        val updatedDecryptedContact = contactFormUiModelMapper.toDecryptedContact(
            contact = contact,
            contactGroupLabels = decryptedContact.contactGroupLabels,
            photos = decryptedContact.photos,
            logos = decryptedContact.logos
        )

        editContact(
            userId = primaryUserId(),
            decryptedContact = updatedDecryptedContact,
            contactId = contactId
        ).getOrElse {
            return emitNewStateFor(ContactFormEvent.SaveContactError.Generic)
        }

        emitNewStateFor(ContactFormEvent.ContactUpdated)
    }

    private fun validateEmail(email: CharSequence): Boolean {
        val regex = InputValidationResult.EMAIL_VALIDATION_PATTERN.toRegex(RegexOption.IGNORE_CASE)
        return regex.matches(email)
    }

    private suspend fun primaryUserId() = primaryUserId.first()

    private fun emitNewStateFor(event: ContactFormEvent) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, event)
    }
}
