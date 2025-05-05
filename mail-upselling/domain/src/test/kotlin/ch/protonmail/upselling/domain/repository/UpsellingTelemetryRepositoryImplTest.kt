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
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.getDimensionValue
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName.GetSubscriptionNameError
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.BaseDimensions
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.UpgradeDimensionsNormal
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.UpgradeDimensionsPromoA
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.UpgradeDimensionsPromoB
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.UpgradeDimensionsSocialProof
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.mockInstant
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
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

internal class UpsellingTelemetryRepositoryImplTest {

    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val getSubscriptionName = mockk<GetSubscriptionName>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val telemetryEnabled = mockk<Provider<Boolean>>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val expectedUpsellingEntryPoint = UpsellingEntryPoint.Feature.ContactGroups

    private val repository: UpsellingTelemetryRepository
        get() = UpsellingTelemetryRepositoryImpl(
            getAccountAgeInDays,
            getPrimaryUser,
            getSubscriptionName,
            telemetryManager,
            telemetryEnabled.get(),
            scopeProvider
        )

    private val user = UserSample.Primary

    @BeforeTest
    fun setup() {
        mockInstant()
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should not track event if the upselling 1 click telemetry flag is disabled`() = runTest {
        // Given
        expectTelemetryDisabled()
        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        verify { telemetryManager wasNot called }
    }

    @Test
    fun `should track the base mailbox button tap event`() = runTest {
        // Given
        expectValidUserData()
        expectTelemetryEnabled()

        val entryPoint = UpsellingEntryPoint.Feature.Mailbox
        val expectedDimensions = BaseDimensions.apply {
            addUpsellModalVersion()
            addUpsellEntryPoint(entryPoint.getDimensionValue())
        }

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap
        val expectedEvent = UpsellingTelemetryEvent.UpsellButtonTapped(expectedDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, entryPoint)

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
        expectTelemetryEnabled()

        val payload = UpsellingTelemetryTargetPlanPayload(
            "mail2022", 1,
            isPromotional = true,
            isVariantB = true,
            isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeAttempt(payload)
        val expectedEvent = UpsellingTelemetryEvent.UpgradeAttempt(UpgradeDimensionsPromoB).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should track the upgrade cancelled event`() = runTest {
        // Given
        expectValidUserData()
        expectTelemetryEnabled()

        val payload = UpsellingTelemetryTargetPlanPayload(
            "mail2022", 1,
            isPromotional = false,
            isVariantB = false,
            isSocialProofVariant = true
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeCancelled(payload)
        val expectedEvent = UpsellingTelemetryEvent.UpgradeCancelled(UpgradeDimensionsSocialProof).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should track the upgrade errored event`() = runTest {
        // Given
        expectValidUserData()
        expectTelemetryEnabled()

        val payload = UpsellingTelemetryTargetPlanPayload(
            "mail2022", 1,
            isPromotional = true,
            isVariantB = false,
            isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.UpgradeErrored(payload)
        val expectedEvent = UpsellingTelemetryEvent.UpgradeErrored(UpgradeDimensionsPromoA).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

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
        expectTelemetryEnabled()

        val payload = UpsellingTelemetryTargetPlanPayload(
            "mail2022", 1,
            isPromotional = false,
            isVariantB = false,
            isSocialProofVariant = false
        )
        val eventType = UpsellingTelemetryEventType.Upgrade.PurchaseCompleted(payload)
        val expectedEvent = UpsellingTelemetryEvent.PurchaseCompleted(UpgradeDimensionsNormal).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should put correct UpsellingEntryPoint into Dimensions`() = runTest {
        // Given
        expectValidUserData()
        expectTelemetryEnabled()
        val customUpsellingEntryPoint = UpsellingEntryPoint.Feature.MobileSignature

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap
        val expectedEvent = UpsellingTelemetryEvent.UpsellButtonTapped(
            BaseDimensions.apply {
                addUpsellEntryPoint(customUpsellingEntryPoint.getDimensionValue())
            }
        ).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, customUpsellingEntryPoint)

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
        expectTelemetryEnabled()
        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        verify { telemetryManager wasNot called }
    }

    @Test
    fun `should not send telemetry event if the subscription name can not be obtained`() {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns GetSubscriptionNameError.left()
        expectTelemetryEnabled()

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap

        // When
        repository.trackEvent(eventType, expectedUpsellingEntryPoint)

        // Then
        verify { telemetryManager wasNot called }
    }

    private fun expectValidUserData() {
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("free").right()
    }

    private fun expectTelemetryEnabled() {
        every { telemetryEnabled.get() } returns true
    }

    private fun expectTelemetryDisabled() {
        every { telemetryEnabled.get() } returns false
    }
}
