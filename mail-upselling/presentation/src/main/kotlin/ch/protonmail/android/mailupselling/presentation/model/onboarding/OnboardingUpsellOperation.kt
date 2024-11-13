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

package ch.protonmail.android.mailupselling.presentation.model.onboarding

import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PlansType

sealed interface OnboardingUpsellOperation {
    sealed interface Action : OnboardingUpsellOperation {
        data class PlanSelected(val plansType: PlansType, val planName: String) : Action

        sealed class TrackEvent(val payload: UpsellingTelemetryTargetPlanPayload) : Action {
            class UpgradeAttempt(payload: UpsellingTelemetryTargetPlanPayload) : TrackEvent(payload)
            class UpgradeCancelled(payload: UpsellingTelemetryTargetPlanPayload) : TrackEvent(payload)
            class UpgradeErrored(payload: UpsellingTelemetryTargetPlanPayload) : TrackEvent(payload)
            class UpgradeSuccess(payload: UpsellingTelemetryTargetPlanPayload) : TrackEvent(payload)
        }
    }
}
