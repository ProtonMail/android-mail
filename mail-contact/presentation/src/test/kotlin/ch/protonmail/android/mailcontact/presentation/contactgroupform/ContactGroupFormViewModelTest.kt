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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.usecase.GetContactGroupError
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContactGroup
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormReducer
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormScreen
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormState
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormViewAction
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormViewModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupFormUiModelMapper
import ch.protonmail.android.mailcontact.presentation.model.emptyContactGroupFormUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactGroupFormPreviewData
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.user.UserIdTestData
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
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ContactGroupFormViewModelTest {

    private val testUserId = UserIdTestData.userId
    private val testLabelId = ContactGroupFormPreviewData.contactGroupFormSampleData.id!!
    private val testColors = listOf(Color.Red)
    private val testColorStrings = listOf(Color.Red.getHexStringFromColor())
    private val testEmptyContactGroup = ContactGroup(
        testUserId,
        testLabelId,
        "Group name",
        Color.Red.getHexStringFromColor(),
        emptyList()
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }

    private val contactGroupFormUiModelMapperMock = mockk<ContactGroupFormUiModelMapper>()
    private val observeContactGroupMock = mockk<ObserveContactGroup>()
    private val savedStateHandleMock = mockk<SavedStateHandle>()

    private val getLabelColors = mockk<GetLabelColors> {
        every { this@mockk.invoke() } returns testColorStrings
    }

    private val reducer = ContactGroupFormReducer()

    private val contactGroupFormViewModel by lazy {
        ContactGroupFormViewModel(
            observeContactGroupMock,
            reducer,
            contactGroupFormUiModelMapperMock,
            savedStateHandleMock,
            getLabelColors,
            observePrimaryUserId
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(Color.Red.getHexStringFromColor()) } returns Color.Red.toArgb()
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
                contactGroup = emptyContactGroupFormUiModel(testColors)
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
                contactGroup = expectedContactGroupFormUiModel
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given Label ID in SavedState, when init and observe contact group, then emits loaded contact group state`() =
        runTest {
            // Given
            val expectedContactGroup = testEmptyContactGroup
            val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
            expectContactGroup(testUserId, testLabelId, expectedContactGroup)
            expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

            expectSavedStateLabelId(testLabelId)

            // When
            contactGroupFormViewModel.state.test {
                // Then
                val actual = awaitItem()
                val expected = ContactGroupFormState.Data(
                    contactGroup = expectedContactGroupFormUiModel
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
        val expectedContactGroup = testEmptyContactGroup
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
                close = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when create and on save action is submitted, then created event is emitted`() = runTest {
        // Given
        expectSavedStateLabelId(null)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnSaveClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = emptyContactGroupFormUiModel(testColors),
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_create_success)),
                displaySaveLoader = true
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `when update and on save action is submitted, then updated event is emitted`() = runTest {
        // Given
        val expectedContactGroup = testEmptyContactGroup
        val expectedContactGroupFormUiModel = ContactGroupFormPreviewData.contactGroupFormSampleData
        expectContactGroup(testUserId, testLabelId, expectedContactGroup)
        expectContactGroupFormUiModel(expectedContactGroup, expectedContactGroupFormUiModel)

        expectSavedStateLabelId(testLabelId)

        // When
        contactGroupFormViewModel.state.test {
            // Then
            awaitItem() // ContactGroup was loaded

            contactGroupFormViewModel.submit(ContactGroupFormViewAction.OnSaveClick)

            val actual = awaitItem()

            val expected = ContactGroupFormState.Data(
                contactGroup = expectedContactGroupFormUiModel,
                closeWithSuccess = Effect.of(TextUiModel(R.string.contact_group_form_update_success)),
                displaySaveLoader = true
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
            contactGroupFormUiModelMapperMock.toContactGroupFormUiModel(contactGroup, testColors)
        } returns expectedContactGroupFormUiModel
    }
}
