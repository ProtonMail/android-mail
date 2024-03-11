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

package ch.protonmail.android.mailmailbox.presentation.upselling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UpsellingButtonViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<UpsellingButtonState> = mutableState.asStateFlow()

    init {
        observePrimaryUserId()
            .onEach { userId ->
                getDynamicPlansAdjustedPrices(userId)
                    .let { dynamicPlans ->
                        Timber.d("UpsellingButtonViewModel: Plans count: ${dynamicPlans.plans.size}")
                        Timber.d("UpsellingButtonViewModel: Plans: ${dynamicPlans.plans.map { it.name }}")

                        mutableState.value = state.value.copy(isShown = dynamicPlans.plans.isNotEmpty())
                    }
            }
            .launchIn(viewModelScope)
    }

    companion object {

        val initialState = UpsellingButtonState(isShown = false)

    }
}
