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

package ch.protonmail.android.mailsettings.presentation.settings.appicon.model

internal data class AppIconData(
    val id: AppIconId,
    val componentName: String,
    val category: IconCategory
) {

    enum class AppIconId {
        Default, Weather, Notes, Calculator
    }

    enum class IconCategory {
        ProtonMail, Discreet
    }

    companion object {
        val DEFAULT = AppIconData(
            id = AppIconId.Default,
            componentName = ".RoutingActivity",
            category = IconCategory.ProtonMail
        )
        val WEATHER = AppIconData(
            id = AppIconId.Weather,
            componentName = ".RoutingWeather",
            category = IconCategory.Discreet
        )
        val NOTES = AppIconData(
            id = AppIconId.Notes,
            componentName = ".RoutingNotes",
            category = IconCategory.Discreet
        )
        val CALCULATOR = AppIconData(
            id = AppIconId.Calculator,
            componentName = ".RoutingCalculator",
            category = IconCategory.Discreet
        )

        val ALL_ICONS = listOf(DEFAULT, WEATHER, NOTES, CALCULATOR)
    }
}

internal fun AppIconData.activityAliasName(activityAliasPrefix: String): String = activityAliasPrefix + componentName
