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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.DismissContentSearchPrompt
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentSearchPromptDismissed
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Whether, and how, the search screen may offer to set content search up for the primary account.
 *
 * Content search already on, unavailable on this build, or unavailable on a 32-bit device all read
 * as nothing to offer.
 *
 * The empty results always carry the offer. The banner only carries it until the account answers,
 * here or from the feature spotlight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContentSearchSetUpPromptViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val observeContentSearchEnabled: ObserveContentSearchEnabled,
    private val observeContentSearchPromptDismissed: ObserveContentSearchPromptDismissed,
    private val dismissContentSearchPrompt: DismissContentSearchPrompt,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
        .filterNotNull()
        .distinctUntilChanged()

    private val isSetUpBannerSetAside = MutableStateFlow(false)

    val canSetUpContentSearch: StateFlow<Boolean> = primaryUserId
        .flatMapLatest { userId -> observeContentSearchEnabled(userId) }
        .distinctUntilChanged()
        .map { isEnabled -> !isEnabled && isContentSearchFeatureEnabled() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), false)

    /**
     * Whether to show the banner, or null until the account's answer has been read.
     *
     * The screen waits for that answer before it focuses the search field. Reading null as "no
     * banner" would raise the keyboard, and then the banner would come up behind it.
     */
    val showSetUpBanner: StateFlow<Boolean?> = primaryUserId
        .flatMapLatest { userId ->
            combine(
                observeContentSearchEnabled(userId),
                observeContentSearchPromptDismissed(userId, ContentSearchPrompt.SetUpOffer),
                isSetUpBannerSetAside
            ) { isEnabled, isDismissed, isSetAside ->
                !isEnabled && !isDismissed && !isSetAside && isContentSearchFeatureEnabled()
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), null)

    /**
     * Records that the primary account has answered the offer, so the banner stops asking.
     *
     * The banner's close button answers it. So does searching.
     * Its set-up button does not. See [setAsideSetUpBanner].
     */
    fun dismissSetUpBanner() {
        viewModelScope.launch {
            dismissContentSearchPrompt(primaryUserId.first(), ContentSearchPrompt.SetUpOffer)
        }
    }

    /**
     * Hides the banner while the user goes through set-up, without recording an answer.
     *
     * The answer is only recorded when the user confirms on the content search sheet. If set-up is
     * cut short, for example by a notification or the app being killed, the banner comes back.
     */
    fun setAsideSetUpBanner() {
        isSetUpBannerSetAside.value = true
    }

    /**
     * Records the answer when the user declines set-up.
     *
     * This applies however the user reached set-up, from the banner or from the empty results.
     */
    fun declineSetUp() = dismissSetUpBanner()
}

private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
