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

import java.time.Instant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightTelemetryEventType
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentEvent
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentViewEvent
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightUIState
import ch.protonmail.android.mailupselling.presentation.reducer.DriveSpotlightContentReducer
import ch.protonmail.android.mailupselling.presentation.usecase.UpdateDriveSpotlightLastTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DriveSpotlightViewModel @Inject constructor(
    private val driveSpotlightTelemetryRepository: DriveSpotlightTelemetryRepository,
    private val updateDriveSpotlightLastTimestamp: UpdateDriveSpotlightLastTimestamp,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val reducer: DriveSpotlightContentReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow<DriveSpotlightUIState>(DriveSpotlightUIState.Loading)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = observePrimaryUserId.invoke().firstOrNull()
            if (userId == null) {
                return@launch emitNewStateFrom(DriveSpotlightContentEvent.UserError)
            }
            emitNewStateFrom(DriveSpotlightContentEvent.DataLoaded)
        }
    }

    fun submit(event: DriveSpotlightContentViewEvent) = viewModelScope.launch {
        when (event) {
            DriveSpotlightContentViewEvent.ContentShown -> updateLastSeenTimestamp()
            DriveSpotlightContentViewEvent.OpenDriveClicked -> trackCTAClicked()
        }
    }

    private suspend fun updateLastSeenTimestamp() {
        updateDriveSpotlightLastTimestamp.invoke(Instant.now().toEpochMilli())
    }

    private fun trackCTAClicked() {
        driveSpotlightTelemetryRepository.trackEvent(DriveSpotlightTelemetryEventType.DriveSpotlightCTATap)
    }

    private fun emitNewStateFrom(operation: DriveSpotlightContentEvent) {
        mutableState.update { reducer.newStateFrom(operation) }
    }
}
