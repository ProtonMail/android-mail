/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.feature.contentsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeContentSearchBottomSheetViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val isContentSearchAllowedOnMobileData: IsContentSearchAllowedOnMobileData,
    private val setAllowContentSearchOnMobileData: SetAllowContentSearchOnMobileData,
    private val enableContentSearch: EnableContentSearch,
    private val startContentIndexingForUser: StartContentIndexingForUser
) : ViewModel() {

    private val mutableState = MutableStateFlow<ContentSearchBottomSheetState>(ContentSearchBottomSheetState.Hide)
    val state: StateFlow<ContentSearchBottomSheetState> = mutableState.asStateFlow()

    private val mutableIsMobileDataEnabled = MutableStateFlow(DefaultAllowMobileData)
    val isMobileDataEnabled: StateFlow<Boolean> = mutableIsMobileDataEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            mutableIsMobileDataEnabled.value = isContentSearchAllowedOnMobileData()
        }
    }

    fun submit(action: HomeContentSearchBottomSheetViewAction) {
        when (action) {
            HomeContentSearchBottomSheetViewAction.RequestToEnable ->
                mutableState.value = ContentSearchBottomSheetState.Show

            HomeContentSearchBottomSheetViewAction.Dismiss ->
                mutableState.value = ContentSearchBottomSheetState.Hide

            HomeContentSearchBottomSheetViewAction.EnableForPrimaryUser -> enableContentSearchForPrimaryUser()
            is HomeContentSearchBottomSheetViewAction.ToggleMobileData -> toggleMobileData(action.enabled)
        }
    }

    private fun enableContentSearchForPrimaryUser() {
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            enableContentSearch(userId)
                .onRight { startContentIndexingForUser(userId) }
                .onLeft { Timber.e("content-search: could not enable from the spotlight sheet for $userId") }
        }
    }

    private fun toggleMobileData(enabled: Boolean) {
        mutableIsMobileDataEnabled.value = enabled
        viewModelScope.launch { setAllowContentSearchOnMobileData(enabled) }
    }

    private companion object {

        const val DefaultAllowMobileData = true
    }
}
