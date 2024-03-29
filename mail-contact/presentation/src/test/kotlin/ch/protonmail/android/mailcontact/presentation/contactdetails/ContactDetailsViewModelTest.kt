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
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.mailcontact.domain.usecase.DeleteContact
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModelMapper
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactDetailsPreviewData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
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

class ContactDetailsViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testContactId = ContactDetailsPreviewData.contactDetailsSampleData.id

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactDetailsUiModelMapperMock = mockk<ContactDetailsUiModelMapper>()
    private val observeDecryptedContactMock = mockk<ObserveDecryptedContact>()
    private val deleteContact = mockk<DeleteContact>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactDetailsReducer()

    private val contactDetailsViewModel by lazy {
        ContactDetailsViewModel(
            observeDecryptedContactMock,
            reducer,
            contactDetailsUiModelMapperMock,
            deleteContact,
            savedStateHandleMock,
            observePrimaryUserId
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
    fun `given empty Contact ID in SavedState, when init, then emits error state`() = runTest {
        // Given
        expectSavedStateContactId(null)

        // When
        contactDetailsViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactDetailsState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.contact_details_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Contact ID in SavedState, when init and observe decrypted Contact, then emits loaded contact state`() =
        runTest {
            // Given
            val expectedDecryptedContact = DecryptedContact(testContactId)
            val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData
            expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
            expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)

            expectSavedStateContactId(testContactId)

            // When
            contactDetailsViewModel.state.test {
                // Then
                val actual = awaitItem()
                val expected = ContactDetailsState.Data(
                    contact = expectedContactDetailsUiModel
                )

                assertEquals(expected, actual)
            }
        }

    @Test
    fun `when OnCloseContactDetailsClick action is submitted, then CloseContactDetails is emitted`() = runTest {
        // Given
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnCloseClick)

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when OnCallClick action is submitted, then call phone is emitted`() = runTest {
        // Given
        val expectedPhoneNumber = "123123123"
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            defaultPhoneNumber = expectedPhoneNumber
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnCallClick(expectedPhoneNumber))

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                callPhoneNumber = Effect.of(expectedPhoneNumber)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when OnLongClick action is submitted, then CopyToClipboard is emitted`() = runTest {
        // Given
        val expectedTextCopiedToClipboard = "Contact's name header content"
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            nameHeader = expectedTextCopiedToClipboard
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnLongClick(expectedTextCopiedToClipboard))

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                copyToClipboard = Effect.of(expectedTextCopiedToClipboard)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when OnEmailClick action is submitted, then open composer is emitted`() = runTest {
        // Given
        val expectedMail = "test@proton.me"
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            defaultEmail = expectedMail
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnEmailClick(expectedMail))

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                openComposer = Effect.of(expectedMail)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when delete request action is submitted, then confirm dialog is shown`() = runTest {
        // Given
        val expectedMail = "test@proton.me"
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            defaultEmail = expectedMail
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)
        expectDeleteContact(testUserId, testContactId)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.DeleteRequested)

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                showDeleteConfirmDialog = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when delete confirm action is submitted, then delete use case is called`() = runTest {
        // Given
        val expectedMail = "test@proton.me"
        val expectedDecryptedContact = DecryptedContact(testContactId)
        val expectedContactDetailsUiModel = ContactDetailsPreviewData.contactDetailsSampleData.copy(
            defaultEmail = expectedMail
        )
        expectDecryptedContact(testUserId, testContactId, expectedDecryptedContact)
        expectContactDetailsUiModel(expectedDecryptedContact, expectedContactDetailsUiModel)
        expectDeleteContact(testUserId, testContactId)

        expectSavedStateContactId(testContactId)

        // When
        contactDetailsViewModel.state.test {
            // Then
            awaitItem() // Contact was loaded

            contactDetailsViewModel.submit(ContactDetailsViewAction.DeleteConfirmed)

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_details_delete_success))
            )

            assertEquals(expected, actual)
            coVerify { deleteContact(testUserId, testContactId) }
        }
    }

    private fun expectSavedStateContactId(contactId: ContactId?) {
        every {
            savedStateHandleMock.get<String>(ContactDetailsScreen.ContactDetailsContactIdKey)
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

    private fun expectContactDetailsUiModel(
        decryptedContact: DecryptedContact,
        expectedContactDetailsUiModel: ContactDetailsUiModel
    ) {
        every {
            contactDetailsUiModelMapperMock.toContactDetailsUiModel(decryptedContact)
        } returns expectedContactDetailsUiModel
    }

    private fun expectDeleteContact(userId: UserId, contactId: ContactId) {
        coEvery { deleteContact(userId, contactId) } returns Unit.right()
    }
}
