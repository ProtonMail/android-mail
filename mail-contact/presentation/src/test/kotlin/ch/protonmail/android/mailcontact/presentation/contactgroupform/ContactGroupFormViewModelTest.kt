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

package ch.protonmail.android.mailcontact.presentation.contactgroupform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.ColorHexWithName
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.usecase.GetColorHexWithNameList
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.CreateContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.EditContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.GetContactEmailsById
import ch.protonmail.android.mailcontact.domain.usecase.GetContactGroupError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupFormPreviewData
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.contact.ContactIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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

class ContactGroupFormViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testLabelId = ContactGroupFormPreviewData.contactGroupFormSampleData.id!!
    private val testColors = listOf(ColorHexWithName(TextUiModel("Red"), Color.Red.getHexStringFromColor()))
    private val testContactGroup = ContactGroup(
        testUserId,
        testLabelId,
        "Group name",
        Color.Red.getHexStringFromColor(),
        listOf(
            ContactEmail(
                UserIdTestData.userId,
                ContactEmailId("ContactEmailId"),
                "John Doe",
                "johndoe@protonmail.com",
                0,
                0,
                ContactIdTestData.contactId1,
                "johndoe@protonmail.com",
                listOf("LabelId1"),
                true
            )
        )
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactGroupFormUiModelMapperMock = mockk<ContactGroupFormUiModelMapper>()
    private val observeContactGroupMock = mockk<ObserveContactGroup>()
    private val getContactEmailsByIdMock = mockk<GetContactEmailsById>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()
    private val createContactGroupMock = mockk<CreateContactGroup>()
    private val editContactGroupMock = mockk<EditContactGroup>()

    private val getColorHexWithNameList = mockk<GetColorHexWithNameList> {
        every { this@mockk.invoke() } returns testColors
    }

    private val reducer = ContactGroupFormReducer()

    private val contactGroupFormViewModel by lazy {
        ContactGroupFormViewModel(
            observeContactGroupMock,
            getContactEmailsByIdMock,
            reducer,
            contactGroupFormUiModelMapperMock,
            savedStateHandleMock,
            createContactGroupMock,
            editContactGroupMock,
            getColorHexWithNameList,
            observePrimaryUserId
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(Color.Red.getHexStringFromColor()) } returns Color.Red.toArgb()
        every { android.graphics.Color.parseColor(Color.Blue.getHexStringFromColor()) } returns Color.Blue.toArgb()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given null Label ID in SavedState, when init, then emits loaded contact group state`() = runTest {
        // Given
        expectSavedStateLabelId(null)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactGroupFormState.Data(
                contactGroup = emptyContactGroupFormUiModel(Color.Red),
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Label ID, when init and observe empty contact group, then emits loaded contact group state`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup.copy(
            members = emptyList()
        )
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData.copy(
            memberCount = 0,
            members = emptyList()
        )
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel,
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Label ID in SavedState, when init and observe contact group, then emits loaded contact group state`() =
        runTest {
            // Given
            val expectedContactGroup = testContactGroup
            val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
            expectContactGroup(testUserId, testLabelId, expectedContactGroup)
            expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

            expectSavedStateLabelId(testLabelId)

            // When
            contactGroupFormViewModel.state.test {
                // Then
                val actual = awaitItem()
                val expected = ContactGroupFormState.Data(
                    contactGroup = expectedContactGroupFormUiModel,
                    colors = testColors
                )

                assertEquals(expected, actual)
            }
        }

    @Test
    fun `given Label ID in SavedState, when init and observe contact group fails, then emits error loading`() =
        runTest {
            // Given
            expectContactGroup(testUserId, testLabelId, null)
            expectSavedStateLabelId(testLabelId)

            // When
            contactGroupFormViewModel.state.test {
                // Then
                val actual = awaitItem()
                val expected = ContactGroupFormState.Loading(
                    errorLoading = Effect.of(TextUiModel(R.string.contact_group_form_loading_error))
                )

                assertEquals(expected, actual)
            }
        }

    @Test
    fun `when on close action is submitted, then close event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnCloseClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel,
                colors = testColors,
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when create and on save action is submitted, then created event is emitted`() = runTest {
        // Given
        val expectedContactGroup = emptyContactGroupFormUiModel(Color.Red)
        expectSavedStateLabelId(null)
        expectCreateContactGroup(
            testUserId,
            expectedContactGroup.name,
            expectedContactGroup.color.getHexStringFromColor()
        )

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnSaveClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroup,
                colors = testColors,
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_create_success)),
                displaySaveLoader = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when create and on save action is submitted, when creating fails, then error event is emitted`() = runTest {
        // Given
        val expectedContactGroup = emptyContactGroupFormUiModel(Color.Red)
        expectSavedStateLabelId(null)
        expectCreateContactGroupFails(
            testUserId,
            expectedContactGroup.name,
            expectedContactGroup.color.getHexStringFromColor()
        )

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnSaveClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroup,
                colors = testColors,
                closeWithSuccess = Effect.empty(),
                showErrorSnackbar = Effect.of(TextUiModel(R.string.contact_group_form_save_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on save action is submitted, then updated event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)
        expectEditContactGroup(
            testUserId,
            expectedContactGroup.labelId,
            expectedContactGroup.name,
            expectedContactGroup.color
        )

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnSaveClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel,
                colors = testColors,
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_update_success)),
                displaySaveLoader = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on update member list action is submitted, then loaded members event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnUpdateMemberList(listOf()))

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel.copy(memberCount = 0, members = emptyList()),
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on remove member action is submitted, then loaded members event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            val index = 0
            contactGroupFormViewModel.submit(
                ContactGroupFormViewAction.OnRemoveMemberClick(
                    expectedContactGroupFormUiModel.members[index].id
                )
            )

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel.copy(memberCount = 0, members = emptyList()),
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on update name action is submitted, then loaded members event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(
                ContactGroupFormViewAction.OnUpdateName("NewName")
            )

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel.copy(name = "NewName"),
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on update color action is submitted, then loaded members event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(
                ContactGroupFormViewAction.OnUpdateColor(Color.Blue)
            )

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel.copy(color = Color.Blue),
                colors = testColors
            )

            assertEquals(expected, actual)
        }
    }

    private fun expectSavedStateLabelId(labelId: LabelId?) {
        every {
            savedStateHandleMock.get<String>(ContactGroupFormScreen.ContactGroupFormLabelIdKey)
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

    private fun expectContactGroupFormUiModel(
        contactGroup: ContactGroup,
        expectedContactGroupFormUiModel: ContactGroupFormUiModel
    ) {
        every {
            contactGroupFormUiModelMapperMock.toContactGroupFormUiModel(contactGroup)
        } returns expectedContactGroupFormUiModel
    }

    private fun expectCreateContactGroup(
        userId: UserId,
        name: String,
        color: String
    ) {
        coEvery {
            createContactGroupMock.invoke(userId, name, color)
        } returns Unit.right()
    }

    private fun expectCreateContactGroupFails(
        userId: UserId,
        name: String,
        color: String
    ) {
        coEvery {
            createContactGroupMock.invoke(userId, name, color)
        } returns CreateContactGroup.CreateContactGroupErrors.FailedToCreateContactGroup.left()
    }

    private fun expectEditContactGroup(
        userId: UserId,
        labelId: LabelId,
        name: String,
        color: String
    ) {
        coEvery {
            editContactGroupMock.invoke(userId, labelId, name, color)
        } returns Unit.right()
    }
}
