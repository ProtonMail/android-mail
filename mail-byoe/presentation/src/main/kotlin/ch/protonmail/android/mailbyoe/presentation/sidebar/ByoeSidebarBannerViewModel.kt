/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.presentation.sidebar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.usecase.CloseByoeSidebarBanner
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeSidebarBannerTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The temporary BYOE entry in the sidebar. The SDK owns the 14-day window, so the visibility is asked again
 * each time the sidebar opens.
 */
@HiltViewModel
class ByoeSidebarBannerViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val getByoeSidebarBannerTreatment: GetByoeSidebarBannerTreatment,
    private val closeByoeSidebarBanner: CloseByoeSidebarBanner,
    private val reportByoeTelemetryEvent: ReportByoeTelemetryEvent
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow<ByoeSidebarBannerState>(ByoeSidebarBannerState.Hidden)
    val state: StateFlow<ByoeSidebarBannerState> = mutableState.asStateFlow()

    private var refreshJob: Job? = null
    private var isClosed = false

    fun submit(action: ByoeSidebarBannerAction) {
        when (action) {
            ByoeSidebarBannerAction.SidebarOpened -> refresh()
            ByoeSidebarBannerAction.ConnectClicked ->
                reportByoeTelemetryEvent(ByoeTelemetryEvent.ExploreClicked(ByoeSurface.SidebarBanner))
            ByoeSidebarBannerAction.CloseClicked -> close()
        }
    }

    private fun refresh() {
        // A closed banner never comes back, even before the SDK has recorded the close.
        if (isClosed) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val userId = primaryUserId.first()
            val treatment = getByoeSidebarBannerTreatment(userId)
            mutableState.value = treatment?.let(ByoeSidebarBannerState::Visible) ?: ByoeSidebarBannerState.Hidden
            // Once per sidebar open; the SDK sends only the first on this install.
            if (treatment != null) reportByoeTelemetryEvent(ByoeTelemetryEvent.SidebarBannerShown, userId)
        }
    }

    private fun close() {
        isClosed = true
        refreshJob?.cancel()
        mutableState.value = ByoeSidebarBannerState.Hidden
        viewModelScope.launch { closeByoeSidebarBanner(primaryUserId.first(), ByoeDismissMethod.Button) }
    }
}
