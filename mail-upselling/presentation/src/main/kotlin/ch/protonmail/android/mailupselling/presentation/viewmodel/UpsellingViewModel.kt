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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.usecase.ObserveMailPlusPlanUpgrades
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.presentation.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.Loading
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingEntryPointKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
internal class UpsellingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMailPlusPlanUpgrades: ObserveMailPlusPlanUpgrades,
    private val upsellingContentReducer: UpsellingContentReducer,
    private val forceEventLoopRepository: EventLoopRepository,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val resetPlanUpgradesCache: ResetPlanUpgradesCache,
    private val appEventBroadcaster: AppEventBroadcaster
) : ViewModel() {

    private val mutableState = MutableStateFlow<UpsellingScreenContentState>(Loading)
    val state = mutableState.asStateFlow()

    init {
        val entryPoint = savedStateHandle
            .get<String>(UpsellingEntryPointKey)
            ?.deserialize<UpsellingEntryPoint.Feature>()
            ?: UpsellingEntryPoint.Feature.Navbar

        viewModelScope.launch {
            val plans = try {
                withTimeoutOrNull(10.seconds) {
                    observeMailPlusPlanUpgrades(entryPoint).first { it.isNotEmpty() }
                }
            } catch (_: NoSuchElementException) {
                null // Flow completed without non-empty list (e.g., no userId)
            }

            if (plans == null) {
                emitNewStateFrom(UpsellingScreenContentEvent.LoadingError.NoSubscriptions)
                return@launch
            }

            emitNewStateFrom(UpsellingScreenContentEvent.DataLoaded(plans, entryPoint))
            appEventBroadcaster.emit(AppEvent.SubscriptionPaywallShown)

            val currentState = mutableState.value
            if (currentState is UpsellingScreenContentState.Data) {
                currentState.plans.variant.toOfferId()?.let { offerId ->
                    appEventBroadcaster.emit(AppEvent.OfferReceived(offerId))
                }
            }
        }
    }

    fun onPurchaseClicked() {
        val currentState = mutableState.value
        if (currentState is UpsellingScreenContentState.Data) {
            currentState.plans.variant.toOfferId()?.let { offerId ->
                viewModelScope.launch {
                    appEventBroadcaster.emit(AppEvent.OfferClicked(offerId))
                }
            }
        }
    }

    fun overrideUpsellingVisibility() = viewModelScope.launch {
        Timber.d("user-subscription: forcing event loop")
        val userId = observePrimaryUserId().first() ?: return@launch
        Timber.d("user-subscription: triggered event loop")

        // Force trigger the event loop
        forceEventLoopRepository.trigger(userId)

        // Invalidate the upgrades list cache (for all user ids)
        resetPlanUpgradesCache()
    }

    private suspend fun emitNewStateFrom(operation: UpsellingScreenContentOperation) {
        mutableState.update { upsellingContentReducer.newStateFrom(operation) }
    }
}

private fun PlanUpgradeVariant.toOfferId(): String? = when (this) {
    is PlanUpgradeVariant.IntroductoryPrice -> "intro_price"
    is PlanUpgradeVariant.BlackFriday.Wave1 -> "black_friday_wave1"
    is PlanUpgradeVariant.BlackFriday.Wave2 -> "black_friday_wave2"
    is PlanUpgradeVariant.SpringPromo.Wave1 -> "spring26_wave1"
    is PlanUpgradeVariant.SpringPromo.Wave2 -> "spring26_wave2"
    is PlanUpgradeVariant.Normal,
    is PlanUpgradeVariant.SocialProof -> null
}
