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

package ch.protonmail.android.mailtelemetry.domain.usecase

import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.mailtelemetry.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RecordUpgradeAttemptTest {

    private val upsellingTelemetryRepository = mockk<UpsellingTelemetryRepository>(relaxUnitFun = true)

    private val recordUpgradeAttempt = RecordUpgradeAttempt(upsellingTelemetryRepository)

    @Test
    fun `call repository method when use case is invoked`() = runTest {
        // When
        recordUpgradeAttempt(userId, generalDimensions, planSpecificDimensions)

        // Then
        coVerify {
            upsellingTelemetryRepository.recordUpgradeAttempt(userId, generalDimensions, planSpecificDimensions)
        }
    }

    companion object {

        val userId = UserIdTestData.userId
        val generalDimensions = GeneralDimensions(
            upsellEntryPoint = UpsellEntryPoint.NAVBAR_UPSELL,
            planBeforeUpgrade = "Free plan",
            modalVariant = UpsellModalVariant.COMPARISON_PLUS,
            upsellFeatureFlags = UpsellFeatureFlags(
                parentFlagName = "MailAndroidV7UnlimitedPlanPlacementRegions",
                childFlagName = "MailAndroidV7UnlimitedPlanPlacementExperiment"
            )
        )
        val planSpecificDimensions = PlanSpecificDimensions(
            selectedPlan = "Mail Plus",
            selectedCycle = "Monthly",
            upsellIsPromotional = false
        )
    }
}
