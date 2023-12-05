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

package ch.protonmail.android.maillabel.presentation.folderparentlist

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
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.maillabel.presentation.model.toFolderUiModel
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.label.LabelTestData
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
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class ParentFolderListViewModelTest {

    private val defaultTestFolder = LabelTestData.buildLabel(
        id = "LabelId",
        type = LabelType.MessageFolder,
        color = Color.Red.getHexStringFromColor()
    )
    private val defaultFolderColorSettings = FolderColorSettings(
        useFolderColor = true,
        inheritParentFolderColor = false
    )

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val savedStateHandle = mockk<SavedStateHandle>()
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(UserIdTestData.userId) } returns flowOf(defaultFolderColorSettings)
    }
    private val observeLabels = mockk<ObserveLabels>()

    private val reducer = ParentFolderListReducer()

    private val parentFolderListViewModel by lazy {
        ParentFolderListViewModel(
            observeLabels,
            reducer,
            observeFolderColorSettings,
            observePrimaryUserId,
            savedStateHandle
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
    fun `given empty parent folder list, when init, then emits empty state`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListLabelIdKey) } returns null
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListParentLabelIdKey) } returns null
        coEvery {
            observeLabels(userId = UserIdTestData.userId, labelType = LabelType.MessageFolder)
        } returns flowOf(emptyList<Label>().right())

        // When
        parentFolderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ParentFolderListState.ListLoaded.Empty(
                labelId = null,
                parentLabelId = null
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given parent folder list, when init, then emits data state`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListLabelIdKey) } returns null
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListParentLabelIdKey) } returns null
        coEvery {
            observeLabels(userId = UserIdTestData.userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())

        // When
        parentFolderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ParentFolderListState.ListLoaded.Data(
                labelId = null,
                parentLabelId = null,
                useFolderColor = true,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given label ids and parent folder list, when init, then emits data state`() = runTest {
        // Given
        val parentLabelId = LabelId("ParentLabelId")
        every {
            savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListLabelIdKey)
        } returns defaultTestFolder.labelId.id
        every {
            savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListParentLabelIdKey)
        } returns parentLabelId.id
        coEvery {
            observeLabels(userId = UserIdTestData.userId, labelType = LabelType.MessageFolder)
        } returns flowOf(listOf(defaultTestFolder).right())

        // When
        parentFolderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ParentFolderListState.ListLoaded.Data(
                labelId = defaultTestFolder.labelId,
                parentLabelId = parentLabelId,
                useFolderColor = true,
                inheritParentFolderColor = false,
                folders = listOf(defaultTestFolder).toFolderUiModel(defaultFolderColorSettings)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given error on loading parent folder list, when init, then emits error state`() = runTest {
        // Given
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListLabelIdKey) } returns null
        every { savedStateHandle.get<String>(ParentFolderListScreen.ParentFolderListParentLabelIdKey) } returns null
        every {
            observeLabels.invoke(UserIdTestData.userId, labelType = LabelType.MessageFolder)
        } returns flowOf(DataError.Local.Unknown.left())

        // When
        parentFolderListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = ParentFolderListState.Loading(
                errorLoading = Effect.of(TextUiModel(R.string.folder_list_loading_error))
            )

            assertEquals(expected, actual)
        }
    }
}
