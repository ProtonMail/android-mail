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

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.usecase.CreateLabel
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
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
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LabelFormViewModelTest {

    private val defaultTestLabel = buildLabel(id = "LabelID", color = Color.Red.getHexStringFromColor())
    private val defaultTestUpdatedName = "UpdatedName"

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val getLabel = mockk<GetLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestLabel.labelId) } returns
            defaultTestLabel.right()
    }

    private val createLabel = mockk<CreateLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestLabel.name, defaultTestLabel.color) } returns
            Unit.right()
    }

    private val updateLabel = mockk<UpdateLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestLabel.copy(name = defaultTestUpdatedName)) } returns
            Unit.right()
    }

    private val deleteLabel = mockk<DeleteLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestLabel.labelId) } returns
            Unit.right()
    }

    private val getLabelColors = mockk<GetLabelColors> {
        every { this@mockk.invoke() } returns listOf(defaultTestLabel.color)
    }

    private val reducer = mockk<LabelFormReducer> {
        every { newStateFrom(any(), any()) } returns LabelFormState.Loading()
    }

    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(LabelFormScreen.LabelIdKey) } returns defaultTestLabel.labelId.id
    }

    private val labelFormViewModel by lazy {
        LabelFormViewModel(
            getLabel,
            createLabel,
            updateLabel,
            deleteLabel,
            getLabelColors,
            reducer,
            observePrimaryUserId,
            savedStateHandle
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
        mockkStatic(android.graphics.Color::class)
        every { android.graphics.Color.parseColor(Color.Red.getHexStringFromColor()) } returns Color.Red.toArgb()
        every { android.graphics.Color.parseColor(Color.Blue.getHexStringFromColor()) } returns Color.Blue.toArgb()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `emits create state`() = runTest {
        val loadedState = LabelFormState.Data.Create(
            isSaveEnabled = false,
            name = "",
            color = defaultTestLabel.color,
            colorList = listOf(Color.Red),
            close = Effect.empty(),
            closeWithSave = Effect.empty()
        )
        every { savedStateHandle.get<String>(LabelFormScreen.LabelIdKey) } returns null
        every {
            reducer.newStateFrom(
                LabelFormState.Loading(),
                LabelFormEvent.LabelLoaded(
                    null,
                    name = "",
                    color = defaultTestLabel.color,
                    colorList = listOf(Color.Red)
                )
            )
        } returns loadedState

        // When
        labelFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `emits update state`() = runTest {
        val loadedState = LabelFormState.Data.Update(
            isSaveEnabled = true,
            name = defaultTestLabel.name,
            color = defaultTestLabel.color,
            colorList = listOf(Color.Red),
            close = Effect.empty(),
            closeWithSave = Effect.empty(),
            labelId = defaultTestLabel.labelId,
            closeWithDelete = Effect.empty()
        )
        every {
            reducer.newStateFrom(
                LabelFormState.Loading(),
                LabelFormEvent.LabelLoaded(
                    labelId = defaultTestLabel.labelId,
                    name = defaultTestLabel.name,
                    color = defaultTestLabel.color,
                    colorList = listOf(Color.Red)
                )
            )
        } returns loadedState

        // When
        labelFormViewModel.state.test {
            // Then
            val actual = awaitItem()

            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `when label is updated`() = runTest {
        val loadedState = LabelFormState.Data.Create(
            isSaveEnabled = false,
            name = "",
            color = defaultTestLabel.color,
            colorList = listOf(Color.Red),
            close = Effect.empty(),
            closeWithSave = Effect.empty()
        )
        every { savedStateHandle.get<String>(LabelFormScreen.LabelIdKey) } returns null
        every {
            reducer.newStateFrom(
                LabelFormState.Loading(),
                LabelFormEvent.LabelLoaded(
                    null,
                    name = "",
                    color = defaultTestLabel.color,
                    colorList = listOf(Color.Red)
                )
            )
        } returns loadedState
        every {
            reducer.newStateFrom(
                loadedState,
                LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName)
            )
        } returns loadedState.copy(name = defaultTestUpdatedName)

        labelFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()

            assertEquals(loadedState, actual)

            // When
            labelFormViewModel.submit(LabelFormViewAction.LabelNameChanged(defaultTestUpdatedName))

            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName), awaitItem())
        }
    }

    @Test
    fun `when color is updated`() = runTest {
        val loadedState = LabelFormState.Data.Create(
            isSaveEnabled = false,
            name = "",
            color = defaultTestLabel.color,
            colorList = listOf(Color.Red),
            close = Effect.empty(),
            closeWithSave = Effect.empty()
        )
        every { savedStateHandle.get<String>(LabelFormScreen.LabelIdKey) } returns null
        every {
            reducer.newStateFrom(
                LabelFormState.Loading(),
                LabelFormEvent.LabelLoaded(
                    null,
                    name = "",
                    color = defaultTestLabel.color,
                    colorList = listOf(Color.Red)
                )
            )
        } returns loadedState
        every {
            reducer.newStateFrom(
                loadedState,
                LabelFormViewAction.LabelColorChanged(Color.Blue)
            )
        } returns loadedState.copy(color = Color.Blue.getHexStringFromColor())

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
}
