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

package ch.protonmail.android.maillabel.presentation.labellist

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LabelListViewModelTest {

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelID")

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }
    private val observeLabels = mockk<ObserveLabels>()

    private val reducer = LabelListReducer()

    private val labelListViewModel by lazy {
        LabelListViewModel(
            observeLabels,
            reducer,
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
    }

    @Test
    fun `given empty label list, when init, then emits empty state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageLabel)
        } returns flowOf(emptyList<Label>().right())

        // When
        labelListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = LabelListState.ListLoaded.Empty()

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given label list, when init, then emits data state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageLabel)
        } returns flowOf(listOf(defaultTestLabel).right())

        // When
        labelListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = LabelListState.ListLoaded.Data(
                labels = listOf(defaultTestLabel)
            )

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given error on loading label list, when init, then emits error state`() = runTest {
        // Given
        every {
            observeLabels.invoke(userId, labelType = LabelType.MessageLabel)
        } returns flowOf(DataError.Local.Unknown.left())

        // When
        labelListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = LabelListState.Loading(errorLoading = Effect.of(Unit))

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `given label list, when action add label, then emits open label form state`() = runTest {
        // Given
        coEvery {
            observeLabels(userId = userId, labelType = LabelType.MessageLabel)
        } returns flowOf(listOf(defaultTestLabel).right())

        // When
        labelListViewModel.state.test {
            awaitItem()

            labelListViewModel.submit(LabelListViewAction.OnAddLabelClick)

            val actual = awaitItem()
            val expected = LabelListState.ListLoaded.Data(
                labels = listOf(defaultTestLabel),
                openLabelForm = Effect.of(Unit)
            )

            assertEquals(expected, actual)
        }
    }
}
