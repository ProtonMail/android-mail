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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Whether the search screen may offer to set content search up for the primary account.
 *
 * Content search already on, unavailable on this build, or unavailable on a 32-bit device all read
 * as nothing to offer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContentSearchSetUpPromptViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled
) : ViewModel() {

    val canSetUpContentSearch: StateFlow<Boolean> = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { userId -> observeContentSearchEnabled(userId) }
        .distinctUntilChanged()
        .map { isEnabled -> !isEnabled && isContentSearchFeatureEnabled() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
