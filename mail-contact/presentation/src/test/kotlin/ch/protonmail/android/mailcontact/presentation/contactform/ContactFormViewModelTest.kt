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
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.BasicContactInfo
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.encode
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.usecase.CreateContact
import ch.protonmail.android.mailcontact.domain.usecase.EditContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section
import ch.protonmail.android.mailcontact.presentation.model.emptyAddressField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModelWithInitialFields
import ch.protonmail.android.mailcontact.presentation.model.emptyEmailField
import ch.protonmail.android.mailcontact.presentation.model.emptyNoteField
import ch.protonmail.android.mailcontact.presentation.model.emptyTelephoneField
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData.contactFormSampleData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.serialize
import org.junit.Test
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactFormViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testContactId = contactFormSampleData().id!!

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactFormUiModelMapperMock = mockk<ContactFormUiModelMapper>()
    private val createContactMock = mockk<CreateContact>()
    private val editContactMock = mockk<EditContact>()
    private val observeDecryptedContactMock = mockk<ObserveDecryptedContact>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactFormReducer()

    private val contactFormViewModel by lazy {
        ContactFormViewModel(
            observeDecryptedContactMock,
            reducer,
            contactFormUiModelMapperMock,
            createContactMock,
            editContactMock,
            observePrimaryUserId,
            savedStateHandleMock
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given empty Contact ID in SavedState, when init, then emits create state`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = emptyContactFormUiModelWithInitialFields()
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Contact ID in SavedState, when error in decrypt, then emits loading error state`() = runTest {
        // Given
        expectSavedStateContactId(testContactId)
        expectDecryptedContact(testUserId, testContactId, null)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.contact_form_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Contact ID in SavedState, when init, then emits update state`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel,
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact name and address in SavedState, when init, then emits create state with data`() = runTest {
        // Given
        val contactName = "Test User"
        val contactEmail = "testuser@proton.me"
        expectSavedStateContactId(null)
        expectSavedStateBasicContactInfo(contactName, contactEmail)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertTrue(actual is ContactFormState.Data)
            assertEquals(contactName, actual.contact.displayName)
            assertEquals(contactEmail, actual.contact.emails.first().value)
        }
    }

    @Test
    fun `given no contact name but address in SavedState, when init, then emits create state with data`() = runTest {
        // Given
        val contactName = ""
        val contactEmail = "test@proton.me"
        expectSavedStateContactId(null)
        expectSavedStateBasicContactInfo(contactName, contactEmail)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertTrue(actual is ContactFormState.Data)
            assertEquals(contactName, actual.contact.displayName)
            assertEquals(contactEmail, actual.contact.emails.first().value)
        }
    }

    @Test
    fun `when OnCloseContactFormClick action is submitted, then CloseContactForm is emitted`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnCloseContactFormClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = emptyContactFormUiModelWithInitialFields(),
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given create mode, when OnSaveClick action is submitted, then close with success is emitted`() = runTest {
        // Given
        expectNoSavedState()

        every {
            contactFormUiModelMapperMock.toDecryptedContact(any(), any(), any(), any())
        } returns DecryptedContact(ContactId(""))
        coEvery {
            createContactMock(testUserId, any())
        } returns Unit.right()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnUpdateDisplayName("Create"))

            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnSaveClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = emptyContactFormUiModelWithInitialFields().copy(displayName = "Create"),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_create_success)),
                displaySaveLoader = true,
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given create mode, when updating the address, the form updates accordingly and save is called`() = runTest {
        // Given
        expectNoSavedState()

        every {
            contactFormUiModelMapperMock.toDecryptedContact(any(), any(), any(), any())
        } returns DecryptedContact(ContactId(""))
        coEvery {
            createContactMock(testUserId, any())
        } returns Unit.right()

        // When
        contactFormViewModel.state.test {
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnUpdateDisplayName("Display"))

            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Addresses))

            awaitItem()

            val fieldId = "2"
            val newValue = InputField.Address(
                fieldId = fieldId,
                streetAddress = "Street",
                postalCode = "Postal",
                city = "City",
                region = "Region",
                country = "Country",
                selectedType = FieldType.AddressType.Address
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Addresses,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            awaitItem()

            val expectedFormAddress = InputField.Address(
                fieldId = "2",
                postalCode = "Postal",
                city = "City",
                country = "Country",
                streetAddress = "Street",
                region = "Region",
                selectedType = FieldType.AddressType.Address
            )

            every {
                contactFormUiModelMapperMock.toDecryptedContact(
                    match {
                        it.addresses == listOf(expectedFormAddress)
                    },
                    any(), any(), any()
                )
            } returns DecryptedContact(ContactId(""), addresses = listOf(mockk()))

            contactFormViewModel.submit(ContactFormViewAction.OnSaveClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = emptyContactFormUiModelWithInitialFields().copy(
                    displayName = "Display",
                    addresses = listOf(expectedFormAddress),
                    incrementalUniqueFieldId = 3
                ),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_create_success)),
                displaySaveLoader = true,
                isSaveEnabled = true
            )

            // Then
            coVerify(exactly = 1) {
                createContactMock(
                    testUserId,
                    match { contact ->
                        contact.addresses.isNotEmpty()
                    }
                )
            }

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given update mode, when OnSaveClick action is submitted, then close with success is emitted`() = runTest {
        // Given
        expectContactFormStateUpdate()
        every {
            contactFormUiModelMapperMock.toDecryptedContact(any(), any(), any(), any())
        } returns DecryptedContact(ContactId(""))
        coEvery {
            editContactMock(testUserId, any(), any())
        } returns Unit.right()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnUpdateDisplayName("Update"))

            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnSaveClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormSampleData().copy(displayName = "Update"),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_update_success)),
                isSaveEnabled = true,
                displaySaveLoader = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given invalid email, when OnSaveClick action is submitted, then show error message is emitted`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactFormUiModel = contactFormSampleData().copy(
            emails = mutableListOf(
                InputField.SingleTyped("1", "invalidEmail", FieldType.EmailType.Email)
            )
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactFormUiModel(expectedDecryptedContact, expectedContactFormUiModel)
        expectSavedStateContactId(testContactId)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnSaveClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = expectedContactFormUiModel,
                isSaveEnabled = true,
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_form_invalid_email_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update display name action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val newValue = "Updated"
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateDisplayName(newValue)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    displayName = newValue
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update first name action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val newValue = "Updated"
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateFirstName(newValue)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    firstName = newValue
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update last name action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val newValue = "Updated"
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateLastName(newValue)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    lastName = newValue
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update email action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            skipItems(1)

            val index = 0
            val fieldId = contactFormUiModel.emails[index].fieldId
            val newValue = InputField.SingleTyped(
                fieldId = fieldId,
                value = "Updated",
                selectedType = FieldType.EmailType.Work
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Emails,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            // Then
            val mutableEmails = contactFormUiModel.emails.toMutableList().apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    emails = mutableEmails
                ),
                isSaveEnabled = true
            )

            val actual = awaitItem()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update telephone action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            skipItems(1)

            val index = 0
            val fieldId = contactFormUiModel.telephones[index].fieldId
            val newValue = InputField.SingleTyped(
                fieldId = fieldId,
                value = "Updated",
                selectedType = FieldType.TelephoneType.Work
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Telephones,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            // Then
            val mutableTelephones = contactFormUiModel.telephones.toMutableList().apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    telephones = mutableTelephones
                ),
                isSaveEnabled = true
            )

            val actual = awaitItem()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update address action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            skipItems(1)

            val index = 0
            val fieldId = contactFormUiModel.addresses[index].fieldId
            val newValue = InputField.Address(
                fieldId = fieldId,
                streetAddress = "Updated",
                postalCode = "Updated",
                city = "Updated",
                region = "Updated",
                country = "Updated",
                selectedType = FieldType.AddressType.Work
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Addresses,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            // Then
            val mutableAddresses = contactFormUiModel.addresses.toMutableList().apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    addresses = mutableAddresses
                ),
                isSaveEnabled = true
            )

            val actual = awaitItem()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update note action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            skipItems(1)

            val index = 0
            val fieldId = contactFormUiModel.notes[index].fieldId
            val newValue = InputField.Note(
                fieldId = fieldId,
                value = "Updated"
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Notes,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            // Then
            val mutableNotes = contactFormUiModel.notes.toMutableList().apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    notes = mutableNotes
                ),
                isSaveEnabled = true
            )

            val actual = awaitItem()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update other action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            skipItems(1)

            val index = 0
            val fieldId = contactFormUiModel.others[index].fieldId
            val newValue = InputField.SingleTyped(
                fieldId = fieldId,
                value = "Updated",
                selectedType = FieldType.OtherType.Role
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Others,
                    fieldId = fieldId,
                    newValue = newValue
                )
            )

            // Then
            val mutableOthers = contactFormUiModel.others.toMutableList().apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    others = mutableOthers
                ),
                isSaveEnabled = true
            )

            val actual = awaitItem()
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add email action is submitted, then state contact is updated`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Emails))

            val actual = awaitItem()
            val contact = emptyContactFormUiModelWithInitialFields()
            val expected = ContactFormState.Data(
                contact = contact.copy(
                    emails = contact.emails.toMutableList().apply {
                        this.add(emptyEmailField(contact.incrementalUniqueFieldId.toString()))
                    },
                    incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add telephone action is submitted, then state contact is updated`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Telephones))

            val actual = awaitItem()
            val contact = emptyContactFormUiModelWithInitialFields()
            val expected = ContactFormState.Data(
                contact = contact.copy(
                    telephones = contact.telephones.toMutableList().apply {
                        this.add(emptyTelephoneField(contact.incrementalUniqueFieldId.toString()))
                    },
                    incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add address action is submitted, then state contact is updated`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Addresses))

            val actual = awaitItem()
            val contact = emptyContactFormUiModelWithInitialFields()
            val expected = ContactFormState.Data(
                contact = contact.copy(
                    addresses = emptyContactFormUiModelWithInitialFields().addresses.toMutableList().apply {
                        this.add(emptyAddressField(contact.incrementalUniqueFieldId.toString()))
                    },
                    incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add note action is submitted, then state contact is updated`() = runTest {
        // Given
        expectNoSavedState()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Notes))

            val actual = awaitItem()
            val contact = emptyContactFormUiModelWithInitialFields()
            val expected = ContactFormState.Data(
                contact = contact.copy(
                    notes = contact.notes.toMutableList().apply {
                        this.add(emptyNoteField(contact.incrementalUniqueFieldId.toString()))
                    },
                    incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add other action is submitted, then state contact is updated`() = runTest {
        // Given
        expectNoSavedState()

        val supportedOtherFieldsCount = 8 // Size of the type list in ContactFormUiMode.emptyRandomOtherField
        // When adding others field the type is selected randomly. Mock random so that we know what type is ued.
        mockkObject(Random)
        every { Random.nextInt(supportedOtherFieldsCount) } returns 0

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Others))

            val actual = awaitItem()
            val contact = emptyContactFormUiModelWithInitialFields()
            val expected = ContactFormState.Data(
                contact = contact.copy(
                    others = contact.others.toMutableList().apply {
                        this.add(
                            InputField.SingleTyped(
                                fieldId = contact.incrementalUniqueFieldId.toString(),
                                value = "",
                                selectedType = FieldType.OtherType.Organization
                            )
                        )
                    },
                    incrementalUniqueFieldId = contact.incrementalUniqueFieldId.plus(1)
                )
            )

            assertEquals(expected, actual)
        }

        unmockkObject(Random)
    }

    @Test
    fun `when on remove email action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val index = 0
            val fieldId = contactFormUiModel.emails[index].fieldId
            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Emails, fieldId)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    emails = contactFormUiModel.emails.toMutableList().apply { this.removeAt(index) }
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on remove telephone action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val index = 0
            val fieldId = contactFormUiModel.telephones[index].fieldId
            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Telephones, fieldId)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    telephones = contactFormUiModel.telephones.toMutableList().apply { this.removeAt(index) }
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on remove address action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val index = 0
            val fieldId = contactFormUiModel.addresses[index].fieldId
            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Addresses, fieldId)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    addresses = contactFormUiModel.addresses.toMutableList().apply { this.removeAt(index) }
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on remove note action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val index = 0
            val fieldId = contactFormUiModel.notes[index].fieldId
            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Notes, fieldId)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    notes = contactFormUiModel.notes.toMutableList().apply { this.removeAt(index) }
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on remove other action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            val index = 0
            val fieldId = contactFormUiModel.others[index].fieldId
            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Others, fieldId)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data(
                contact = contactFormUiModel.copy(
                    others = contactFormUiModel.others.toMutableList().apply { this.removeAt(index) }
                ),
                isSaveEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectNoSavedState() {
        expectSavedStateContactId(null)
        expectSavedStateNoBasicContactInfo()
    }

    private fun expectSavedStateContactId(contactId: ContactId?) {
        every {
            savedStateHandleMock.get<String>(ContactFormScreen.ContactFormContactIdKey)
        } returns contactId?.id
    }

    private fun expectSavedStateBasicContactInfo(contactName: String?, contactEmail: String) {
        every {
            savedStateHandleMock.get<String>(ContactFormScreen.ContactFormBasicContactInfoKey)
        } returns BasicContactInfo(contactName, contactEmail).encode().serialize()
    }

    private fun expectSavedStateNoBasicContactInfo() {
        every {
            savedStateHandleMock.get<String>(ContactFormScreen.ContactFormBasicContactInfoKey)
        } returns null
    }

    private fun expectDecryptedContact(
        userId: UserId,
        contactId: ContactId,
        decryptedContact: DecryptedContact?
    ) {
        every {
            observeDecryptedContactMock.invoke(userId, contactId)
        } returns flowOf(decryptedContact?.right() ?: DataError.Local.NoDataCached.left())
    }

    private fun expectContactFormUiModel(
        decryptedContact: DecryptedContact,
        expectedContactFormUiModel: ContactFormUiModel
    ) {
        every {
            contactFormUiModelMapperMock.toContactFormUiModel(decryptedContact)
        } returns expectedContactFormUiModel
    }

    private fun expectContactFormStateUpdate(): ContactFormUiModel {
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactFormUiModel = contactFormSampleData()
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactFormUiModel(expectedDecryptedContact, expectedContactFormUiModel)

        expectSavedStateContactId(testContactId)
        return expectedContactFormUiModel
    }

}
