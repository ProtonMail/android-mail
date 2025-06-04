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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightTelemetryEventType
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentViewEvent
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightUIState
import ch.protonmail.android.mailupselling.presentation.reducer.DriveSpotlightContentReducer
import ch.protonmail.android.mailupselling.presentation.usecase.UpdateDriveSpotlightLastTimestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class DriveSpotlightViewModelTest {

    private val telemetryRepository = mockk<DriveSpotlightTelemetryRepository>(relaxUnitFun = true)
    private val reducer = DriveSpotlightContentReducer()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val updateDriveSpotlightLastTimestamp = mockk<UpdateDriveSpotlightLastTimestamp>()

    private val viewModel: DriveSpotlightViewModel by lazy {
        DriveSpotlightViewModel(
            driveSpotlightTelemetryRepository = telemetryRepository,
            updateDriveSpotlightLastTimestamp = updateDriveSpotlightLastTimestamp,
            observePrimaryUserId = observePrimaryUserId,
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
    fun `should emit loading state at start`() = runTest {
        coEvery { observePrimaryUserId.invoke() } returns flowOf(UserIdSample.Primary)
        // When + Then
        viewModel.state.test {
            assertEquals(
                DriveSpotlightUIState.Data,
                awaitItem()
            )
        }
    }

    @Test
    fun `should emit error when drive storage can not be determined`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(null)

        // When + Then
        viewModel.state.test {
            assertEquals(
                DriveSpotlightUIState.Error(
                    Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
                ),
                awaitItem()
            )
        }
    }

    @Test
    fun `should call update use case when displayed`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(UserIdSample.Primary)
        coEvery { updateDriveSpotlightLastTimestamp.invoke(any()) } just runs

        // When + Then
        viewModel.submit(DriveSpotlightContentViewEvent.ContentShown)
        viewModel.state.test {
            assertEquals(
                DriveSpotlightUIState.Data,
                awaitItem()
            )
            coVerify(exactly = 1) { updateDriveSpotlightLastTimestamp.invoke(any()) }
        }
    }

    @Test
    fun `should call upselling repo when CTA clicked`() = runTest {
        // Given
        coEvery { observePrimaryUserId.invoke() } returns flowOf(UserIdSample.Primary)
        coEvery {
            telemetryRepository.trackEvent(DriveSpotlightTelemetryEventType.DriveSpotlightCTATap)
        } just runs

        // When + Then
        viewModel.submit(DriveSpotlightContentViewEvent.OpenDriveClicked)
        viewModel.state.test {
            assertEquals(
                DriveSpotlightUIState.Data,
                awaitItem()
            )
            coVerify(exactly = 1) {
                telemetryRepository.trackEvent(DriveSpotlightTelemetryEventType.DriveSpotlightCTATap)
            }
        }
    }
}
