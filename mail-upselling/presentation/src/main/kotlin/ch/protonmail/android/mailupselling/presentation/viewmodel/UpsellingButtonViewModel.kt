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
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailfeatureflags.domain.model.UnlimitedPlanPlacementExperimentEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.UnlimitedPlanPlacementRegions
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = UpsellingButtonViewModel.Factory::class)
class UpsellingButtonViewModel @AssistedInject constructor(
    @Assisted val upsellingEntryPoint: UpsellingEntryPoint.Feature,
    private val recordUpsellButtonTapped: RecordUpsellButtonTapped,
    private val observePrimaryUserId: ObservePrimaryUserId,
    observeUpsellingVisibility: ObserveUpsellingVisibility
) : ViewModel() {

    val state: StateFlow<UpsellingState> = observeUpsellingVisibility(upsellingEntryPoint)
        .map { visibility -> UpsellingState(visibility) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = initialState
        )

    fun recordUpsellButtonTapped(upsellEntryPoint: UpsellEntryPoint, modalVariant: UpsellModalVariant) =
        viewModelScope.launch {
            recordUpsellButtonTapped(
                userId = observePrimaryUserId().filterNotNull().first(),
                generalDimensions = GeneralDimensions(
                    upsellEntryPoint = upsellEntryPoint,
                    planBeforeUpgrade = FREE_PLAN,
                    modalVariant = modalVariant,
                    upsellFeatureFlags = UpsellFeatureFlags(
                        parentFlagName = UnlimitedPlanPlacementRegions.key,
                        childFlagName = UnlimitedPlanPlacementExperimentEnabled.key
                    )
                )
            )
        }

    @AssistedFactory
    interface Factory {

        fun create(upsellingEntryPoint: UpsellingEntryPoint.Feature): UpsellingButtonViewModel
    }

    companion object {

        val initialState = UpsellingState(visibility = UpsellingVisibility.Hidden)
    }
}

private const val FREE_PLAN = "Free plan"
