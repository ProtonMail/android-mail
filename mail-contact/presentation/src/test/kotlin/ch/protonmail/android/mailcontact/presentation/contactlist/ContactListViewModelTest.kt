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

package ch.protonmail.android.mailcontact.presentation.contactlist

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModelMapper
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
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactListViewModelTest {

    private val defaultTestContact = Contact(
        UserIdTestData.userId,
        ContactId("1"),
        "first contact",
        listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("contact email id 1"),
                "First contact email",
                "firstcontact+alias@protonmail.com",
                0,
                0,
                ContactId("1"),
                "firstcontact@protonmail.com",
                emptyList(),
                true
            )
        )
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val observeContacts = mockk<ObserveContacts>()

    private val reducer = ContactListReducer()

    private val contactListItemUiModelMapper = ContactListItemUiModelMapper()

    private val contactListViewModel by lazy {
        ContactListViewModel(
            observeContacts,
            reducer,
            contactListItemUiModelMapper,
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
    fun `given empty contact list, when init, then emits empty state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(emptyList<Contact>().right())

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Empty()

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when init, then emits data state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                )
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given error on loading contact list, when init, then emits error state`() = runTest {
        // Given
        every {
            observeContacts.invoke(UserIdTestData.userId)
        } returns flowOf(GetContactError.left())

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.contact_list_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action open bottom sheet, then emits open state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnOpenBottomSheet)

            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action dismiss bottom sheet, then emits open state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnDismissBottomSheet)

            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action new contact, then emits open contact form state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnNewContactClick)

            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openContactForm = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action new contact group, then emits open group form state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnNewContactGroupClick)

            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openContactGroupForm = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action import contact, then emits open import state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnImportContactClick)

            val actual = awaitItem()
            val expected = ContactListState.ListLoaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openImportContact = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }
}
