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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.repository.ContactsPermissionRepository
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ContactSuggestionsField
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.usecase.SortContactsForSuggestions
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.SearchContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.domain.usecase.SearchDeviceContacts
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RecipientsViewModelTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val searchContacts = mockk<SearchContacts>()
    private val searchContactGroups = mockk<SearchContactGroups>()
    private val searchDeviceContacts = mockk<SearchDeviceContacts>()
    private val sortContactsForSuggestions = mockk<SortContactsForSuggestions>()
    private val contactsPermissionRepository = mockk<ContactsPermissionRepository> {
        every { this@mockk.observePermissionDenied() } returns flowOf()
    }
    private val recipientsStateManager = spyk<RecipientsStateManager>()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit empty suggestions when the search term is empty`() = runTest {
        // Given
        val searchTerm = ""
        val viewModel = viewModel()

        // When
        viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)

        viewModel.contactsSuggestions.test {
            assertTrue(awaitItem().isEmpty())
        }

        viewModel.contactSuggestionsFieldFlow.test {
            assertEquals(ContactSuggestionsField.TO, awaitItem())
        }
    }

    @Test
    fun `should update recipients via the recipients state manager when performing the task`() {
        // Given
        val recipients = listOf<RecipientUiModel>(
            RecipientUiModel.Valid("aa@bb.cc"),
            RecipientUiModel.Invalid("__")
        )
        val viewModel = viewModel()

        // When
        viewModel.updateRecipients(recipients, ContactSuggestionsField.BCC)

        // Then
        verify { recipientsStateManager.updateRecipients(recipients, ContactSuggestionsField.BCC) }
        confirmVerified(recipientsStateManager)
    }

    @Test
    fun `should close suggestions when close suggestions is called`() = runTest {
        // Given
        val searchTerm = ""
        val viewModel = viewModel()

        // When
        viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)
        viewModel.contactSuggestionsFieldFlow.test {
            assertEquals(ContactSuggestionsField.TO, awaitItem())

            viewModel.closeSuggestions()
            advanceUntilIdle()
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun `should emit different fields suggestions when the search field changes`() = runTest {
        // Given
        val searchTerm = ""
        val viewModel = viewModel()

        // When + Then
        viewModel.contactsSuggestions.test {
            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)
            assertTrue(awaitItem().isEmpty())
        }

        viewModel.contactSuggestionsFieldFlow.test {
            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)
            assertEquals(ContactSuggestionsField.TO, awaitItem())

            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.CC)
            assertEquals(ContactSuggestionsField.CC, awaitItem())

            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.BCC)
            assertEquals(ContactSuggestionsField.BCC, awaitItem())
        }
    }

    @Test
    fun `should reflect empty suggestions emissions`() = runTest {
        // Given
        val searchTerm = "searchTerm"
        expectEmptySuggestions(searchTerm)
        val viewModel = viewModel()

        // When + Then
        viewModel.contactsSuggestions.test {
            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `should return contact suggestions once sorted`() = runTest {
        // Given
        val searchTerm = "searchTerm"
        val expectedSuggestions = listOf<ContactSuggestionUiModel>(
            ContactSuggestionUiModel.Contact("name", "N", "test@proton.me"),
            ContactSuggestionUiModel.ContactGroup("name", listOf("test2@proton.me", "test3@proton.me"), "#FFFFFF")
        )

        val mockedSearchResult = mockk<Either<GetContactError, List<Contact>>> {
            every { this@mockk.getOrNull() } returns mockk()
        }
        val mockedSearchGroupResult = mockk<Either<GetContactError, List<ContactGroup>>> {
            every { this@mockk.getOrNull() } returns mockk()
        }
        val mockedSearchDeviceResult = spyk<List<DeviceContact>>().right()

        coEvery { searchContacts(userId, searchTerm) } returns flowOf(mockedSearchResult)
        coEvery { searchContactGroups(userId, searchTerm) } returns flowOf(mockedSearchGroupResult)
        coEvery { searchDeviceContacts(searchTerm) } returns mockedSearchDeviceResult
        coEvery { sortContactsForSuggestions(any(), any(), any(), 100) } returns expectedSuggestions

        val viewModel = viewModel()

        // When + Then
        viewModel.contactsSuggestions.test {
            skipItems(1)
            viewModel.updateSearchTerm(searchTerm, ContactSuggestionsField.TO)
            assertEquals(expectedSuggestions, awaitItem())
        }
    }

    @Test
    fun `should emit contacts denied state when collected and repository returns a value (true)`() = runTest {
        // Given
        val expectedValue = true
        every { contactsPermissionRepository.observePermissionDenied() } returns flowOf(expectedValue.right())
        val viewModel = viewModel()

        // When + Then
        viewModel.contactsPermissionDenied.test {
            assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit contacts non denied state when collected and repository returns a value (false)`() = runTest {
        // Given
        val expectedValue = false
        every { contactsPermissionRepository.observePermissionDenied() } returns flowOf(expectedValue.right())
        val viewModel = viewModel()

        // When + Then
        viewModel.contactsPermissionDenied.test {
            assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `should emit contacts non denied state when collected and repository returns an error`() = runTest {
        // Given
        val expectedValue = false
        every {
            contactsPermissionRepository.observePermissionDenied()
        } returns flowOf(DataError.Local.NoDataCached.left())
        val viewModel = viewModel()

        // When + Then
        viewModel.contactsPermissionDenied.test {
            assertEquals(expectedValue, awaitItem())
            awaitComplete()
        }
    }

    private fun viewModel() = RecipientsViewModel(
        observePrimaryUserId,
        searchContacts,
        searchContactGroups,
        searchDeviceContacts,
        sortContactsForSuggestions,
        contactsPermissionRepository,
        testDispatcher,
        recipientsStateManager
    )

    private fun expectEmptySuggestions(@Suppress("SameParameterValue") forTerm: String) {
        coEvery { searchContacts(userId, forTerm) } returns flowOf(emptyList<Contact>().right())
        coEvery { searchContactGroups(userId, forTerm) } returns flowOf(emptyList<ContactGroup>().right())
        coEvery { searchDeviceContacts(forTerm) } returns emptyList<DeviceContact>().right()
        coEvery { sortContactsForSuggestions(any(), any(), any(), 100) } returns emptyList<ContactSuggestionUiModel>()
    }

    private companion object {

        val userId = UserId("user-id")
    }
}
