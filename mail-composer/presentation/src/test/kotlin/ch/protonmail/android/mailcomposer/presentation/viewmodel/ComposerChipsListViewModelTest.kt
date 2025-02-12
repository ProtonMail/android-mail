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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.chips.item.ChipItemsList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ComposerChipsListViewModelTest {

    private lateinit var viewModel: ComposerChipsListViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @BeforeTest
    fun setup() {
        viewModel = ComposerChipsListViewModel()
    }

    @Test
    fun `when adding an item it emits the updated state`() = runTest {
        // Given
        val newItem = ChipItem.Valid("aa@bb.cc")

        // When + Then
        viewModel.state.test {
            viewModel.state.value.listState.updateItems(listOf(newItem))

            val newState = awaitItem()
            assertEquals(ChipItemsList.Unfocused.Single(newItem), newState.listState.getItems())
            assertEquals(Effect.empty(), newState.duplicateRemovalWarning)
            assertEquals(Effect.empty(), newState.invalidEntryWarning)
        }
    }

    @Test
    fun `when adding a duplicate item it emits the error state`() = runTest {
        // Given
        val newItem = ChipItem.Valid("aa@bb.cc")
        val expectedDuplicatedEffect = Effect.of(TextUiModel(R.string.composer_error_duplicate_recipient))

        // When + Then
        viewModel.state.test {
            repeat(2) {
                viewModel.state.value.listState.type(newItem.value)
                viewModel.state.value.listState.type("\n")
            }

            skipItems(2)

            val newState = awaitItem()
            assertEquals(ChipItemsList.Unfocused.Single(newItem), newState.listState.getItems())
            assertEquals(expectedDuplicatedEffect, newState.duplicateRemovalWarning)
            assertEquals(Effect.empty(), newState.invalidEntryWarning)
        }
    }

    @Test
    fun `when adding an invalid item it emits the invalid entry warning`() = runTest {
        // Given
        val newItem = ChipItem.Invalid("__")
        val expectedInvalidEffect = Effect.of(TextUiModel(R.string.composer_error_invalid_email))

        // When + Then
        viewModel.state.test {
            viewModel.state.value.listState.type(newItem.value)
            viewModel.state.value.listState.type("\n")

            skipItems(1)

            val state = awaitItem()
            assertEquals(ChipItemsList.Unfocused.Single(newItem), state.listState.getItems())
            assertEquals(Effect.empty(), state.duplicateRemovalWarning)
            assertEquals(expectedInvalidEffect, state.invalidEntryWarning)

            cancelAndConsumeRemainingEvents()
        }
    }
}
