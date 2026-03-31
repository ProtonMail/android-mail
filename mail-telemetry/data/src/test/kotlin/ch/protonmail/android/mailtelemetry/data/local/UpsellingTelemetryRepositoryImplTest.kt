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

import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailtelemetry.data.mapper.toLocal
import ch.protonmail.android.mailtelemetry.domain.model.GeneralDimensions
import ch.protonmail.android.mailtelemetry.domain.model.PlanSpecificDimensions
import ch.protonmail.android.mailtelemetry.domain.model.UpsellEntryPoint
import ch.protonmail.android.mailtelemetry.domain.model.UpsellFeatureFlags
import ch.protonmail.android.mailtelemetry.domain.model.UpsellModalVariant
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

class UpsellingTelemetryRepositoryImplTest {

    private val upsellingTelemetryDataSource = mockk<UpsellingTelemetryDataSource>(relaxUnitFun = true)
    private val executeWithUserSession = mockk<ExecuteWithUserSession>()

    private val upsellingTelemetryRepository = UpsellingTelemetryRepositoryImpl(
        upsellingTelemetryDataSource,
        executeWithUserSession
    )

    @Test
    fun `record upgrade attempt`() = runTest {
        // Given
        val mockSession = mockk<MailUserSessionWrapper>()
        expectMockSession(userId, mockSession)

        // When
        upsellingTelemetryRepository.recordUpgradeAttempt(userId, generalDimensions, planSpecificDimensions)

        // Then
        coVerify {
            upsellingTelemetryDataSource.recordUpgradeAttempt(
                mockSession,
                generalDimensions.toLocal(),
                planSpecificDimensions.toLocal()
            )
        }
    }

    @Test
    fun `record upgrade cancelled by user`() = runTest {
        // Given
        val mockSession = mockk<MailUserSessionWrapper>()
        expectMockSession(userId, mockSession)

        // When
        upsellingTelemetryRepository.recordUpgradeCancelledByUser(userId, generalDimensions, planSpecificDimensions)

        // Then
        coVerify {
            upsellingTelemetryDataSource.recordUpgradeCancelledByUser(
                mockSession,
                generalDimensions.toLocal(),
                planSpecificDimensions.toLocal()
            )
        }
    }

    @Test
    fun `record upgrade error`() = runTest {
        // Given
        val mockSession = mockk<MailUserSessionWrapper>()
        expectMockSession(userId, mockSession)

        // When
        upsellingTelemetryRepository.recordUpgradeError(userId, generalDimensions, planSpecificDimensions)

        // Then
        coVerify {
            upsellingTelemetryDataSource.recordUpgradeError(
                mockSession,
                generalDimensions.toLocal(),
                planSpecificDimensions.toLocal()
            )
        }
    }

    @Test
    fun `record upgrade success`() = runTest {
        // Given
        val mockSession = mockk<MailUserSessionWrapper>()
        expectMockSession(userId, mockSession)

        // When
        upsellingTelemetryRepository.recordUpgradeSuccess(userId, generalDimensions, planSpecificDimensions)

        // Then
        coVerify {
            upsellingTelemetryDataSource.recordUpgradeSuccess(
                mockSession,
                generalDimensions.toLocal(),
                planSpecificDimensions.toLocal()
            )
        }
    }

    @Test
    fun `record upsell button tapped`() = runTest {
        // Given
        val mockSession = mockk<MailUserSessionWrapper>()
        expectMockSession(userId, mockSession)

        // When
        upsellingTelemetryRepository.recordUpsellButtonTapped(userId, generalDimensions)

        // Then
        coVerify {
            upsellingTelemetryDataSource.recordUpsellButtonTapped(
                mockSession,
                generalDimensions.toLocal()
            )
        }
    }

    private fun expectMockSession(userId: UserId, mockSession: MailUserSessionWrapper) {
        coEvery {
            executeWithUserSession(
                userId,
                any<suspend (MailUserSessionWrapper) -> Either<DataError, Unit>>()
            )
        } coAnswers {
            val action = secondArg<suspend (MailUserSessionWrapper) -> Either<DataError, Unit>>()
            action(mockSession).right()
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
