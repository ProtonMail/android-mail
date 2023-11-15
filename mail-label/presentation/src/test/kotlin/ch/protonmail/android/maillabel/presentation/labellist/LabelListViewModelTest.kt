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

import android.util.Log
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLabels
import ch.protonmail.android.testdata.label.LabelTestData
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
import me.proton.core.label.domain.entity.Label
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LabelListViewModelTest {

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelID")

    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeLabels = mockk<ObserveLabels> {
        every { this@mockk.invoke(userId) } returns flowOf(
            listOf(defaultTestLabel).right()
        )
    }

    private val reducer = mockk<LabelListReducer> {
        every { newStateFrom(any(), any()) } returns LabelListState.Loading()
    }

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
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `emits empty state`() = runTest {
        // Given
        coEvery { observePrimaryUserId() } returns flowOf(userId)
        coEvery { observeLabels(userId = any()) } returns flowOf(emptyList<Label>().right())
        every {
            reducer.newStateFrom(
                LabelListState.Loading(),
                LabelListEvent.LabelListLoaded(emptyList())
            )
        } returns LabelListState.Data(emptyList())

        // When
        labelListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = LabelListState.Data(labels = emptyList())

            assertEquals(expected, actual)
        }
    }

    @Test
    fun `emits data state`() = runTest {
        // Given
        every {
            reducer.newStateFrom(
                LabelListState.Loading(),
                LabelListEvent.LabelListLoaded(listOf(defaultTestLabel))
            )
        } returns LabelListState.Data(listOf(defaultTestLabel))

        // When
        labelListViewModel.state.test {
            // Then
            val actual = awaitItem()
            val expected = LabelListState.Data(
                listOf(defaultTestLabel)
            )

            assertEquals(expected, actual)
        }
    }

}
