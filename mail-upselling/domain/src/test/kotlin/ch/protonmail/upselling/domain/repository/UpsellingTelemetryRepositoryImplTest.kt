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

package ch.protonmail.upselling.domain.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName.GetSubscriptionNameError
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.AfterTest

internal class UpsellingTelemetryRepositoryImplTest {

    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val getSubscriptionName = mockk<GetSubscriptionName>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val repository = UpsellingTelemetryRepositoryImpl(
        getAccountAgeInDays,
        getPrimaryUser,
        getSubscriptionName,
        telemetryManager,
        scopeProvider
    )

    private val user = UserSample.Primary

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should track the base mailbox button tap event`() = runTest {
        // Given
        expectValidUserData()

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap
        val expectedEvent = UpsellingTelemetryEvent.UpsellButtonTapped(BaseDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(eventType)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should track the upgrade attempt event`() = runTest {
        // Given
        expectValidUserData()

        val payload = UpsellingTelemetryTargetPlanPayload("mail2022", 1)
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeAttempt(payload)
        val expectedEvent = UpsellingTelemetryEvent.UpgradeAttempt(UpgradeDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(eventType)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should track the purchase completed event`() = runTest {
        // Given
        expectValidUserData()

        val payload = UpsellingTelemetryTargetPlanPayload("mail2022", 1)
        val eventType = UpsellingTelemetryEventType.Upgrade.PurchaseCompleted(payload)
        val expectedEvent = UpsellingTelemetryEvent.PurchaseCompleted(UpgradeDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(eventType)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should not send telemetry event if the primary user can not be obtained`() {
        // Given
        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap

        // When
        repository.trackEvent(eventType)

        // Then
        verify { telemetryManager wasNot called }
    }

    @Test
    fun `should not send telemetry event if the subscription name can not be obtained`() {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns GetSubscriptionNameError.left()

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap

        // When
        repository.trackEvent(eventType)

        // Then
        verify { telemetryManager wasNot called }
    }

    private fun expectValidUserData() {
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("free").right()
    }

    private companion object {

        val BaseDimensions = UpsellingTelemetryEventDimensions().apply {
            addPlanBeforeUpgrade("free")
            addDaysSinceAccountCreation("01-03")
            addUpsellModalVersion("A.1")
        }

        val UpgradeDimensions = UpsellingTelemetryEventDimensions().apply {
            addPlanBeforeUpgrade("free")
            addDaysSinceAccountCreation("01-03")
            addUpsellModalVersion("A.1")
            addSelectedPlan("mail2022")
            addSelectedPlanCycle(1)
        }
    }
}
