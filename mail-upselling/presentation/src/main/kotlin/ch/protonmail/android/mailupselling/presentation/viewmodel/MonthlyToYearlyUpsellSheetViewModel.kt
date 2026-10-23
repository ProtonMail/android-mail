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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailfeatureflags.domain.model.MonthlyToYearlyUpsellExperiment
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PurchaseFlow
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellExperimentFlag
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeAttempt
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeCancelledByUser
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeError
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeSuccess
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.model.MonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.usecase.ObserveMonthlyToYearlyOffer
import ch.protonmail.android.mailupselling.domain.usecase.RecordMonthlyToYearlyOptOut
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.domain.usecase.UpsellVariantPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@HiltViewModel
class MonthlyToYearlyUpsellSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observePrimaryUserId: ObservePrimaryUserId,
    observeMonthlyToYearlyOffer: ObserveMonthlyToYearlyOffer,
    private val recordMonthlyToYearlyOptOut: RecordMonthlyToYearlyOptOut,
    private val eventLoopRepository: EventLoopRepository,
    private val resetPlanUpgradesCache: ResetPlanUpgradesCache,
    private val recordUpsellButtonTapped: RecordUpsellButtonTapped,
    private val recordUpgradeAttempt: RecordUpgradeAttempt,
    private val recordUpgradeCancelledByUser: RecordUpgradeCancelledByUser,
    private val recordUpgradeError: RecordUpgradeError,
    private val recordUpgradeSuccess: RecordUpgradeSuccess,
    @AppScope private val appScope: CoroutineScope
) : ViewModel() {

    private val entryPoint = savedStateHandle.get<String>(EntryPointKey)
        ?.let { name -> UpsellEntryPoint.entries.firstOrNull { it.name == name } }
        ?: UpsellEntryPoint.NAVBAR_UPSELL

    val offer: StateFlow<MonthlyToYearlyOffer?> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId -> observeMonthlyToYearlyOffer(userId).mapNotNull { it.getOrNull() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            val opened = offer.filterNotNull().first()
            val userId = primaryUserId() ?: return@launch
            recordUpsellButtonTapped(userId, opened.generalDimensions(entryPoint))
        }
    }

    fun recordDoNotShowAgain() {
        appScope.launch {
            val userId = primaryUserId() ?: return@launch
            recordMonthlyToYearlyOptOut(userId)
            resetPlanUpgradesCache()
        }
    }

    fun onSwitchedToYearly() {
        appScope.launch {
            val userId = primaryUserId() ?: return@launch
            eventLoopRepository.triggerAndWait(userId)
            resetPlanUpgradesCache()
        }
    }

    fun recordUpgradeAttempt() = recordUpgradeEvent(recordUpgradeAttempt::invoke)

    fun recordUpgradeCancelledByUser() = recordUpgradeEvent(recordUpgradeCancelledByUser::invoke)

    fun recordUpgradeError() = recordUpgradeEvent(recordUpgradeError::invoke)

    fun recordUpgradeSuccess() = recordUpgradeEvent(recordUpgradeSuccess::invoke)

    private fun recordUpgradeEvent(record: suspend (UserId, GeneralDimensions, PlanSpecificDimensions) -> Unit) {
        val offer = offer.value ?: return
        viewModelScope.launch {
            val userId = primaryUserId() ?: return@launch
            record(userId, offer.generalDimensions(entryPoint), offer.planSpecificDimensions())
        }
    }

    private suspend fun primaryUserId(): UserId? = observePrimaryUserId().first()

    companion object {

        const val EntryPointKey = "MonthlyToYearlyUpsellEntryPointKey"
    }
}

private fun MonthlyToYearlyOffer.generalDimensions(entryPoint: UpsellEntryPoint) = GeneralDimensions(
    upsellEntryPoint = entryPoint,
    planBeforeUpgrade = plan.planName,
    modalVariant = when (plan) {
        UpsellVariantPlan.MAIL_PLUS -> UpsellModalVariant.MONTHLY_TO_YEARLY_PLUS
        UpsellVariantPlan.UNLIMITED -> UpsellModalVariant.MONTHLY_TO_YEARLY_UNLIMITED
    },
    upsellExperimentFlag = UpsellExperimentFlag(flagName = MonthlyToYearlyUpsellExperiment.key),
    promoCampaign = null
)

private fun MonthlyToYearlyOffer.planSpecificDimensions() = PlanSpecificDimensions(
    selectedPlan = plan.planName,
    selectedCycle = PlanUpgradeCycle.Yearly.name,
    upsellIsPromotional = false,
    purchaseFlow = PurchaseFlow.SDK
)
