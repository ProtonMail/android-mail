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
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerCta
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerOrigin
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.usecase.ObserveByoeImportPausedForStorage
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * The mailbox banner shown while a Gmail import is paused because the user ran out of storage. It goes away by
 * itself once the import resumes after an upgrade, or is cancelled or undone.
 *
 * Its copy and buttons are written for a free user, so it only shows when there is a storage upsell to offer: a
 * paying user, for one, doesn't see it until there is a banner for them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ByoeStorageBannerViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    observeByoeImportPausedForStorage: ObserveByoeImportPausedForStorage,
    private val dismissal: ByoeStorageBannerDismissal,
    private val reportByoeTelemetryEvent: ReportByoeTelemetryEvent,
    observeUpsellingVisibility: ObserveUpsellingVisibility
) : ViewModel() {

    private var isAppearanceReported = false

    val state: StateFlow<ByoeStorageBannerState> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            combine(
                observeByoeImportPausedForStorage(userId),
                dismissal.isDismissed(userId),
                // Deduplicated, so the hidden banner is logged once per change, not on every plans update.
                observeUpsellingVisibility(UpsellingEntryPoint.Feature.ByoeStorage).distinctUntilChanged()
            ) { isPaused, isDismissed, upselling ->
                when {
                    !isPaused || isDismissed -> ByoeStorageBannerState.Hidden
                    upselling == UpsellingVisibility.Hidden -> {
                        Timber.i("byoe: Gmail import paused for storage, but no upsell to offer, so no storage banner")
                        ByoeStorageBannerState.Hidden
                    }
                    else -> ByoeStorageBannerState.Visible
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ByoeStorageBannerState.Hidden)

    fun submit(action: ByoeStorageBannerAction) {
        when (action) {
            is ByoeStorageBannerAction.VisibilityChanged -> onVisibilityChanged(action.isVisible)
            ByoeStorageBannerAction.UpgradeClicked -> reportCta(ByoeBannerCta.Upgrade)
            ByoeStorageBannerAction.NotNowClicked -> {
                reportCta(ByoeBannerCta.NotNow)
                viewModelScope.launch { dismissal.dismiss(observePrimaryUserId().filterNotNull().first()) }
            }
        }
    }

    private fun onVisibilityChanged(isVisible: Boolean) {
        if (!isVisible) {
            isAppearanceReported = false
            return
        }
        if (isAppearanceReported) return
        isAppearanceReported = true
        reportByoeTelemetryEvent(ByoeTelemetryEvent.UpgradeBannerShown(ByoeBannerOrigin.InboxStorageLimit))
    }

    private fun reportCta(cta: ByoeBannerCta) {
        reportByoeTelemetryEvent(ByoeTelemetryEvent.UpgradeBannerCtaClicked(ByoeBannerOrigin.InboxStorageLimit, cta))
    }

    private companion object {

        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
