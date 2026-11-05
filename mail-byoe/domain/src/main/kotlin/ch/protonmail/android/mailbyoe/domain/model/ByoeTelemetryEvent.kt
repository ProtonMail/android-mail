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

package ch.protonmail.android.mailbyoe.domain.model

/**
 * BYOE telemetry the app reports to the SDK, one per SDK `report*` call. Telemetry is best effort: the SDK decides
 * which reports it sends, e.g. only the first sidebar banner appearance on an install.
 */
sealed interface ByoeTelemetryEvent {

    /** Report it each time the banner shows. */
    data object SidebarBannerShown : ByoeTelemetryEvent

    /** No Android call site: there is no BYOE onboarding drawer. */
    data object OnboardingDrawerShown : ByoeTelemetryEvent

    /** No Android call site: there is no BYOE onboarding drawer. */
    data class OnboardingDrawerDismissed(val method: ByoeDismissMethod) : ByoeTelemetryEvent

    /** Report it each time the settings show Connect Gmail. */
    data object SettingsShown : ByoeTelemetryEvent

    /** Report it each time the settings show Connect Gmail; unlike [SettingsShown], every report is sent. */
    data object SettingsView : ByoeTelemetryEvent

    data class ConnectStarted(val surface: ByoeSurface, val importMode: ByoeImportMode) : ByoeTelemetryEvent

    data class ConnectSucceeded(val surface: ByoeSurface) : ByoeTelemetryEvent

    data class ConnectAbandoned(val surface: ByoeSurface) : ByoeTelemetryEvent

    data class ConnectFailed(val surface: ByoeSurface, val errorType: ByoeConnectErrorType) : ByoeTelemetryEvent

    data class ExploreClicked(val surface: ByoeSurface) : ByoeTelemetryEvent

    /** Report it on every inbox view. */
    data object InboxViewed : ByoeTelemetryEvent

    data object UpsellWallShown : ByoeTelemetryEvent

    data class UpgradeBannerShown(val origin: ByoeBannerOrigin) : ByoeTelemetryEvent

    data class UpgradeBannerCtaClicked(val origin: ByoeBannerOrigin, val cta: ByoeBannerCta) : ByoeTelemetryEvent

    data object UpsellScreenShown : ByoeTelemetryEvent

    data class UpsellCtaClicked(val cta: ByoeUpsellCta) : ByoeTelemetryEvent

    data object UpsellSucceeded : ByoeTelemetryEvent

    /** [errorReason] is sent as given: a stable code, never a message. */
    data class UpsellFailed(val errorReason: String) : ByoeTelemetryEvent
}

/** Whether the import box was ticked when the user tapped Connect. */
enum class ByoeImportMode {
    Enabled,
    Disabled;

    companion object {

        fun of(importEmails: Boolean) = if (importEmails) Enabled else Disabled
    }
}

enum class ByoeBannerOrigin {

    /** No Android call site: there is no upgrade banner in the settings. */
    Settings,
    InboxStorageLimit
}

enum class ByoeBannerCta {
    Upgrade,
    NotNow
}

enum class ByoeUpsellCta {
    Primary,
    Secondary
}
