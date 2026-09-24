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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailfeatureflags.domain.model.PlusToUnlimitedUpsellExperiment
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellExperimentFlag
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlusToUnlimitedEligibility
import ch.protonmail.android.mailupselling.domain.usecase.ResolvePlanBeforeUpgrade
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveActiveUpsellTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlusToUnlimitedUpsellViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    observePlusToUnlimitedEligibility: ObservePlusToUnlimitedEligibility,
    resolveActiveUpsellTheme: ResolveActiveUpsellTheme,
    private val resolvePlanBeforeUpgrade: ResolvePlanBeforeUpgrade,
    private val recordUpsellButtonTapped: RecordUpsellButtonTapped
) : ViewModel() {

    val theme: StateFlow<UpsellContentTheme?> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            observePlusToUnlimitedEligibility(userId)
                .scan<Either<DataError, Boolean>, Boolean?>(null) { lastKnown, verdict ->
                    verdict.getOrNull() ?: lastKnown
                }
                .map { eligible ->
                    if (eligible == true) resolveActiveUpsellTheme(userId) else null
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = null
        )

    fun recordUpsellButtonTapped() = viewModelScope.launch {
        recordUpsellButtonTapped(
            userId = observePrimaryUserId().filterNotNull().first(),
            generalDimensions = GeneralDimensions(
                upsellEntryPoint = UpsellEntryPoint.NAVBAR_UPSELL,
                planBeforeUpgrade = resolvePlanBeforeUpgrade(),
                modalVariant = UpsellModalVariant.COMPARISON_UNLIMITED,
                upsellExperimentFlag = UpsellExperimentFlag(flagName = PlusToUnlimitedUpsellExperiment.key),
                promoCampaign = theme.value?.telemetryId
            )
        )
    }
}
