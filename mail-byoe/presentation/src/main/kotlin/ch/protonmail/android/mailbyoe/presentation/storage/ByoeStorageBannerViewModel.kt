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

package ch.protonmail.android.mailbyoe.presentation.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbyoe.domain.usecase.ObserveByoeImportPausedForStorage
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The mailbox banner shown while a Gmail import is paused because the user ran out of storage. It goes away by
 * itself once the import resumes after an upgrade, or is cancelled or undone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ByoeStorageBannerViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    observeByoeImportPausedForStorage: ObserveByoeImportPausedForStorage,
    private val dismissal: ByoeStorageBannerDismissal
) : ViewModel() {

    val state: StateFlow<ByoeStorageBannerState> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            combine(observeByoeImportPausedForStorage(userId), dismissal.isDismissed(userId)) { isPaused, isDismissed ->
                if (isPaused && !isDismissed) ByoeStorageBannerState.Visible else ByoeStorageBannerState.Hidden
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ByoeStorageBannerState.Hidden)

    fun submit(action: ByoeStorageBannerAction) {
        when (action) {
            ByoeStorageBannerAction.NotNowClicked -> viewModelScope.launch {
                dismissal.dismiss(observePrimaryUserId().filterNotNull().first())
            }
        }
    }

    private companion object {

        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
