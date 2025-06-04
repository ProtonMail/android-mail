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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.model.UpsellingButtonState
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveMailboxOneClickUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class UpsellingButtonViewModel @Inject constructor(
    observeMailboxOneClickUpsellingVisibility: ObserveMailboxOneClickUpsellingVisibility,
    private val upsellingTelemetryRepository: UpsellingTelemetryRepository,
    private val driveSpotlightTelemetryRepository: DriveSpotlightTelemetryRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<UpsellingButtonState> = mutableState.asStateFlow()

    init {
        observeMailboxOneClickUpsellingVisibility()
            .onEach { visibility -> mutableState.update { UpsellingButtonState(visibility) } }
            .launchIn(viewModelScope)
    }

    fun trackButtonInteraction(type: UpsellingVisibility) {
        when (type) {
            UpsellingVisibility.HIDDEN -> return
            UpsellingVisibility.PROMO -> upsellingTelemetryRepository.trackEvent(
                UpsellingTelemetryEventType.Base.MailboxButtonTap,
                UpsellingEntryPoint.Feature.MailboxPromo
            )
            UpsellingVisibility.NORMAL -> upsellingTelemetryRepository.trackEvent(
                UpsellingTelemetryEventType.Base.MailboxButtonTap,
                UpsellingEntryPoint.Feature.Mailbox
            )
            UpsellingVisibility.DRIVE_SPOTLIGHT -> driveSpotlightTelemetryRepository.trackEvent(
                DriveSpotlightTelemetryEventType.MailboxDriveSpotlightButtonTap
            )
        }
    }

    companion object {

        val initialState = UpsellingButtonState(visibility = UpsellingVisibility.HIDDEN)
    }
}
