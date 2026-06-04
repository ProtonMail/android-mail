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
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.model.UnlimitedPlanPlacementExperimentEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.UnlimitedPlanPlacementRegions
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeAttempt
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeCancelledByUser
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeError
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeSuccess
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.repository.UpsellRatingTriggerRepository
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlanUpgrades
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.presentation.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.Loading
import ch.protonmail.android.mailupselling.presentation.model.UpsellingTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingEntryPointKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
internal class UpsellingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observePlanUpgrades: ObservePlanUpgrades,
    private val upsellingContentReducer: UpsellingContentReducer,
    private val forceEventLoopRepository: EventLoopRepository,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val resetPlanUpgradesCache: ResetPlanUpgradesCache,
    private val appEventBroadcaster: AppEventBroadcaster,
    private val recordUpgradeAttempt: RecordUpgradeAttempt,
    private val recordUpgradeCancelledByUser: RecordUpgradeCancelledByUser,
    private val recordUpgradeError: RecordUpgradeError,
    private val recordUpgradeSuccess: RecordUpgradeSuccess,
    private val upsellRatingTriggerRepository: UpsellRatingTriggerRepository
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow<UpsellingScreenContentState>(Loading)
    val state = mutableState.asStateFlow()

    val entryPoint = savedStateHandle
        .get<String>(UpsellingEntryPointKey)
        ?.deserialize<UpsellingEntryPoint.Feature>()
        ?: UpsellingEntryPoint.Feature.Navbar

    init {

        viewModelScope.launch {
            val plans = try {
                withTimeoutOrNull(10.seconds) {
                    observePlanUpgrades(entryPoint).first { it.isNotEmpty() }
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

    fun recordUpgradeAttempt(upsellingTelemetryPayload: UpsellingTelemetryPayload) =
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeAttempt::invoke)

    fun recordUpgradeCancelledByUser(upsellingTelemetryPayload: UpsellingTelemetryPayload) =
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeCancelledByUser::invoke)

    fun recordUpgradeError(upsellingTelemetryPayload: UpsellingTelemetryPayload) =
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeError::invoke)

    fun recordUpgradeSuccess(upsellingTelemetryPayload: UpsellingTelemetryPayload) {
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeSuccess::invoke)
        // Fire-and-forget, scope-independent: the screen-scoped viewModelScope may be cancelled by
        // the navigateBack() that runs on success, so the emit must not rely on it.
        upsellRatingTriggerRepository.emitUpsellSuccess()
    }

    private fun recordUpgradeEvent(
        upsellingTelemetryPayload: UpsellingTelemetryPayload,
        record: suspend (UserId, GeneralDimensions, PlanSpecificDimensions) -> Unit
    ) = viewModelScope.launch {
        record(
            primaryUserId.first(),
            GeneralDimensions(
                upsellEntryPoint = entryPoint.toGeneralDimension(
                    upsellingTelemetryPayload.isIntroOffer,
                    upsellingTelemetryPayload.upsellIsPromotional
                ),
                planBeforeUpgrade = FREE_PLAN,
                modalVariant = upsellingTelemetryPayload.modalVariant,
                upsellFeatureFlags = UpsellFeatureFlags(
                    parentFlagName = UnlimitedPlanPlacementRegions.key,
                    childFlagName = UnlimitedPlanPlacementExperimentEnabled.key
                )
            ),
            PlanSpecificDimensions(
                selectedPlan = upsellingTelemetryPayload.selectedPlan,
                selectedCycle = upsellingTelemetryPayload.selectedCycle,
                upsellIsPromotional = upsellingTelemetryPayload.upsellIsPromotional
            )
        )
    }

    private fun UpsellingEntryPoint.Feature.toGeneralDimension(
        isIntroOffer: Boolean,
        upsellIsPromotional: Boolean
    ): UpsellEntryPoint {
        if (isIntroOffer) {
            return UpsellEntryPoint.DOLLAR_PROMO
        }
        return when (this) {
            UpsellingEntryPoint.Feature.AutoDelete -> UpsellEntryPoint.AUTO_DELETE_MESSAGES
            UpsellingEntryPoint.Feature.ContactGroups -> UpsellEntryPoint.CONTACT_GROUPS
            UpsellingEntryPoint.Feature.Folders -> UpsellEntryPoint.FOLDERS_CREATION
            UpsellingEntryPoint.Feature.Labels -> UpsellEntryPoint.LABELS_CREATION
            UpsellingEntryPoint.Feature.MobileSignature -> UpsellEntryPoint.MOBILE_SIGNATURE_EDIT
            UpsellingEntryPoint.Feature.Navbar -> if (upsellIsPromotional) {
                UpsellEntryPoint.MAILBOX_TOP_BAR_PROMO
            } else {
                UpsellEntryPoint.MAILBOX_TOP_BAR
            }
            UpsellingEntryPoint.Feature.ScheduleSend -> UpsellEntryPoint.SCHEDULE_SEND
            UpsellingEntryPoint.Feature.Sidebar -> UpsellEntryPoint.NAVBAR_UPSELL
            UpsellingEntryPoint.Feature.Snooze -> UpsellEntryPoint.SNOOZE
        }
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
    is PlanUpgradeVariant.SummerCampaign.Wave1 -> "summer26_wave1"
    is PlanUpgradeVariant.SummerCampaign.Wave2 -> "summer26_wave2"
    is PlanUpgradeVariant.Normal,
    is PlanUpgradeVariant.SocialProof -> null
}

private const val FREE_PLAN = "Free plan"
