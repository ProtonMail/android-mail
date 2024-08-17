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

class UpsellingTelemetryEventDimensions {

    private val mutableMap = mutableMapOf<String, String>()
    fun asMap() = mutableMap.toMap()

    fun addPlanBeforeUpgrade(value: String) =
        mutableMap.put(UpsellingTelemetryEventDimensionsKey.PlanBeforeUpgrade.name, value)

    fun addDaysSinceAccountCreation(value: String) =
        mutableMap.put(UpsellingTelemetryEventDimensionsKey.DaysSinceAccountCreation.name, value)

    fun addUpsellModalVersion(value: String = "A.1") =
        mutableMap.put(UpsellingTelemetryEventDimensionsKey.UpsellModalVersion.name, value)

    fun addSelectedPlan(value: String) = mutableMap.put(UpsellingTelemetryEventDimensionsKey.SelectedPlan.name, value)

    fun addSelectedPlanCycle(value: Int) =
        mutableMap.put(UpsellingTelemetryEventDimensionsKey.SelectedCycle.name, value.toString())

    fun addUpsellEntryPoint(value: String) =
        mutableMap.put(UpsellingTelemetryEventDimensionsKey.UpsellEntryPoint.name, value)
}
