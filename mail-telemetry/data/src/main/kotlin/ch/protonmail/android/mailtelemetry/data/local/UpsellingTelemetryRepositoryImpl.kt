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

package ch.protonmail.android.mailtelemetry.data.local

import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailtelemetry.data.mapper.toLocal
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.repository.UpsellingTelemetryRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class UpsellingTelemetryRepositoryImpl @Inject constructor(
    private val dataSource: UpsellingTelemetryDataSource,
    private val executeWithUserSession: ExecuteWithUserSession
) : UpsellingTelemetryRepository {

    override suspend fun recordUpgradeAttempt(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    ) {
        executeWithUserSession(userId) { mailUserSession ->
            dataSource.recordUpgradeAttempt(
                mailUserSession = mailUserSession,
                general = general.toLocal(),
                planSpecific = planSpecific.toLocal()
            )
        }
    }

    override suspend fun recordUpgradeCancelledByUser(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    ) {
        executeWithUserSession(userId) { mailUserSession ->
            dataSource.recordUpgradeCancelledByUser(
                mailUserSession = mailUserSession,
                general = general.toLocal(),
                planSpecific = planSpecific.toLocal()
            )
        }
    }

    override suspend fun recordUpgradeError(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    ) {
        executeWithUserSession(userId) { mailUserSession ->
            dataSource.recordUpgradeError(
                mailUserSession = mailUserSession,
                general = general.toLocal(),
                planSpecific = planSpecific.toLocal()
            )
        }
    }

    override suspend fun recordUpgradeSuccess(
        userId: UserId,
        general: GeneralDimensions,
        planSpecific: PlanSpecificDimensions
    ) {
        executeWithUserSession(userId) { mailUserSession ->
            dataSource.recordUpgradeSuccess(
                mailUserSession = mailUserSession,
                general = general.toLocal(),
                planSpecific = planSpecific.toLocal()
            )
        }
    }

    override suspend fun recordUpsellButtonTapped(userId: UserId, general: GeneralDimensions) {
        executeWithUserSession(userId) { mailUserSession ->
            dataSource.recordUpsellButtonTapped(
                mailUserSession = mailUserSession,
                general = general.toLocal()
            )
        }
    }
}
