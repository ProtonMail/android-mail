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

package ch.protonmail.upselling.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType.Base
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.model.UpsellingButtonState
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveMailboxOneClickUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpsellingButtonViewModelTest {

    private val oneClickUpsellingVisibility = mockk<ObserveMailboxOneClickUpsellingVisibility>()
    private val upsellingTelemetryRepository = mockk<UpsellingTelemetryRepository>(relaxUnitFun = true)

    private val viewModel: UpsellingButtonViewModel by lazy {
        UpsellingButtonViewModel(oneClickUpsellingVisibility, upsellingTelemetryRepository)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should emit not shown when visibility is false`() = runTest {
        // Given
        every { oneClickUpsellingVisibility.invoke() } returns flowOf(false)
        val expected = UpsellingButtonState(isShown = false)

        // When + Then
        viewModel.state.test {
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `should emit shown when visibility is true`() = runTest {
        // Given
        every { oneClickUpsellingVisibility.invoke() } returns flowOf(true)
        val expected = UpsellingButtonState(isShown = true)

        // When + Then
        viewModel.state.test {
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `should call the UC with the expected event when tracking the upselling button tap`() = runTest {
        // Given
        every { oneClickUpsellingVisibility.invoke() } returns flowOf(true)

        // When
        viewModel.trackButtonInteraction()

        // Then
        coVerify(exactly = 1) {
            upsellingTelemetryRepository.trackEvent(Base.MailboxButtonTap, UpsellingEntryPoint.BottomSheet.Mailbox)
        }
    }
}
