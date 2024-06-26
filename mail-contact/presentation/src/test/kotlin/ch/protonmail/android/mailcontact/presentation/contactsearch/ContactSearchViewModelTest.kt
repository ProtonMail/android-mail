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

package ch.protonmail.android.mailcontact.presentation.contactsearch

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.SearchContactGroups
import ch.protonmail.android.mailcontact.domain.usecase.SearchContacts
import ch.protonmail.android.mailcontact.presentation.model.ContactSearchUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactSearchUiModelMapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.contact.ContactEmailSample
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContactSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val reducer = ContactSearchReducer()

    private val contactSearchUiModelMapper = mockk<ContactSearchUiModelMapper>()
    private val searchContactsMock = mockk<SearchContacts>()
    private val searchContactGroupsMock = mockk<SearchContactGroups>()

    private val contactSearchViewModel by lazy {
        ContactSearchViewModel(
            reducer,
            contactSearchUiModelMapper,
            searchContactsMock,
            searchContactGroupsMock,
            observePrimaryUserId
        )
    }

    private val expectedSearchTerm = "searching for this"
    private val expectedContacts = listOf(ContactSample.Stefano, ContactSample.Francesco)
    private val expectedContactGroups = listOf(
        ContactGroup(
            UserIdSample.Primary,
            LabelIdSample.LabelCoworkers,
            "Coworkers contact group",
            "#AABBCC",
            listOf(ContactEmailSample.contactEmail1)
        )
    )
    private val expectedContactSearchUiModels = listOf(
        ContactSearchUiModel.Contact(
            expectedContacts[0].id,
            expectedContacts[0].name,
            expectedContacts[0].contactEmails.first().email,
            "S"
        ),
        ContactSearchUiModel.Contact(
            expectedContacts[1].id,
            expectedContacts[1].name,
            expectedContacts[1].contactEmails.first().email,
            "F"
        )
    )
    private val expectedContactGroupsSearchUiModels = listOf(
        ContactSearchUiModel.ContactGroup(
            expectedContactGroups[0].labelId,
            expectedContactGroups[0].name,
            Color.Red,
            emailCount = 1
        )
    )

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state is empty`() = runTest {
        // Given

        // When
        contactSearchViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactSearchState(
                close = Effect.empty(),
                uiModels = null,
                searchValue = ""
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `handles OnSearchValueChanged`() = runTest {
        // Given
        expectSearchContacts(
            expectedUserId = UserIdTestData.userId,
            expectedSearchTerm = expectedSearchTerm,
            expectedContacts = expectedContacts
        )

        expectSearchContactGroups(
            expectedUserId = UserIdTestData.userId,
            expectedSearchTerm = expectedSearchTerm,
            expectedContactGroups = expectedContactGroups,
            returnEmpty = true
        )

        expectContactSearchUiModelMapper(expectedContacts, expectedContactSearchUiModels)

        expectContactGroupsSearchUiModelMapper(expectedContactGroups, expectedContactGroupsSearchUiModels)

        contactSearchViewModel.submit(ContactSearchViewAction.OnSearchValueChanged(expectedSearchTerm))

        // When
        contactSearchViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertNotNull(actual.uiModels)
            assertTrue(actual.uiModels!!.contains(expectedContactSearchUiModels[0]))
            assertTrue(actual.uiModels!!.contains(expectedContactSearchUiModels[1]))
            assertTrue(actual.uiModels!!.contains(expectedContactGroupsSearchUiModels[0]))
        }
    }

    @Test
    fun `handles OnSearchValueCleared`() = runTest {
        // Given
        expectSearchContacts(
            expectedUserId = UserIdTestData.userId,
            expectedSearchTerm = expectedSearchTerm,
            expectedContacts = expectedContacts
        )

        expectSearchContactGroups(
            expectedUserId = UserIdTestData.userId,
            expectedSearchTerm = expectedSearchTerm,
            expectedContactGroups = expectedContactGroups,
            returnEmpty = true
        )

        expectContactSearchUiModelMapper(expectedContacts, expectedContactSearchUiModels)

        expectContactGroupsSearchUiModelMapper(expectedContactGroups, expectedContactGroupsSearchUiModels)

        contactSearchViewModel.submit(ContactSearchViewAction.OnSearchValueChanged(expectedSearchTerm))

        contactSearchViewModel.submit(ContactSearchViewAction.OnSearchValueCleared)

        // When
        contactSearchViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertNull(actual.uiModels)
        }
    }

    private fun expectSearchContacts(
        expectedUserId: UserId,
        expectedSearchTerm: String,
        expectedContacts: List<Contact>
    ): List<Contact> {
        coEvery {
            searchContactsMock.invoke(expectedUserId, expectedSearchTerm, false)
        } returns flowOf(expectedContacts.right())
        return expectedContacts
    }

    private fun expectSearchContactGroups(
        expectedUserId: UserId,
        expectedSearchTerm: String,
        expectedContactGroups: List<ContactGroup>,
        returnEmpty: Boolean
    ): List<ContactGroup> {
        coEvery {
            searchContactGroupsMock.invoke(expectedUserId, expectedSearchTerm, returnEmpty = returnEmpty)
        } returns flowOf(expectedContactGroups.right())
        return expectedContactGroups
    }

    private fun expectContactSearchUiModelMapper(
        contacts: List<Contact>,
        expected: List<ContactSearchUiModel.Contact>
    ) {
        every { contactSearchUiModelMapper.contactsToContactSearchUiModelList(contacts) } returns expected
    }

    private fun expectContactGroupsSearchUiModelMapper(
        contacts: List<ContactGroup>,
        expected: List<ContactSearchUiModel.ContactGroup>
    ) {
        every { contactSearchUiModelMapper.contactGroupsToContactSearchUiModelList(contacts) } returns expected
    }
}
