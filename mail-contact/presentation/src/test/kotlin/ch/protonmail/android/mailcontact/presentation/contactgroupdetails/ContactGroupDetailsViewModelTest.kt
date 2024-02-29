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

package ch.protonmail.android.mailcontact.presentation.contactgroupdetails

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.GetContactGroupError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupDetailsUiModelMapper
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupDetailsPreviewData
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactIdTestData
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
import me.proton.core.contact.domain.entity.ContactEmail
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactGroupDetailsViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testLabelId = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData.id
    private val testEmptyContactGroup = ContactGroup(
        testUserId,
        testLabelId,
        "Group name",
        Color.Red.getHexStringFromColor(),
        listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("contact email id 1"),
                "First name from contact email",
                "test1+alias@protonmail.com",
                0,
                0,
                ContactIdTestData.contactId1,
                "test1@protonmail.com",
                listOf("LabelId1"),
                true
            )
        )
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactGroupDetailsUiModelMapperMock = mockk<ContactGroupDetailsUiModelMapper>()
    private val observeContactGroupMock = mockk<ObserveContactGroup>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val reducer = ContactGroupDetailsReducer()

    private val contactGroupDetailsViewModel by lazy {
        ContactGroupDetailsViewModel(
            observeContactGroupMock,
            reducer,
            contactGroupDetailsUiModelMapperMock,
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
    fun `given empty Label ID in SavedState, when init, then emits error state`() = runTest {
        // Given
        expectSavedStateLabelId(null)

        // When
        contactGroupDetailsViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactGroupDetailsState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.contact_group_details_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Label ID, when init and observe empty contact group, then emits loaded contact group state`() = runTest {
        // Given
        val expectedContactGroup = testEmptyContactGroup.copy(
            members = emptyList()
        )
        val expectedContactGroupDetailsUiModel = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData.copy(
            memberCount = 0,
            members = emptyList()
        )
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupDetailsUiModel(expectedContactGroup, expectedContactGroupDetailsUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupDetailsViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactGroupDetailsState.Data(
                isSendEnabled = false,
                contactGroup = expectedContactGroupDetailsUiModel
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Label ID in SavedState, when init and observe contact group, then emits loaded contact group state`() =
        runTest {
            // Given
            val expectedContactGroup = testEmptyContactGroup
            val expectedContactGroupDetailsUiModel = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
            expectContactGroup(testUserId, testLabelId, expectedContactGroup)
            expectContactGroupDetailsUiModel(expectedContactGroup, expectedContactGroupDetailsUiModel)

            expectSavedStateLabelId(testLabelId)

            // When
            contactGroupDetailsViewModel.state.test {
                // Then
                val actual = awaitItem()
                val expected = ContactGroupDetailsState.Data(
                    isSendEnabled = true,
                    contactGroup = expectedContactGroupDetailsUiModel
                )

                assertEquals(expected, actual)
            }
        }

    @Test
    fun `when on close action is submitted, then close event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testEmptyContactGroup
        val expectedContactGroupDetailsUiModel = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupDetailsUiModel(expectedContactGroup, expectedContactGroupDetailsUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupDetailsViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupDetailsViewModel.submit(ContactGroupDetailsViewAction.OnCloseClick)

            val actual = awaitItem()

            val expected = ContactGroupDetailsState.Data(
                isSendEnabled = true,
                contactGroup = expectedContactGroupDetailsUiModel,
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when on email click action is submitted, then compose email event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testEmptyContactGroup
        val expectedContactGroupDetailsUiModel = ContactGroupDetailsPreviewData.contactGroupDetailsSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupDetailsUiModel(expectedContactGroup, expectedContactGroupDetailsUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupDetailsViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupDetailsViewModel.submit(ContactGroupDetailsViewAction.OnEmailClick)

            val actual = awaitItem()

            val expected = ContactGroupDetailsState.Data(
                isSendEnabled = true,
                contactGroup = expectedContactGroupDetailsUiModel,
                openComposer = Effect.of(
                    expectedContactGroupDetailsUiModel.members.map { it.email }
                )
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectSavedStateLabelId(labelId: LabelId?) {
        every {
            savedStateHandleMock.get<String>(ContactGroupDetailsScreen.ContactGroupDetailsLabelIdKey)
        } returns labelId?.id
    }

    private fun expectContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactGroup: ContactGroup?
    ) {
        every {
            observeContactGroupMock.invoke(userId, labelId)
        } returns flowOf(contactGroup?.right() ?: GetContactGroupError.GetLabelsError.left())
    }

    private fun expectContactGroupDetailsUiModel(
        contactGroup: ContactGroup,
        expectedContactGroupDetailsUiModel: ContactGroupDetailsUiModel
    ) {
        every {
            contactGroupDetailsUiModelMapperMock.toContactGroupDetailsUiModel(contactGroup)
        } returns expectedContactGroupDetailsUiModel
    }
}
