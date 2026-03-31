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

package ch.protonmail.android.mailtelemetry.domain.repository

import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import me.proton.core.domain.entity.UserId

interface UpsellingTelemetryRepository {

    suspend fun recordUpgradeAttempt(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    )

    suspend fun recordUpgradeCancelledByUser(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    )

    suspend fun recordUpgradeError(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    )

    suspend fun recordUpgradeSuccess(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    )

    suspend fun recordUpsellButtonTapped(userId: UserId, general: GeneralDimensions)
}
