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

package ch.protonmail.android.maillabel.presentation.labelform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.usecase.CreateLabel
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.domain.usecase.IsLabelLimitReached
import ch.protonmail.android.maillabel.domain.usecase.IsLabelNameAllowed
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LabelFormViewModelTest {

    private val defaultTestLabel = buildLabel(id = "LabelID", color = Color.Red.getHexStringFromColor())
    private val defaultTestUpdatedName = "UpdatedName"

    private val loadedCreateState = LabelFormState.Data.Create(
        isSaveEnabled = false,
        name = "",
        color = defaultTestLabel.color,
        colorList = listOf(Color.Red)
    )
    private val loadedUpdateState = LabelFormState.Data.Update(
        isSaveEnabled = true,
        name = defaultTestLabel.name,
        color = defaultTestLabel.color,
        colorList = listOf(Color.Red),
        labelId = defaultTestLabel.labelId
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val getLabel = mockk<GetLabel>()
    private val createLabel = mockk<CreateLabel> {
        coEvery { this@mockk.invoke(userId, any(), any()) } returns
            Unit.right()
    }
    private val updateLabel = mockk<UpdateLabel> {
        coEvery { this@mockk.invoke(userId, any()) } returns
            Unit.right()
    }
    private val deleteLabel = mockk<DeleteLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestLabel.labelId, LabelType.MessageLabel) } returns
            Unit.right()
    }

    private val getLabelColors = mockk<GetLabelColors> {
        every { this@mockk.invoke() } returns listOf(defaultTestLabel.color)
    }

    private val isLabelNameAllowed = mockk<IsLabelNameAllowed>()
    private val isLabelLimitReached = mockk<IsLabelLimitReached>()

    private val reducer = LabelFormReducer()

    private val savedStateHandle = mockk<SavedStateHandle>()

    private val labelFormViewModel by lazy {
        LabelFormViewModel(
            getLabel,
            createLabel,
            updateLabel,
            deleteLabel,
            getLabelColors,
            isLabelNameAllowed,
            isLabelLimitReached,
            reducer,
            observePrimaryUserId,
            savedStateHandle
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
        unmockkStatic(android.graphics.Color::class)
    }

    @Test
    fun `given null label id value, when init, then emits create label state`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null

        // When
        labelFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `given label id value, when init, then emits update label state`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns defaultTestLabel.labelId.id
        coEvery {
            getLabel.invoke(userId, defaultTestLabel.labelId, LabelType.MessageLabel)
        } returns defaultTestLabel.right()

        // When
        labelFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `given create state, when action label name changed, then emits updated label`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())
        }
    }

    @Test
    fun `given create state, when action label color changed, then emits updated label`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelColorChanged(Color.Blue))
            // Then
            assertEquals(loadedState.copy(color = Color.Blue.getHexStringFromColor()), awaitItem())
        }
    }

    @Test
    fun `given create state, when action label save, then emits close with save`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName) } returns true.right()
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageLabel) } returns false.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    displayCreateLoader = true,
                    closeWithSave = Effect.of(Unit)
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action label save, then emits close with save`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns defaultTestLabel.labelId.id
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName) } returns true.right()
        coEvery {
            getLabel.invoke(userId, defaultTestLabel.labelId, LabelType.MessageLabel)
        } returns defaultTestLabel.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, closeWithSave = Effect.of(Unit)), awaitItem())
        }
    }

    @Test
    fun `given create state and name already exists, when action label save, then emits error snack`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageLabel) } returns false.right()
        coEvery { isLabelNameAllowed.invoke(userId, any()) } returns false.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showLabelAlreadyExistsSnackbar = Effect.of(Unit)
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given name with trailing whitespace, when action label save, then emit success`() = runTest {
        // Given
        val loadedState = loadedCreateState
        val nameWithTrailingWhitespace = " $defaultTestUpdatedName "
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageLabel) } returns false.right()
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName) } returns true.right()
        coEvery { createLabel.invoke(userId, defaultTestUpdatedName, any()) } returns Unit.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(nameWithTrailingWhitespace))
            // Then
            assertEquals(loadedState.copy(name = nameWithTrailingWhitespace, isSaveEnabled = true), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = nameWithTrailingWhitespace,
                    isSaveEnabled = true,
                    closeWithSave = Effect.of(Unit),
                    displayCreateLoader = true
                ),
                awaitItem()
            )
        }
        coVerify {
            // Verify that we use the name trimmed from leading and trailing whitespaces
            isLabelNameAllowed.invoke(userId, defaultTestUpdatedName)
            createLabel.invoke(userId, defaultTestUpdatedName, defaultTestLabel.color)
        }
    }

    @Test
    fun `given create state and limit reached, when action label save, then emits error snack`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageLabel) } returns true.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showLabelLimitReachedSnackbar = Effect.of(Unit)
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action delete, then emits close with delete`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns defaultTestLabel.labelId.id
        coEvery {
            getLabel.invoke(userId, defaultTestLabel.labelId, LabelType.MessageLabel)
        } returns defaultTestLabel.right()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnDeleteClick)
            // Then
            assertEquals(
                loadedState.copy(closeWithDelete = Effect.of(Unit)),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and error in save, when action save, then emits save label error`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(LabelFormScreen.LabelFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageLabel) } returns DataError.Local.Unknown.left()

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            labelFormViewModel.submit(LabelFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showSaveLabelErrorSnackbar = Effect.of(Unit)
                ),
                awaitItem()
            )
        }
    }
}
