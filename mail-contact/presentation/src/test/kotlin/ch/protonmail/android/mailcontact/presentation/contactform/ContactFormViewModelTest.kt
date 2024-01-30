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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.usecase.CreateContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.FieldType
import ch.protonmail.android.mailcontact.presentation.model.InputField
import ch.protonmail.android.mailcontact.presentation.model.Section
import ch.protonmail.android.mailcontact.presentation.model.emptyAddressField
import ch.protonmail.android.mailcontact.presentation.model.emptyContactFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.emptyEmailField
import ch.protonmail.android.mailcontact.presentation.model.emptyNoteField
import ch.protonmail.android.mailcontact.presentation.model.emptyTelephoneField
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactFormPreviewData.contactFormSampleData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactFormViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testContactId = contactFormSampleData.id!!

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactFormUiModelMapperMock = mockk<ContactFormUiModelMapper>()
    private val createContactMock = mockk<CreateContact>()
    private val observeDecryptedContactMock = mockk<ObserveDecryptedContact>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactFormReducer()

    private val contactFormViewModel by lazy {
        ContactFormViewModel(
            observeDecryptedContactMock,
            reducer,
            contactFormUiModelMapperMock,
            createContactMock,
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
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel()
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
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when OnCloseContactFormClick action is submitted, then CloseContactForm is emitted`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnCloseContactFormClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel(),
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given create mode, when OnSaveClick action is submitted, then close with success is emitted`() = runTest {
        // Given
        expectSavedStateContactId(null)
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
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(displayName = "Create"),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success)),
                displayCreateLoader = true,
                isSaveEnabled = true
            )

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

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnUpdateDisplayName("Update"))

            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnSaveClick)

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormSampleData.copy(displayName = "Update"),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_form_save_success))
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
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    displayName = newValue
                )
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
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    firstName = newValue
                )
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
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    lastName = newValue
                )
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
            // Then
            val actual = awaitItem()

            val index = 0
            val newValue = InputField.SingleTyped(
                value = "Updated",
                selectedType = FieldType.EmailType.Work
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Emails,
                    index = index,
                    newValue = newValue
                )
            )

            val mutableEmails = contactFormUiModel.emails.apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    emails = mutableEmails
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update telephone action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            val index = 0
            val newValue = InputField.SingleTyped(
                value = "Updated",
                selectedType = FieldType.TelephoneType.Work
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Telephones,
                    index = index,
                    newValue = newValue
                )
            )

            val mutableTelephones = contactFormUiModel.telephones.apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    telephones = mutableTelephones
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update address action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            val index = 0
            val newValue = InputField.Address(
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
                    index = index,
                    newValue = newValue
                )
            )

            val mutableAddresses = contactFormUiModel.addresses.apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    addresses = mutableAddresses
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update note action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            val index = 0
            val newValue = InputField.Note(
                value = "Updated"
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Notes,
                    index = index,
                    newValue = newValue
                )
            )

            val mutableNotes = contactFormUiModel.notes.apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    notes = mutableNotes
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on update other action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            val index = 0
            val newValue = InputField.SingleTyped(
                value = "Updated",
                selectedType = FieldType.OtherType.Role
            )
            contactFormViewModel.submit(
                ContactFormViewAction.OnUpdateItem(
                    section = Section.Others,
                    index = index,
                    newValue = newValue
                )
            )

            val mutableOthers = contactFormUiModel.others.apply {
                this[index] = newValue
            }
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    others = mutableOthers
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add email action is submitted, then state contact is updated`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Emails))

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(
                    emails = emptyContactFormUiModel().emails.apply {
                        this.add(emptyEmailField())
                    }
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add telephone action is submitted, then state contact is updated`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Telephones))

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(
                    telephones = emptyContactFormUiModel().telephones.apply {
                        this.add(emptyTelephoneField())
                    }
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add address action is submitted, then state contact is updated`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Addresses))

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(
                    addresses = emptyContactFormUiModel().addresses.apply {
                        this.add(emptyAddressField())
                    }
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add note action is submitted, then state contact is updated`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Notes))

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(
                    notes = emptyContactFormUiModel().notes.apply {
                        this.add(emptyNoteField())
                    }
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on add other action is submitted, then state contact is updated`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(ContactFormViewAction.OnAddItemClick(Section.Others))

            val actual = awaitItem()
            val expected = ContactFormState.Data.Create(
                contact = emptyContactFormUiModel().copy(
                    others = emptyContactFormUiModel().others.apply {
                        this.add(
                            (contactFormViewModel.state.value as ContactFormState.Data.Create).contact.others.last()
                        )
                    }
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on remove email action is submitted, then state contact is updated`() = runTest {
        // Given
        val contactFormUiModel = expectContactFormStateUpdate()

        // When
        contactFormViewModel.state.test {
            // Then
            awaitItem()

            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Emails, 0)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    emails = contactFormUiModel.emails.apply { this.removeAt(0) }
                )
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

            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Telephones, 0)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    telephones = contactFormUiModel.telephones.apply { this.removeAt(0) }
                )
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

            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Addresses, 0)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    addresses = contactFormUiModel.addresses.apply { this.removeAt(0) }
                )
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

            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Notes, 0)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    notes = contactFormUiModel.notes.apply { this.removeAt(0) }
                )
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

            contactFormViewModel.submit(
                ContactFormViewAction.OnRemoveItemClick(Section.Others, 0)
            )

            val actual = awaitItem()
            val expected = ContactFormState.Data.Update(
                contact = contactFormUiModel.copy(
                    others = contactFormUiModel.others.apply { this.removeAt(0) }
                )
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectSavedStateContactId(contactId: ContactId?) {
        every {
            savedStateHandleMock.get<String>(ContactFormScreen.ContactFormContactIdKey)
        } returns contactId?.id
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
        val expectedContactFormUiModel = contactFormSampleData
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactFormUiModel(expectedDecryptedContact, expectedContactFormUiModel)

        expectSavedStateContactId(testContactId)
        return expectedContactFormUiModel
    }

}
