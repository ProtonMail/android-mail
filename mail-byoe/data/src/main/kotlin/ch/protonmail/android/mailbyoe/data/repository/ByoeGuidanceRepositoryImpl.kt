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

package ch.protonmail.android.mailbyoe.data.repository

import arrow.core.Either
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerCta
import ch.protonmail.android.mailbyoe.domain.model.ByoeBannerOrigin
import ch.protonmail.android.mailbyoe.domain.model.ByoeConnectErrorType
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportMode
import ch.protonmail.android.mailbyoe.domain.model.ByoeOnboardingStep
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.domain.model.ByoeUpsellCta
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionSettingsAccess
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ByoeGuidance
import uniffi.mail_uniffi.ByoeGuidanceTreatment
import uniffi.mail_uniffi.ByoeSettingsAccess
import uniffi.mail_uniffi.OnboardingStep
import javax.inject.Inject
import uniffi.mail_uniffi.ByoeBannerCta as SdkBannerCta
import uniffi.mail_uniffi.ByoeBannerOrigin as SdkBannerOrigin
import uniffi.mail_uniffi.ByoeConnectErrorType as SdkConnectErrorType
import uniffi.mail_uniffi.ByoeDismissMethod as SdkDismissMethod
import uniffi.mail_uniffi.ByoeImportMode as SdkImportMode
import uniffi.mail_uniffi.ByoeSurface as SdkSurface
import uniffi.mail_uniffi.ByoeUpsellCta as SdkUpsellCta

class ByoeGuidanceRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession
) : ByoeGuidanceRepository {

    override suspend fun getSettingsAccess(userId: UserId): Either<DataError, GmailConnectionSettingsAccess> =
        withByoeGuidance(userId) { guidance ->
            guidance.settings().also { Timber.d("byoe: Rust guidance settings = $it") }.toSettingsAccess()
        }

    override suspend fun getOnboardingTreatment(userId: UserId): Either<DataError, ByoeTreatment?> =
        withByoeGuidance(userId) { guidance ->
            guidance.onboarding().also { Timber.d("byoe: Rust guidance onboarding = $it") }?.toTreatment()
        }

    override suspend fun getSidebarBannerTreatment(userId: UserId): Either<DataError, ByoeTreatment?> =
        withByoeGuidance(userId) { guidance ->
            guidance.sidebarBanner().also { Timber.d("byoe: Rust guidance sidebar banner = $it") }?.toTreatment()
        }

    override suspend fun markSidebarBannerClosed(userId: UserId, method: ByoeDismissMethod): Either<DataError, Unit> =
        withByoeGuidance(userId) { guidance -> guidance.markSidebarBannerClosed(method.toSdk()) }

    override suspend fun markGuidanceDismissed(
        userId: UserId,
        surface: ByoeSurface,
        method: ByoeDismissMethod
    ): Either<DataError, Unit> = withByoeGuidance(userId) { guidance ->
        when (surface) {
            ByoeSurface.OnboardingScreen -> guidance.markOnboardingDismissed(method.toSdk())
            ByoeSurface.Spotlight -> guidance.markSpotlightDismissed(method.toSdk())
            ByoeSurface.OnboardingDrawer,
            ByoeSurface.SidebarBanner,
            ByoeSurface.Settings,
            ByoeSurface.DeepLink -> Timber.w("byoe: no guidance to dismiss on $surface")
        }
    }

    override suspend fun isOnboardingFinishedRecorded(userId: UserId): Either<DataError, Boolean> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.isOnboardingStepShown(OnboardingStep.FINISHED)
            }
        }

    override suspend fun recordOnboardingFinished(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.markOnboardingStep(OnboardingStep.FINISHED)
            }
        }

    override suspend fun recordOnboardingStep(userId: UserId, step: ByoeOnboardingStep): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                store.markOnboardingStep(step.toSdk())
            }
        }

    override suspend fun debugResetOnboardingSteps(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store ->
                OnboardingStepKeys.forEach { key -> store.setValue(key, false) }
            }
        }

    override suspend fun debugClearScreenVisibility(userId: UserId): Either<DataError, Unit> =
        executeWithUserSession(userId) { session ->
            session.getRustUserSession().screenVisibilityStore().use { store -> store.clear() }
        }

    override suspend fun report(userId: UserId, event: ByoeTelemetryEvent): Either<DataError, Unit> =
        withByoeGuidance(userId) { guidance -> guidance.report(event) }

    private suspend fun <T> withByoeGuidance(
        userId: UserId,
        action: suspend (ByoeGuidance) -> T
    ): Either<DataError, T> = executeWithUserSession(userId) { session ->
        session.getRustUserSession().byoeGuidance().use { action(it) }
    }
}

// The names the SDK records its onboarding steps under. It has no call to forget a step, only to forget a name.
private val OnboardingStepKeys = listOf(
    "onboarding_page_upsell",
    "onboarding_page_welcome",
    "onboarding_page_offline",
    "onboarding_page_byoe",
    "onboarding_page_notifications",
    "onboarding_shown"
)

private suspend fun ByoeGuidance.report(event: ByoeTelemetryEvent) = when (event) {
    ByoeTelemetryEvent.SidebarBannerShown -> reportSidebarBannerShown()
    ByoeTelemetryEvent.OnboardingDrawerShown -> reportOnboardingDrawerShown()
    is ByoeTelemetryEvent.OnboardingDrawerDismissed -> reportOnboardingDrawerDismissed(event.method.toSdk())
    ByoeTelemetryEvent.SettingsShown -> reportSettingsShown()
    ByoeTelemetryEvent.SettingsView -> reportSettingsView()
    is ByoeTelemetryEvent.ConnectStarted -> reportConnectStarted(event.surface.toSdk(), event.importMode.toSdk())
    is ByoeTelemetryEvent.ConnectSucceeded -> reportConnectSucceeded(event.surface.toSdk())
    is ByoeTelemetryEvent.ConnectAbandoned -> reportConnectAbandoned(event.surface.toSdk())
    is ByoeTelemetryEvent.ConnectFailed -> reportConnectFailed(event.surface.toSdk(), event.errorType.toSdk())
    is ByoeTelemetryEvent.ExploreClicked -> reportExploreClicked(event.surface.toSdk())
    ByoeTelemetryEvent.InboxViewed -> reportInboxViewed()
    ByoeTelemetryEvent.UpsellWallShown -> reportUpsellWallShown()
    is ByoeTelemetryEvent.UpgradeBannerShown -> reportUpgradeBannerShown(event.origin.toSdk())
    is ByoeTelemetryEvent.UpgradeBannerCtaClicked ->
        reportUpgradeBannerCtaClicked(event.origin.toSdk(), event.cta.toSdk())
    ByoeTelemetryEvent.UpsellScreenShown -> reportUpsellScreenShown()
    is ByoeTelemetryEvent.UpsellCtaClicked -> reportUpsellCtaClicked(event.cta.toSdk())
    ByoeTelemetryEvent.UpsellSucceeded -> reportUpsellSucceeded()
    is ByoeTelemetryEvent.UpsellFailed -> reportUpsellFailed(event.errorReason)
}

private fun ByoeDismissMethod.toSdk() = when (this) {
    ByoeDismissMethod.Button -> SdkDismissMethod.BUTTON
    ByoeDismissMethod.SwipeDown -> SdkDismissMethod.SWIPE_DOWN
    ByoeDismissMethod.TapOutside -> SdkDismissMethod.TAP_OUTSIDE
}

private fun ByoeSurface.toSdk() = when (this) {
    ByoeSurface.OnboardingDrawer -> SdkSurface.ONBOARDING_DRAWER
    ByoeSurface.OnboardingScreen -> SdkSurface.ONBOARDING_SCREEN
    ByoeSurface.Spotlight -> SdkSurface.SPOTLIGHT
    ByoeSurface.SidebarBanner -> SdkSurface.SIDEBAR_BANNER
    ByoeSurface.Settings -> SdkSurface.SETTINGS
    ByoeSurface.DeepLink -> SdkSurface.DEEP_LINK
}

private fun ByoeImportMode.toSdk() = when (this) {
    ByoeImportMode.Enabled -> SdkImportMode.ENABLED
    ByoeImportMode.Disabled -> SdkImportMode.DISABLED
    ByoeImportMode.Unknown -> SdkImportMode.UNKNOWN
}

private fun ByoeConnectErrorType.toSdk() = when (this) {
    ByoeConnectErrorType.NoAccess -> SdkConnectErrorType.NO_ACCESS
    ByoeConnectErrorType.AlreadyAdded -> SdkConnectErrorType.ALREADY_ADDED
    ByoeConnectErrorType.CreateFailed -> SdkConnectErrorType.CREATE_FAILED
    ByoeConnectErrorType.ConvertFailed -> SdkConnectErrorType.CONVERT_FAILED
    ByoeConnectErrorType.WrongAccount -> SdkConnectErrorType.WRONG_ACCOUNT
    ByoeConnectErrorType.TokenFailed -> SdkConnectErrorType.TOKEN_FAILED
    ByoeConnectErrorType.Unknown -> SdkConnectErrorType.UNKNOWN
}

private fun ByoeBannerOrigin.toSdk() = when (this) {
    ByoeBannerOrigin.Settings -> SdkBannerOrigin.SETTINGS
    ByoeBannerOrigin.InboxStorageLimit -> SdkBannerOrigin.INBOX_STORAGE_LIMIT
}

private fun ByoeBannerCta.toSdk() = when (this) {
    ByoeBannerCta.Upgrade -> SdkBannerCta.UPGRADE
    ByoeBannerCta.NotNow -> SdkBannerCta.NOT_NOW
}

private fun ByoeUpsellCta.toSdk() = when (this) {
    ByoeUpsellCta.Primary -> SdkUpsellCta.PRIMARY
    ByoeUpsellCta.Secondary -> SdkUpsellCta.SECONDARY
}

private fun ByoeOnboardingStep.toSdk() = when (this) {
    ByoeOnboardingStep.Upsell -> OnboardingStep.UPSELL
    ByoeOnboardingStep.Welcome -> OnboardingStep.WELCOME
    ByoeOnboardingStep.Offline -> OnboardingStep.OFFLINE
    ByoeOnboardingStep.Byoe -> OnboardingStep.BYOE
    ByoeOnboardingStep.Notifications -> OnboardingStep.NOTIFICATIONS
}

private fun ByoeSettingsAccess.toSettingsAccess() = when (this) {
    ByoeSettingsAccess.HIDDEN -> GmailConnectionSettingsAccess.Hidden
    ByoeSettingsAccess.LIST_ONLY -> GmailConnectionSettingsAccess.ListOnly
    ByoeSettingsAccess.FULL -> GmailConnectionSettingsAccess.Full
}

private fun ByoeGuidanceTreatment.toTreatment() = when (this) {
    ByoeGuidanceTreatment.TREATMENT1 -> ByoeTreatment.Treatment1
    ByoeGuidanceTreatment.TREATMENT2 -> ByoeTreatment.Treatment2
}
