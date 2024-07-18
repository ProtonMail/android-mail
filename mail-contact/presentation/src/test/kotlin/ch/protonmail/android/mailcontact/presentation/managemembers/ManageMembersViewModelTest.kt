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

package ch.protonmail.android.mailcontact.presentation.managemembers

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModel
import ch.protonmail.android.mailcontact.presentation.model.ManageMembersUiModelMapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ManageMembersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(TestDispatcherProvider().Main)

    private val defaultTestContact = Contact(
        UserIdTestData.userId,
        ContactId("ContactId1"),
        "John Doe",
        listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("ContactEmailId1"),
                "John Doe",
                "johndoe+alias@protonmail.com",
                0,
                0,
                ContactId("ContactId1"),
                "johndoe@protonmail.com",
                emptyList(),
                true,
                lastUsedTime = 0
            ),
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("ContactEmailId2"),
                "Jane Doe",
                "janedoe@protonmail.com",
                0,
                0,
                ContactId("ContactId1"),
                "janedoe@protonmail.com",
                emptyList(),
                true,
                lastUsedTime = 0
            )
        )
    )
    private val defaultTestSelectedContactEmailIds = listOf(ContactEmailId("ContactEmailId2"))
    private val defaultTestManageMembersUiModel = listOf(
        ManageMembersUiModel(
            id = ContactEmailId("ContactEmailId1"),
            name = "John Doe",
            email = "johndoe+alias@protonmail.com",
            initials = "JD",
            isSelected = false,
            isDisplayed = true
        ),
        ManageMembersUiModel(
            id = ContactEmailId("ContactEmailId2"),
            name = "Jane Doe",
            email = "janedoe@protonmail.com",
            initials = "JD",
            isSelected = true,
            isDisplayed = true
        )
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val manageMembersUiModelMapperMock = mockk<ManageMembersUiModelMapper>()
    private val observeContactsMock = mockk<ObserveContacts>()

    private val reducer = ManageMembersReducer()

    private val manageMembersViewModel by lazy {
        ManageMembersViewModel(
            observeContactsMock,
            reducer,
            manageMembersUiModelMapperMock,
            observePrimaryUserId
        )
    }

    @Test
    fun `given contact list, when init, then emits data state`() = runTest {
        // Given
        val contacts = listOf(defaultTestContact)
        expectContactsData(contacts)
        expectUiModelMapper(contacts, defaultTestSelectedContactEmailIds, defaultTestManageMembersUiModel)

        // When
        manageMembersViewModel.state.test {
            awaitItem()

            manageMembersViewModel.initViewModelWithData(defaultTestSelectedContactEmailIds)

            // Then
            val actual = awaitItem()
            val expected = ManageMembersState.Data(
                members = defaultTestManageMembersUiModel
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when on done click, then emits on done state`() = runTest {
        // Given
        val contacts = listOf(defaultTestContact)
        expectContactsData(contacts)
        expectUiModelMapper(contacts, defaultTestSelectedContactEmailIds, defaultTestManageMembersUiModel)

        // When
        manageMembersViewModel.state.test {
            awaitItem()

            manageMembersViewModel.initViewModelWithData(defaultTestSelectedContactEmailIds)

            awaitItem()

            manageMembersViewModel.submit(ManageMembersViewAction.OnDoneClick)

            // Then
            val actual = awaitItem()
            val expected = ManageMembersState.Data(
                members = defaultTestManageMembersUiModel,
                onDone = Effect.of(listOf(ContactEmailId("ContactEmailId2")))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when on member click, then emits updated state`() = runTest {
        // Given
        val contacts = listOf(defaultTestContact)
        expectContactsData(contacts)
        expectUiModelMapper(contacts, defaultTestSelectedContactEmailIds, defaultTestManageMembersUiModel)

        // When
        manageMembersViewModel.state.test {
            awaitItem()

            manageMembersViewModel.initViewModelWithData(defaultTestSelectedContactEmailIds)

            awaitItem()

            manageMembersViewModel.submit(ManageMembersViewAction.OnMemberClick(ContactEmailId("ContactEmailId1")))

            // Then
            val actual = awaitItem()
            val updatedMembers = defaultTestManageMembersUiModel.toMutableList().apply {
                this[0] = this[0].copy(isSelected = true)
            }
            val expected = ManageMembersState.Data(
                members = updatedMembers
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when on search value change, then emits updated state`() = runTest {
        // Given
        val contacts = listOf(defaultTestContact)
        expectContactsData(contacts)
        expectUiModelMapper(contacts, defaultTestSelectedContactEmailIds, defaultTestManageMembersUiModel)

        // When
        manageMembersViewModel.state.test {
            awaitItem()

            manageMembersViewModel.initViewModelWithData(defaultTestSelectedContactEmailIds)

            awaitItem()

            manageMembersViewModel.submit(ManageMembersViewAction.OnSearchValueChanged("John"))

            // Then
            val actual = awaitItem()
            val updatedMembers = defaultTestManageMembersUiModel.toMutableList().apply {
                this[1] = this[1].copy(isDisplayed = false)
            }
            val expected = ManageMembersState.Data(
                members = updatedMembers
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectContactsData(contacts: List<Contact>) {
        coEvery {
            observeContactsMock(userId = UserIdTestData.userId)
        } returns flowOf(contacts.right())
    }

    private fun expectUiModelMapper(
        contacts: List<Contact>,
        selectedContactEmailIds: List<ContactEmailId>,
        manageMembersUiModel: List<ManageMembersUiModel>
    ) {
        every {
            manageMembersUiModelMapperMock.toManageMembersUiModelList(
                contacts = contacts,
                selectedContactEmailIds = selectedContactEmailIds
            )
        } returns manageMembersUiModel
    }
}
