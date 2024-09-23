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
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType.Upgrade
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.domain.usecase.FilterDynamicPlansByUserSubscription
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.Loading
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingBottomSheetContentState.UpsellingBottomSheetContentOperation.UpsellingBottomSheetContentEvent
import ch.protonmail.android.mailupselling.presentation.reducer.UpsellingBottomSheetContentReducer
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.usecase.UpdateUpsellingOneClickLastTimestamp
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import me.proton.core.util.kotlin.takeIfNotEmpty

@HiltViewModel(assistedFactory = UpsellingBottomSheetViewModel.Factory::class)
internal class UpsellingBottomSheetViewModel @AssistedInject constructor(
    @Assisted val upsellingEntryPoint: UpsellingEntryPoint.BottomSheet,
    observePrimaryUser: ObservePrimaryUser,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val filterDynamicPlansByUserSubscription: FilterDynamicPlansByUserSubscription,
    private val updateUpsellingOneClickLastTimestamp: UpdateUpsellingOneClickLastTimestamp,
    private val upsellingTelemetryRepository: UpsellingTelemetryRepository,
    private val upsellingBottomSheetContentReducer: UpsellingBottomSheetContentReducer
) : ViewModel() {

    @AssistedFactory
    interface Factory {

        fun create(upsellingEntryPoint: UpsellingEntryPoint.BottomSheet): UpsellingBottomSheetViewModel
    }

    private val mutableState = MutableStateFlow<UpsellingBottomSheetContentState>(Loading)
    val state = mutableState.asStateFlow()

    init {
        observePrimaryUser().mapLatest { user ->
            val userId = user?.userId
                ?: return@mapLatest emitNewStateFrom(UpsellingBottomSheetContentEvent.LoadingError.NoUserId)

            val dynamicPlans = runCatching { getDynamicPlansAdjustedPrices(userId) }.getOrElse {
                return@mapLatest emitNewStateFrom(UpsellingBottomSheetContentEvent.LoadingError.NoSubscriptions)
            }

            val dynamicPlan = filterDynamicPlansByUserSubscription(userId, dynamicPlans).takeIfNotEmpty()?.first()
                ?: return@mapLatest emitNewStateFrom(UpsellingBottomSheetContentEvent.LoadingError.NoSubscriptions)

            emitNewStateFrom(UpsellingBottomSheetContentEvent.DataLoaded(userId, dynamicPlan, upsellingEntryPoint))
        }.launchIn(viewModelScope)
    }

    suspend fun updateLastSeenTimestamp() {
        updateUpsellingOneClickLastTimestamp(Instant.now().toEpochMilli())
    }

    fun trackUpgradeAttempt(payload: UpsellingTelemetryTargetPlanPayload) {
        upsellingTelemetryRepository.trackEvent(Upgrade.UpgradeAttempt(payload), upsellingEntryPoint)
    }

    fun trackUpgradeCancelled(payload: UpsellingTelemetryTargetPlanPayload) {
        upsellingTelemetryRepository.trackEvent(Upgrade.UpgradeCancelled(payload), upsellingEntryPoint)
    }

    fun trackUpgradeErrored(payload: UpsellingTelemetryTargetPlanPayload) {
        upsellingTelemetryRepository.trackEvent(Upgrade.UpgradeErrored(payload), upsellingEntryPoint)
    }

    fun trackPurchaseCompleted(payload: UpsellingTelemetryTargetPlanPayload) {
        upsellingTelemetryRepository.trackEvent(Upgrade.PurchaseCompleted(payload), upsellingEntryPoint)
    }

    private fun emitNewStateFrom(operation: UpsellingBottomSheetContentOperation) {
        mutableState.update { upsellingBottomSheetContentReducer.newStateFrom(operation) }
    }
}
