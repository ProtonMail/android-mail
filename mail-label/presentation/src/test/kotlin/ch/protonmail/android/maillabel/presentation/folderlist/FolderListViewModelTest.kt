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

package ch.protonmail.android.maillabel.presentation.folderlist

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.maillabel.presentation.model.toFolderUiModel
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.UpdateEnableFolderColor
import ch.protonmail.android.mailsettings.domain.usecase.UpdateInheritFolderColor
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.label.LabelTestData.systemLabelsAsMessageFolders
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class FolderListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val defaultTestFolder = LabelTestData.buildLabel(
        id = "LabelID",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor()
    )
    private val mixedTestFolders = buildList {
        add(defaultTestFolder)
        addAll(systemLabelsAsMessageFolders(userId))
    }

    private val defaultFolderColorSettings = FolderColorSettings(
        useFolderColor = true,
        inheritParentFolderColor = false
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(userId) } returns flowOf(defaultFolderColorSettings)
    }
    private val observeLabels = mockk<ObserveLabels>()
    private val updateEnableFolderColor = mockk<UpdateEnableFolderColor>()
    private val updateInheritFolderColor = mockk<UpdateInheritFolderColor>()

    private val reducer = FolderListReducer()
    private val colorMapper = ColorMapper()

    private val folderListViewModel by lazy {
        FolderListViewModel(
            observeLabels,
            reducer,
            observeFolderColorSettings,
            updateEnableFolderColor,
            updateInheritFolderColor,
            colorMapper,
            observePrimaryUserId
        )
    }

    @Test
    fun `given empty folder list, when init, then emits empty state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(emptyList<Label>().right())

        // When
        folderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Empty(useFolderColor = true, inheritParentFolderColor = false)

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given folder list, when init, then emits data state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())

        // When
        folderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Data(
                useFolderColor = true,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings, colorMapper)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given folder list, when init, then emits data state with filtered non-system folders`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(mixedTestFolders.right())

        // When
        folderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Data(
                useFolderColor = true,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings, colorMapper)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given error on loading folder list, when init, then emits error state`() = runTest {
        // Given
        every {
            observeLabels.invoke(userId, labelType = LabelType.MessageFolder)
        } returns flowOf(DataError.Local.Unknown.left())

        // When
        folderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = FolderListState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.folder_list_loading_error))
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given folder list, when action add folder, then emits open folder form state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())

        // When
        folderListViewModel.state.test {
            awaitItem()

            folderListViewModel.submit(FolderListViewAction.OnAddFolderClick)

            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Data(
                useFolderColor = true,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings, colorMapper),
                openFolderForm = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given folder list, when action disable use folder color, then emits updated data state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())
        coEvery {
            updateEnableFolderColor(userId = userId, false)
        } returns Unit

        // When
        folderListViewModel.state.test {
            awaitItem()

            folderListViewModel.submit(FolderListViewAction.OnChangeUseFolderColor(false))

            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Data(
                useFolderColor = false,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings, colorMapper)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given folder list, when action enable inherit parent color, then emits updated data state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())
        coEvery {
            updateInheritFolderColor(userId = userId, true)
        } returns Unit

        // When
        folderListViewModel.state.test {
            awaitItem()

            folderListViewModel.submit(FolderListViewAction.OnChangeInheritParentFolderColor(true))

            val actual = awaitItem()
            val expected = FolderListState.ListLoaded.Data(
                useFolderColor = true,
                inheritParentFolderColor = true,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings, colorMapper)
            )

            assertEquals(expected, actual)
        }
    }
}
