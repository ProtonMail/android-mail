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

sealed class UpsellingTelemetryEventDimensionsKey(val name: String) {
    data object PlanBeforeUpgrade : UpsellingTelemetryEventDimensionsKey("plan_before_upgrade")
    data object DaysSinceAccountCreation : UpsellingTelemetryEventDimensionsKey("days_since_account_creation")
    data object UpsellModalVersion : UpsellingTelemetryEventDimensionsKey("upsell_modal_version")
    data object UpsellVariant : UpsellingTelemetryEventDimensionsKey("upsell_variant")
    data object UpsellIsPromotional : UpsellingTelemetryEventDimensionsKey("upsell_is_promotional")
    data object SelectedPlan : UpsellingTelemetryEventDimensionsKey("selected_plan")
    data object SelectedCycle : UpsellingTelemetryEventDimensionsKey("selected_cycle")
    data object UpsellEntryPoint : UpsellingTelemetryEventDimensionsKey("upsell_entry_point")
}
