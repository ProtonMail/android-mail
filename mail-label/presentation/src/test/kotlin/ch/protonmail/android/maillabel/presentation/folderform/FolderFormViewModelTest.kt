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
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.UserUpgradeState
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.usecase.GetUpsellingVisibility
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("MaxLineLength")
class FolderFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val defaultTestFolder = buildLabel(
        id = "LabelID",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor(),
        isNotified = true
    )
    private val defaultTestParentFolder = buildLabel(
        id = "ParentLabelID",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor(),
        isNotified = true
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
        colorList = listOf(Color.Red),
        displayColorPicker = true,
        useFolderColor = defaultFolderColorSettings.useFolderColor,
        inheritParentFolderColor = defaultFolderColorSettings.inheritParentFolderColor
    )
    private val loadedUpdateState = FolderFormState.Data.Update(
        isSaveEnabled = true,
        name = defaultTestFolder.name,
        color = defaultTestFolder.color,
        parent = null,
        notifications = true,
        colorList = listOf(Color.Red),
        displayColorPicker = true,
        useFolderColor = defaultFolderColorSettings.useFolderColor,
        inheritParentFolderColor = defaultFolderColorSettings.inheritParentFolderColor,
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
    private val userUpgradeState = mockk<UserUpgradeState> {
        every { this@mockk.isUserPendingUpgrade } returns false
    }

    private val getUpsellingVisibility = mockk<GetUpsellingVisibility> {
        coEvery { this@mockk.invoke(any()) } returns false
    }

    private val reducer = FolderFormReducer()

    private val colorMapper = ColorMapper()

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
            getUpsellingVisibility,
            userUpgradeState,
            observeFolderColorSettings,
            reducer,
            colorMapper,
            observePrimaryUserId,
            savedStateHandle
        )
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
    fun `given id for label with parent, when init, then emits update folder state with parent`() = runTest {
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
        coEvery {
            isLabelNameAllowed.invoke(userId, defaultTestUpdatedName, loadedState.parent?.labelId)
        } returns true.right()
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
                    closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved)),
                    displayCreateLoader = true
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state, when action folder save fails, then emits show error snack`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName, any()) } returns true.right()
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns false.right()
        coEvery {
            createFolder.invoke(userId, any(), any(), any(), any())
        } returns DataError.Remote.Http(NetworkError.Unknown).left()

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
                    showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error)),
                    displayCreateLoader = false
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
        coEvery {
            isLabelNameAllowed.invoke(userId, defaultTestUpdatedName, loadedState.parent?.labelId)
        } returns true.right()
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
    fun `given update state and no changes, when action folder save, then emits close with success`() = runTest {
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
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(close = Effect.of(Unit)),
                awaitItem()
            )
        }
    }

    @Test
    fun `given name with spaces, when action folder save, then trim and emits close with success`() = runTest {
        // Given
        val loadedState = loadedUpdateState
        val nameWithWhitespaces = " $defaultTestUpdatedName "
        every {
            savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey)
        } returns defaultTestFolder.labelId.id
        coEvery { isLabelNameAllowed.invoke(userId, defaultTestUpdatedName, any()) } returns true.right()
        coEvery {
            getLabel.invoke(userId, defaultTestFolder.labelId, LabelType.MessageFolder)
        } returns defaultTestFolder.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.FolderNameChanged(nameWithWhitespaces))
            // Then
            assertEquals(loadedState.copy(name = nameWithWhitespaces), awaitItem())

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
            // Then
            assertEquals(
                loadedState.copy(
                    name = nameWithWhitespaces,
                    closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
                ),
                awaitItem()
            )
        }
        coVerify {
            // Verify that we use the name trimmed from leading and trailing whitespaces
            isLabelNameAllowed.invoke(userId, defaultTestUpdatedName, null)
            updateLabel.invoke(
                userId,
                defaultTestFolder.copy(name = defaultTestUpdatedName)
            )
        }
    }

    @Test
    fun `given create state and name already exists, when action folder save, then emits error snack`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns false.right()
        coEvery { isLabelNameAllowed.invoke(userId, any(), any()) } returns false.right()

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
    fun `given create state and limit reached, when action folder save, then emits error snack`() = runTest {
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
                    showNormSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and limit reached, when action folder save and observeUpsellingVisibility is true, emits ShowUpselling`() =
        runTest {
            // Given
            val loadedState = loadedCreateState
            every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
            coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns true.right()
            coEvery { getUpsellingVisibility.invoke(UpsellingEntryPoint.Feature.Folders) } returns true

            folderFormViewModel.state.test {
                // Initial loaded state
                val actual = awaitItem()
                assertEquals(loadedState, actual)

                // When
                folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)
                // Then
                assertEquals(
                    loadedState.copy(
                        upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show),
                        displayCreateLoader = false
                    ),
                    awaitItem()
                )
            }
        }

    @Test
    fun `given create state, limit reached, upselling in progress, emits upselling in progress`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns true.right()
        every { userUpgradeState.isUserPendingUpgrade } returns true

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.OnSaveClick)

            // Then
            assertEquals(
                loadedState.copy(
                    upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress)),
                    displayCreateLoader = false
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state with upselling shown, when action hide upselling, emits hide upselling`() = runTest {
        // Given
        val loadedState = loadedCreateState
        every { savedStateHandle.get<String>(FolderFormScreen.FolderFormLabelIdKey) } returns null
        coEvery { isLabelLimitReached.invoke(userId, LabelType.MessageFolder) } returns true.right()

        folderFormViewModel.state.test {
            // Initial loaded state
            val actual = awaitItem()
            assertEquals(loadedState, actual)

            // When
            folderFormViewModel.submit(FolderFormViewAction.HideUpselling)
            // Then
            assertEquals(
                loadedState.copy(
                    upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide),
                    displayCreateLoader = false
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action delete requested, then emits show dialog`() = runTest {
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
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteRequested)

            // Then
            assertEquals(
                loadedState.copy(
                    confirmDeleteDialogState = DeleteDialogState.Shown(
                        title = TextUiModel.TextRes(R.string.delete_folder),
                        message = TextUiModel.TextRes(R.string.delete_folder_message)
                    )
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action delete canceled, then emits hide dialog`() = runTest {
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
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteRequested)
            skipItems(1)
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteCanceled)

            // Then
            assertEquals(
                loadedState.copy(
                    confirmDeleteDialogState = DeleteDialogState.Hidden
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given update state, when action delete confirmed, then emits hide dialog and close with success`() = runTest {
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
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteRequested)
            skipItems(1)
            folderFormViewModel.submit(FolderFormViewAction.OnDeleteConfirmed)

            // Then
            assertEquals(
                loadedState.copy(
                    closeWithSuccess = Effect.of(TextUiModel(R.string.folder_deleted)),
                    confirmDeleteDialogState = DeleteDialogState.Hidden
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `given create state and error in save, when action save, then emits error snack`() = runTest {
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
