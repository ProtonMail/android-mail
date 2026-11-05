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
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.ByoeUpsellCta
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsSdkUpgradesPurchaseEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailfeatureflags.domain.model.PlusToUnlimitedUpsellExperiment
import ch.protonmail.android.mailfeatureflags.domain.model.UpsellPlanExperiment
import ch.protonmail.android.mailsession.domain.model.gainedSubscriptionSince
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsession.domain.usecase.ObserveUser
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PurchaseFlow
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellExperimentFlag
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeAttempt
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeCancelledByUser
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeError
import ch.protonmail.android.mailtelemetry.domain.usecase.RecordUpgradeSuccess
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.repository.UpsellRatingTriggerRepository
import ch.protonmail.android.mailupselling.domain.usecase.ObservePlanUpgrades
import ch.protonmail.android.mailupselling.domain.usecase.RecordBlackFridayOptOut
import ch.protonmail.android.mailupselling.domain.usecase.RecordPlusToUnlimitedOptOut
import ch.protonmail.android.mailupselling.domain.usecase.ResetPlanUpgradesCache
import ch.protonmail.android.mailupselling.domain.usecase.ResolvePlanBeforeUpgrade
import ch.protonmail.android.mailupselling.presentation.UpsellingContentReducer
import ch.protonmail.android.mailupselling.presentation.extension.toOfferId
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentOperation.UpsellingScreenContentEvent
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState.Loading
import ch.protonmail.android.mailupselling.presentation.model.UpsellingTelemetryPayload
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceListUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeInstanceUiModel
import ch.protonmail.android.mailupselling.presentation.model.planupgrades.PlanUpgradeVariant
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen.UpsellingEntryPointKey
import ch.protonmail.android.mailupselling.presentation.usecase.ResolveActiveUpsellTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
@HiltViewModel
internal class UpsellingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observePlanUpgrades: ObservePlanUpgrades,
    private val upsellingContentReducer: UpsellingContentReducer,
    private val forceEventLoopRepository: EventLoopRepository,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeUser: ObserveUser,
    @AppScope private val appScope: CoroutineScope,
    private val resetPlanUpgradesCache: ResetPlanUpgradesCache,
    private val appEventBroadcaster: AppEventBroadcaster,
    private val recordUpgradeAttempt: RecordUpgradeAttempt,
    private val recordUpgradeCancelledByUser: RecordUpgradeCancelledByUser,
    private val recordUpgradeError: RecordUpgradeError,
    private val recordUpgradeSuccess: RecordUpgradeSuccess,
    private val upsellRatingTriggerRepository: UpsellRatingTriggerRepository,
    private val resolveActiveUpsellTheme: ResolveActiveUpsellTheme,
    private val recordPlusToUnlimitedOptOut: RecordPlusToUnlimitedOptOut,
    private val recordBlackFridayOptOut: RecordBlackFridayOptOut,
    private val resolvePlanBeforeUpgrade: ResolvePlanBeforeUpgrade,
    @IsSdkUpgradesPurchaseEnabled private val sdkPurchaseEnabled: FeatureFlag<Boolean>,
    private val reportByoeTelemetryEvent: ReportByoeTelemetryEvent
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow<UpsellingScreenContentState>(Loading)
    val state = mutableState.asStateFlow()

    private var activeTheme: UpsellContentTheme? = null
    private var purchaseStarted = false

    val closeAfterUpgradedElsewhere: StateFlow<Effect<Unit>> = primaryUserId
        .flatMapLatest { userId -> observeUser(userId) }
        .mapNotNull { it.getOrNull() }
        .distinctUntilChanged { old, new -> !new.gainedSubscriptionSince(old) }
        .drop(1)
        .filter { !purchaseStarted }
        .map { Effect.of(Unit) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Effect.empty())

    private val planBeforeUpgrade = viewModelScope.async { resolvePlanBeforeUpgrade() }

    private val currentOfferId: String?
        get() = (mutableState.value as? UpsellingScreenContentState.Data)?.plans?.variant?.toOfferId()

    private val experimentFlagKey
        get() = if (entryPoint == UpsellingEntryPoint.Feature.PlusUnlimited) {
            PlusToUnlimitedUpsellExperiment.key
        } else {
            UpsellPlanExperiment.key
        }

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

            val theme = if (entryPoint == UpsellingEntryPoint.Feature.PlusUnlimited) {
                resolveActiveUpsellTheme(primaryUserId.first())
            } else {
                null
            }
            activeTheme = theme

            emitNewStateFrom(UpsellingScreenContentEvent.DataLoaded(plans, entryPoint, theme))
            // Once per screen entry: the plans load once, here.
            reportByoeShown()
            appEventBroadcaster.emit(AppEvent.SubscriptionPaywallShown)

            currentOfferId?.let { appEventBroadcaster.emit(AppEvent.OfferReceived(it)) }
        }
    }

    fun onPurchaseClicked() {
        purchaseStarted = true
        val offerId = currentOfferId ?: return
        viewModelScope.launch { appEventBroadcaster.emit(AppEvent.OfferClicked(offerId)) }
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

    fun optOutOfBlackFridayAfterPurchase() = appScope.launch {
        val variant = (mutableState.value as? UpsellingScreenContentState.Data)?.plans?.variant
        if (variant !is PlanUpgradeVariant.BlackFriday) return@launch
        recordBlackFridayOptOut(primaryUserId.first())
    }

    fun recordDoNotShowAgain() = appScope.launch {
        val userId = observePrimaryUserId().first() ?: return@launch
        val variant = (mutableState.value as? UpsellingScreenContentState.Data)?.plans?.variant
        if (variant is PlanUpgradeVariant.BlackFriday) {
            recordBlackFridayOptOut(userId)
        } else {
            recordPlusToUnlimitedOptOut(userId)
        }
    }

    fun recordUpgradeAttempt(upsellingTelemetryPayload: UpsellingTelemetryPayload) {
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeAttempt::invoke)
        reportByoeStorageUpsell { ByoeTelemetryEvent.UpsellCtaClicked(upsellCta(upsellingTelemetryPayload)) }
    }

    // A cancelled purchase is not a failure, so it reports no BYOE telemetry.
    fun recordUpgradeCancelledByUser(upsellingTelemetryPayload: UpsellingTelemetryPayload) {
        purchaseStarted = false
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeCancelledByUser::invoke)
    }

    fun recordUpgradeError(upsellingTelemetryPayload: UpsellingTelemetryPayload) {
        purchaseStarted = false
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeError::invoke)
        reportByoeStorageUpsell { ByoeTelemetryEvent.UpsellFailed(BYOE_UPGRADE_ERROR_REASON) }
    }

    fun recordUpgradeSuccess(upsellingTelemetryPayload: UpsellingTelemetryPayload) {
        recordUpgradeEvent(upsellingTelemetryPayload, recordUpgradeSuccess::invoke)
        reportByoeStorageUpsell { ByoeTelemetryEvent.UpsellSucceeded }
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
                planBeforeUpgrade = planBeforeUpgrade.await(),
                modalVariant = upsellingTelemetryPayload.modalVariant,
                upsellExperimentFlag = UpsellExperimentFlag(
                    flagName = experimentFlagKey
                ),
                promoCampaign = currentOfferId ?: activeTheme?.telemetryId
            ),
            PlanSpecificDimensions(
                selectedPlan = upsellingTelemetryPayload.selectedPlan,
                selectedCycle = upsellingTelemetryPayload.selectedCycle,
                upsellIsPromotional = upsellingTelemetryPayload.upsellIsPromotional,
                purchaseFlow = when (sdkPurchaseEnabled.get()) {
                    true -> PurchaseFlow.SDK
                    false -> PurchaseFlow.LEGACY
                }
            )
        )
    }

    private fun reportByoeShown() {
        when (entryPoint) {
            UpsellingEntryPoint.Feature.ByoeConnectionLimit ->
                reportByoeTelemetryEvent(ByoeTelemetryEvent.UpsellWallShown)

            UpsellingEntryPoint.Feature.ByoeStorage -> reportByoeTelemetryEvent(ByoeTelemetryEvent.UpsellScreenShown)
            else -> Unit
        }
    }

    /** The CTA, success and failure events belong to the storage upsell funnel only, not to the connection limit. */
    private fun reportByoeStorageUpsell(event: () -> ByoeTelemetryEvent) {
        if (entryPoint == UpsellingEntryPoint.Feature.ByoeStorage) reportByoeTelemetryEvent(event())
    }

    /**
     * The primary CTA buys the default plan: the one a single-button layout shows, else the longer cycle. The
     * secondary CTA buys the other plan, which a two-plan layout lets the user pick.
     */
    private fun upsellCta(payload: UpsellingTelemetryPayload): ByoeUpsellCta {
        val plans = (mutableState.value as? UpsellingScreenContentState.Data)?.plans?.list
            as? PlanUpgradeInstanceListUiModel.Data
        if (plans == null) {
            Timber.w("byoe: no plans to tell the upsell CTA from, reporting it as primary")
            return ByoeUpsellCta.Primary
        }
        val isDefaultPlan = payload.selectedCycle == plans.defaultCycle().name
        return if (isDefaultPlan) ByoeUpsellCta.Primary else ByoeUpsellCta.Secondary
    }

    // Mirrors the footer's layout choice (UpsellingPlanButtonsFooter): these layouts show only the shorter cycle.
    private fun PlanUpgradeInstanceListUiModel.Data.defaultCycle(): PlanUpgradeCycle {
        val showsOnlyShorterCycle = when (this) {
            is PlanUpgradeInstanceListUiModel.Data.IntroPrice ->
                shorterCycle is PlanUpgradeInstanceUiModel.Promotional
            is PlanUpgradeInstanceListUiModel.Data.BlackFriday ->
                shorterCycle is PlanUpgradeInstanceUiModel.Promotional.BlackFriday &&
                    variant == PlanUpgradeVariant.BlackFriday.Wave2
            else -> false
        }
        return if (showsOnlyShorterCycle) shorterCycle.cycle else longerCycle.cycle
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
            UpsellingEntryPoint.Feature.PlusUnlimited -> UpsellEntryPoint.NAVBAR_UPSELL
            UpsellingEntryPoint.Feature.Sidebar -> UpsellEntryPoint.NAVBAR_UPSELL
            UpsellingEntryPoint.Feature.Snooze -> UpsellEntryPoint.SNOOZE
            // TODO: Switch to the BYOE storage entry point once the SDK exposes one.
            UpsellingEntryPoint.Feature.ByoeStorage -> UpsellEntryPoint.MAILBOX_TOP_BAR
            // TODO: Switch to the BYOE connection limit entry point once the SDK exposes one.
            UpsellingEntryPoint.Feature.ByoeConnectionLimit -> UpsellEntryPoint.MAILBOX_TOP_BAR
        }
    }

    private suspend fun emitNewStateFrom(operation: UpsellingScreenContentOperation) {
        mutableState.update { upsellingContentReducer.newStateFrom(operation) }
    }

    private companion object {

        /** A stable code, as the SDK asks: the payment flow only gives a message. */
        const val BYOE_UPGRADE_ERROR_REASON = "upgrade_error"
    }
}
