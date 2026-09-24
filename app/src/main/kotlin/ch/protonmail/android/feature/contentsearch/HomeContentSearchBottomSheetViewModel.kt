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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.EnableContentSearch
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchAllowedOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.SetAllowContentSearchOnMobileData
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexingForUser
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val startContentIndexingForUser: StartContentIndexingForUser,
    private val dismissContentSearchPrompt: DismissContentSearchPrompt
) : ViewModel() {

    private val mutableState = MutableStateFlow<ContentSearchBottomSheetState>(ContentSearchBottomSheetState.Hide)
    val state: StateFlow<ContentSearchBottomSheetState> = mutableState.asStateFlow()

    private val mutableIsMobileDataEnabled = MutableStateFlow(DefaultAllowMobileData)
    val isMobileDataEnabled: StateFlow<Boolean> = mutableIsMobileDataEnabled.asStateFlow()

    // Persisting the mobile-data toggle and enabling content search both run in their own
    // launch, so enabling can otherwise race ahead of a toggle that has not hit disk yet and
    // enqueue indexing with the stale constraint. Track the write so enabling can join it first.
    private var mobileDataWriteJob: Job? = null

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
            mobileDataWriteJob?.join()

            val userId = observePrimaryUserId().filterNotNull().first()
            enableContentSearch(userId)
                .onRight {
                    startContentIndexingForUser(userId)
                    // The search screen's set-up banner only hides itself for now when tapped.
                    // Confirming here is what answers it for good.
                    dismissContentSearchPrompt(userId, ContentSearchPrompt.SetUpOffer)
                }
                .onLeft { Timber.e("content-search: could not enable from the spotlight sheet for $userId") }
        }
    }

    private fun toggleMobileData(enabled: Boolean) {
        mutableIsMobileDataEnabled.value = enabled
        mobileDataWriteJob = viewModelScope.launch { setAllowContentSearchOnMobileData(enabled) }
    }

    private companion object {

        const val DefaultAllowMobileData = true
    }
}
