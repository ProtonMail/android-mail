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
import ch.protonmail.android.mailcontact.domain.usecase.ObserveDecryptedContact
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactDetailsUiModelMapper
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactDetailsPreviewData
import ch.protonmail.android.testdata.user.UserIdTestData
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
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactDetailsReducer()

    private val contactDetailsViewModel by lazy {
        ContactDetailsViewModel(
            observeDecryptedContactMock,
            reducer,
            contactDetailsUiModelMapperMock,
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

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnCloseContactDetailsClick)

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when OnDeleteClick action is submitted, then CloseContactDetails is emitted`() = runTest {
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

            contactDetailsViewModel.submit(ContactDetailsViewAction.OnDeleteClick)

            val actual = awaitItem()

            val expected = ContactDetailsState.Data(
                contact = expectedContactDetailsUiModel,
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_details_delete_success))
            )

            assertEquals(expected, actual)
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
}
