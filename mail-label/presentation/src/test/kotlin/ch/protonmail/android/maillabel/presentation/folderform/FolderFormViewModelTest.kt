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

package ch.protonmail.android.maillabel.presentation.folderform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.usecase.CreateFolder
import ch.protonmail.android.maillabel.domain.usecase.DeleteLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabel
import ch.protonmail.android.maillabel.domain.usecase.GetLabelColors
import ch.protonmail.android.maillabel.domain.usecase.IsLabelLimitReached
import ch.protonmail.android.maillabel.domain.usecase.IsLabelNameAllowed
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
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
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class FolderFormViewModelTest {

    private val defaultTestFolder = buildLabel(
        id = "LabelID",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor()
    )
    private val defaultTestParentFolder = buildLabel(
        id = "ParentLabelID",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor()
    )
    private val defaultTestUpdatedName = "UpdatedName"
    private val defaultFolderColorSettings = FolderColorSettings(
        useFolderColor = true,
        inheritParentFolderColor = false
    )

    private val loadedCreateState = FolderFormState.Data.Create(
        isSaveEnabled = false,
        name = "",
        color = defaultTestFolder.color,
        parent = null,
        notifications = true,
        colorList = listOf(Color.Red)
    )
    private val loadedUpdateState = FolderFormState.Data.Update(
        isSaveEnabled = true,
        name = defaultTestFolder.name,
        color = defaultTestFolder.color,
        parent = null,
        notifications = true,
        colorList = listOf(Color.Red),
        labelId = defaultTestFolder.labelId
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(userId) } returns flowOf(defaultFolderColorSettings)
    }

    private val getLabel = mockk<GetLabel>()
    private val createFolder = mockk<CreateFolder> {
        coEvery { this@mockk.invoke(userId, any(), any(), any(), any()) } returns
            Unit.right()
    }
    private val updateLabel = mockk<UpdateLabel> {
        coEvery { this@mockk.invoke(userId, any()) } returns
            Unit.right()
    }
    private val deleteLabel = mockk<DeleteLabel> {
        coEvery { this@mockk.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder) } returns
            Unit.right()
    }

    private val getLabelColors = mockk<GetLabelColors> {
        every { this@mockk.invoke() } returns listOf(defaultTestFolder.color)
    }

    private val isLabelNameAllowed = mockk<IsLabelNameAllowed>()
    private val isLabelLimitReached = mockk<IsLabelLimitReached>()

    private val reducer = FolderFormReducer()

    private val savedStateHandle = mockk<SavedStateHandle>()

    private val folderFormViewModel by lazy {
        FolderFormViewModel(
            getLabel,
            createFolder,
            updateLabel,
            deleteLabel,
            getLabelColors,
            isLabelNameAllowed,
            isLabelLimitReached,
            observeFolderColorSettings,
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
    fun `given null label id value, when init, then emits create folder state`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null

        // When
        folderFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `given label id value, when init, then emits update folder state`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every {
            savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        } returns defaultTestFolder.labelId.id
        coEvery {
            getLabel.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestFolder.right()

        // When
        folderFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `given id for label with parent, when init, then emits update folder state`() = runTest {
        // Given
        val defaultTestFolder = defaultTestFolder.copy(parentId = defaultTestParentFolder.labelId)
        val loadedState = loadedUpdateState.copy(parent = defaultTestParentFolder)
        every {
            savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        } returns defaultTestFolder.labelId.id
        coEvery {
            getLabel.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestFolder.right()
        coEvery {
            getLabel.invoke(userId, defaultTestParentFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestParentFolder.right()

        // When
        folderFormViewModel.state.test {
            // Then
            val actual = awaitItem()
            assertEquals(loadedState, actual)
        }
    }

    @Test
    fun `given create state, when action folder name changed, then emits updated folder`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())
        }
    }

    @Test
    fun `given create state, when action folder color changed, then emits updated folder`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderColorChanged(Color.Blue))
            // Then
            assertEquals(loadedState.copy(color = Color.Blue.getHexStringFromColor()), awaitItem())
        }
    }

    @Test
    fun `given create state, when action folder parent changed, then emits updated folder`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery {
            getLabel.invoke(userId, defaultTestParentFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestParentFolder.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderParentChanged(defaultTestParentFolder.labelId))
            // Then
            assertEquals(loadedState.copy(parent = defaultTestParentFolder), awaitItem())
        }
    }

    @Test
    fun `given create state, when action folder notifications changed, then emits updated folder`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNotificationsChanged(false))
            // Then
            assertEquals(loadedState.copy(notifications = false), awaitItem())
        }
    }

    @Test
    fun `given create state, when action folder save, then emits close with success`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName) } returns true.right()
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns false.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action folder save, then emits close with success`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every {
            savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        } returns defaultTestFolder.labelId.id
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName) } returns true.right()
        coEvery {
            getLabel.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestFolder.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and name already exists, when action folder save, then emits close with save`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns false.right()
        coEvery { isLabelNameAllowed.invoke(userId, any()) } returns false.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.label_already_exists))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and limit reached, when action folder save, then emits close with save`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns true.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action delete, then emits close with delete`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        every {
            savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        } returns defaultTestFolder.labelId.id
        coEvery {
            getLabel.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestFolder.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteClick)
            // Then
            assertEquals(
                loadedState.copy(closeWithSuccess = Effect.of(TextUiModel(R.string.folder_deleted))),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and error in save, when action save, then emits save folder error`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns DataError.Local.Unknown.left()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(defaultTestUpdatedName))
            // Then
            assertEquals(loadedState.copy(name = defaultTestUpdatedName, isSaveEnabled = true), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = defaultTestUpdatedName,
                    isSaveEnabled = true,
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error))
                ),
                awaitItem()
            )
        }
    }
}
