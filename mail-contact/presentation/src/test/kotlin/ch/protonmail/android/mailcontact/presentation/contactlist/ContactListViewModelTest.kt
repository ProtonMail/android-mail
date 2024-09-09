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

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.GetInitials
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.usecase.GetContactGroupLabelsError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroupLabels
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailcontact.domain.usecase.featureflags.IsContactGroupsCrudEnabled
import ch.protonmail.android.mailcontact.domain.usecase.featureflags.IsContactSearchEnabled
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModelMapper
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
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
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ContactListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val defaultTestContactGroupLabel = Label(
        userId = UserIdTestData.userId,
        labelId = LabelId("LabelId1"),
        parentId = null,
        name = "Label 1",
        type = LabelType.ContactGroup,
        path = "",
        color = Color.Red.getHexStringFromColor(),
        order = 0,
        isNotified = null,
        isExpanded = null,
        isSticky = null
    )
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
                listOf(defaultTestContactGroupLabel.labelId.id),
                true,
                lastUsedTime = 0
            )
        )
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val observeContacts = mockk<ObserveContacts>()
    private val observeContactGroupLabels = mockk<ObserveContactGroupLabels>()
    private val isContactGroupsCrudEnabledMock = mockk<IsContactGroupsCrudEnabled> {
        every { this@mockk() } returns true
    }
    private val isContactSearchEnabledMock = mockk<IsContactSearchEnabled> {
        every { this@mockk() } returns true
    }

    private val reducer = ContactListReducer()

    private val isPaidUser = mockk<IsPaidUser>()
    private val getInitials = GetInitials()
    private val contactListItemUiModelMapper = ContactListItemUiModelMapper(getInitials)
    private val colorMapper = ColorMapper()
    private val contactGroupItemUiModelMapper = ContactGroupItemUiModelMapper(colorMapper)

    private val contactListViewModel by lazy {
        ContactListViewModel(
            observeContacts,
            observeContactGroupLabels,
            isPaidUser,
            reducer,
            contactListItemUiModelMapper,
            contactGroupItemUiModelMapper,
            isContactGroupsCrudEnabledMock,
            isContactSearchEnabledMock,
            observePrimaryUserId
        )
    }

    @Test
    fun `given empty contact list, when init, then emits empty state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(emptyList<Contact>().right())
        coEvery {
            observeContactGroupLabels(userId = UserIdTestData.userId)
        } returns flowOf(emptyList<Label>().right())

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.Loaded.Empty()

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when init, then emits data state`() = runTest {
        // Given
        expectContactsData()

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
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
        coEvery {
            observeContactGroupLabels(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContactGroupLabel).right())

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
    fun `given error on loading contact group list, when init, then emits error state`() = runTest {
        // Given
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())
        coEvery {
            observeContactGroupLabels(userId = UserIdTestData.userId)
        } returns flowOf(GetContactGroupLabelsError.left())

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
    fun `when feature flag IsContactGroupsCrudEnabled is false then emit appropriate event`() = runTest {
        // Given
        expectContactsData()
        every { isContactGroupsCrudEnabledMock.invoke() } returns false

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                isContactGroupsCrudEnabled = false,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when feature flag IsContactSearchEnabled is false then emit appropriate event`() = runTest {
        // Given
        expectContactsData()
        every { isContactSearchEnabledMock.invoke() } returns false

        // When
        contactListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = false
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action open bottom sheet, then emits open state`() = runTest {
        // Given
        expectContactsData()

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnOpenBottomSheet)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action dismiss bottom sheet, then emits open state`() = runTest {
        // Given
        expectContactsData()

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnDismissBottomSheet)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action new contact, then emits open contact form state`() = runTest {
        // Given
        expectContactsData()

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnNewContactClick)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openContactForm = Effect.of(Unit),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given paid user contact list, when action new contact group, then emits open group form state`() = runTest {
        // Given
        expectContactsData()
        expectPaidUser(true)

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnNewContactGroupClick)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openContactGroupForm = Effect.of(Unit),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given free user contact list, when action new contact group, then emits open group form state`() = runTest {
        // Given
        expectContactsData()
        expectPaidUser(false)

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnNewContactGroupClick)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                subscriptionError = Effect.of(TextUiModel.TextRes(R.string.contact_group_form_subscription_error)),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given contact list, when action import contact, then emits open import state`() = runTest {
        // Given
        expectContactsData()

        // When
        contactListViewModel.state.test {
            awaitItem()

            contactListViewModel.submit(ContactListViewAction.OnImportContactClick)

            val actual = awaitItem()
            val expected = ContactListState.Loaded.Data(
                contacts = contactListItemUiModelMapper.toContactListItemUiModel(
                    listOf(defaultTestContact)
                ),
                contactGroups = contactGroupItemUiModelMapper.toContactGroupItemUiModel(
                    listOf(defaultTestContact), listOf(defaultTestContactGroupLabel)
                ),
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide),
                openImportContact = Effect.of(Unit),
                isContactGroupsCrudEnabled = true,
                isContactSearchEnabled = true
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectContactsData() {
        coEvery {
            observeContacts(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContact).right())
        coEvery {
            observeContactGroupLabels(userId = UserIdTestData.userId)
        } returns flowOf(listOf(defaultTestContactGroupLabel).right())
    }

    private fun expectPaidUser(value: Boolean) {
        coEvery { isPaidUser(userId = UserIdTestData.userId) } returns value.right()
    }
}
