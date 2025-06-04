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
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEvent.DriveSpotlightMailboxButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.DriveSpotlightTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName.GetSubscriptionNameError
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

internal class DriveSpotlightTelemetryRepositoryImplTest {

    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val getSubscriptionName = mockk<GetSubscriptionName>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val baseDimensions: DriveSpotlightEventDimensions
        get() = DriveSpotlightEventDimensions().apply {
            addPlanBeforeUpgrade("free")
            addDaysSinceAccountCreation("01-03")
        }

    private val repository: DriveSpotlightTelemetryRepository
        get() = DriveSpotlightTelemetryRepositoryImpl(
            getAccountAgeInDays,
            getPrimaryUser,
            getSubscriptionName,
            telemetryManager,
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
    fun `should track the mailbox button tap event`() = runTest {
        // Given
        expectValidUserData()

        val expectedDimensions = baseDimensions
        val expectedEvent = DriveSpotlightMailboxButtonTapped(expectedDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(DriveSpotlightTelemetryEventType.MailboxDriveSpotlightButtonTap)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    @Test
    fun `should track CTA tap event`() = runTest {
        // Given
        expectValidUserData()

        val expectedDimensions = baseDimensions
        val expectedEvent = DriveSpotlightEvent.DriveSpotlightCTAButtonTapped(expectedDimensions).toTelemetryEvent()

        // When
        repository.trackEvent(DriveSpotlightTelemetryEventType.DriveSpotlightCTATap)

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
        // Given + When
        repository.trackEvent(DriveSpotlightTelemetryEventType.DriveSpotlightCTATap)

        // Then
        verify { telemetryManager wasNot called }
    }

    @Test
    fun `should not send telemetry event if the subscription name can not be obtained`() {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns GetSubscriptionNameError.left()

        // When
        repository.trackEvent(DriveSpotlightTelemetryEventType.MailboxDriveSpotlightButtonTap)

        // Then
        verify { telemetryManager wasNot called }
    }

    private fun expectValidUserData() {
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("free").right()
    }
}
