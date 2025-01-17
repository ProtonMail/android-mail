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

package ch.protonmail.android.mailbugreport.domain

/**
 * Holds the feature flag configurations for the "Application logs" feature accessible from the Settings.
 *
 * @param enabled Enables the "Application logs" section in the settings and logs application events
 * to a file in the internal app storage (cache) during the application lifecycle.
 * Moreover, it allows users to attach logs when reporting an issue via the "Report a Problem" screen,
 * accessible from the Sidebar menu.
 *
 * @param internalEnabled Enables the intermediate "Application logs" screen and also allows exporting logcat data
 * from the device in use. This is enabled and available only for the internal QA team to provide logs
 * to the development team while testing features in development.
 *
 * Note: both settings are enabled by default regardless of the feature flags in non-production builds.
 */
data class LogsExportFeatureSetting(
    private val enabled: Boolean,
    private val internalEnabled: Boolean
) {

    val isEnabled = enabled || internalEnabled
    val isInternalFeatureEnabled = internalEnabled
}
