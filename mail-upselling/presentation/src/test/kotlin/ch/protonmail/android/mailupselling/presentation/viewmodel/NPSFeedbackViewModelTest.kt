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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailupselling.domain.usecase.RecordNPSFeedbackTriggered
import ch.protonmail.android.mailupselling.domain.usecase.SkipNPSFeedback
import ch.protonmail.android.mailupselling.domain.usecase.SubmitNPSFeedback
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackUIState
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import ch.protonmail.android.mailupselling.presentation.reducer.NPSFeedbackContentReducer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class NPSFeedbackViewModelTest {

    private val reducer = NPSFeedbackContentReducer()
    private val recordNPSFeedbackTriggered = mockk<RecordNPSFeedbackTriggered>()
    private val submitNPSFeedback = mockk<SubmitNPSFeedback>()
    private val skipNPSFeedback = mockk<SkipNPSFeedback>()

    private val initialState = NPSFeedbackUIState(
        selection = null,
        feedbackText = TextFieldValue(),
        showSuccess = Effect.empty(),
        submitted = false,
        submitEnabled = false
    )

    private val viewModel: NPSFeedbackViewModel by lazy {
        NPSFeedbackViewModel(
            recordNPSFeedbackTriggered = recordNPSFeedbackTriggered,
            submitNPSFeedback = submitNPSFeedback,
            skipNPSFeedback = skipNPSFeedback,
            reducer = reducer
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit initial state at start`() = runTest {
        // When + Then
        viewModel.state.test {
            assertEquals(
                initialState,
                awaitItem()
            )
        }
    }


    @Test
    fun `should record triggered when displayed`() = runTest {
        // Given
        coEvery { recordNPSFeedbackTriggered.invoke() } just runs

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.ContentShown)
        viewModel.state.test {
            assertEquals(
                initialState,
                awaitItem()
            )
            coVerify(exactly = 1) { recordNPSFeedbackTriggered.invoke() }
        }
    }

    @Test
    fun `should update rating value`() = runTest {
        // Given

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.OptionSelected(9))
        viewModel.state.test {
            assertEquals(
                initialState.copy(selection = 9, submitEnabled = true),
                awaitItem()
            )
        }
    }

    @Test
    fun `should update text field`() = runTest {
        // Given

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.FeedbackChanged(TextFieldValue("new")))
        viewModel.state.test {
            assertEquals(
                initialState.copy(feedbackText = TextFieldValue("new")),
                awaitItem()
            )
        }
    }

    @Test
    fun `should mark as submitted once submit tapped`() = runTest {
        // Given
        coEvery { submitNPSFeedback.invoke("new", 10) } just runs

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.OptionSelected(10))
        viewModel.submit(NPSFeedbackViewEvent.FeedbackChanged(TextFieldValue("new")))
        viewModel.submit(NPSFeedbackViewEvent.SubmitClicked)
        viewModel.state.test {
            assertEquals(
                initialState.copy(
                    feedbackText = TextFieldValue("new"), selection = 10, submitted = true,
                    showSuccess = Effect.of(
                        Unit
                    )
                ),
                awaitItem()
            )
            coVerify(exactly = 1) { submitNPSFeedback.invoke("new", 10) }
        }
    }

    @Test
    fun `should record skipped when dismissed`() = runTest {
        // Given
        coEvery { skipNPSFeedback.invoke() } just runs

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.Dismissed)
        viewModel.state.test {
            assertEquals(
                initialState,
                awaitItem()
            )
            coVerify(exactly = 1) { skipNPSFeedback.invoke() }
        }
    }

    @Test
    fun `should not mark as skipped once submit tapped`() = runTest {
        // Given
        coEvery { submitNPSFeedback.invoke("new1", 9) } just runs

        // When + Then
        viewModel.submit(NPSFeedbackViewEvent.OptionSelected(9))
        viewModel.submit(NPSFeedbackViewEvent.FeedbackChanged(TextFieldValue("new1")))
        viewModel.submit(NPSFeedbackViewEvent.SubmitClicked)
        viewModel.submit(NPSFeedbackViewEvent.Dismissed)
        viewModel.state.test {
            assertEquals(
                initialState.copy(
                    feedbackText = TextFieldValue("new1"), selection = 9, submitted = true,
                    showSuccess = Effect.of(
                        Unit
                    )
                ),
                awaitItem()
            )
            coVerify(exactly = 1) { submitNPSFeedback.invoke("new1", 9) }
        }
    }
}
