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

package ch.protonmail.android.mailupselling.domain.model.telemetry

import java.util.Locale
import java.util.TimeZone
import ch.protonmail.android.mailupselling.domain.repository.InstalledProtonApp

internal class NPSFeedbackEventDimensions {

    private val mutableMap = mutableMapOf<String, String>(
        NPSFeedbackEventDimensionsKey.DeviceOS.name to "Android"
    )
    fun asMap() = mutableMap.toMap()

    fun addPlanName(value: String?) {
        if (value == null) return
        mutableMap.put(NPSFeedbackEventDimensionsKey.PlanName.name, value)
    }

    fun addTerritory(locale: Locale, timezone: TimeZone) {
        val value = "${locale.displayName}-${timezone.displayName}"
        mutableMap.put(NPSFeedbackEventDimensionsKey.Territory.name, value)
    }

    fun addDaysSinceAccountCreation(value: String) =
        mutableMap.put(NPSFeedbackEventDimensionsKey.DaysSinceAccountCreation.name, value)

    fun addRatingValue(value: Int) = mutableMap.put(NPSFeedbackEventDimensionsKey.RatingValue.name, value.toString())

    fun addNoRatingValue() = mutableMap.put(NPSFeedbackEventDimensionsKey.RatingValue.name, NO_RATING_VALUE)

    fun addComment(value: String) = mutableMap.put(NPSFeedbackEventDimensionsKey.Comment.name, value)

    fun addInstalledApps(apps: Set<InstalledProtonApp>) = with(mutableMap) {
        put(NPSFeedbackEventDimensionsKey.VpnInstalled.name, apps.contains(InstalledProtonApp.VPN).toString())
        put(NPSFeedbackEventDimensionsKey.DriveInstalled.name, apps.contains(InstalledProtonApp.Drive).toString())
        put(NPSFeedbackEventDimensionsKey.CalendarInstalled.name, apps.contains(InstalledProtonApp.Calendar).toString())
        put(NPSFeedbackEventDimensionsKey.PassInstalled.name, apps.contains(InstalledProtonApp.Pass).toString())
        put(NPSFeedbackEventDimensionsKey.WalletInstalled.name, apps.contains(InstalledProtonApp.Wallet).toString())
    }
}

private const val NO_RATING_VALUE = "-1"

private sealed class NPSFeedbackEventDimensionsKey(val name: String) {
    data object PlanName : NPSFeedbackEventDimensionsKey("plan_name")
    data object RatingValue : NPSFeedbackEventDimensionsKey("rating_value")
    data object Comment : NPSFeedbackEventDimensionsKey("comment")
    data object Territory : NPSFeedbackEventDimensionsKey("territory")
    data object DeviceOS : NPSFeedbackEventDimensionsKey("device_os")
    data object VpnInstalled : NPSFeedbackEventDimensionsKey("vpn_installed")
    data object DriveInstalled : NPSFeedbackEventDimensionsKey("drive_installed")
    data object CalendarInstalled : NPSFeedbackEventDimensionsKey("calendar_installed")
    data object PassInstalled : NPSFeedbackEventDimensionsKey("pass_installed")
    data object WalletInstalled : NPSFeedbackEventDimensionsKey("wallet_installed")
    data object DaysSinceAccountCreation : NPSFeedbackEventDimensionsKey("days_since_account_creation")
}
