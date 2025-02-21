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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.getDimensionValue
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepositoryImpl
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.BaseDimensions
import ch.protonmail.upselling.domain.repository.UpsellingTelemetryRepositoryTestHelper.mockInstant
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(Parameterized::class)
internal class UpsellingTelemetryRepositoryImplDimensionsTest(testName: String, private val testInput: TestInput) {

    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val getSubscriptionName = mockk<GetSubscriptionName>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val telemetryEnabled = mockk<Provider<Boolean>> { every { this@mockk.get() } returns true }
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

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
    fun test() = runTest {
        // Given
        expectValidUserData()

        val eventType = UpsellingTelemetryEventType.Base.MailboxButtonTap
        val expectedEvent = UpsellingTelemetryEvent.UpsellButtonTapped(
            BaseDimensions.apply {
                addUpsellEntryPoint(testInput.entryPoint.getDimensionValue())
                addUpsellModalVersion()
            }
        ).toTelemetryEvent()

        // When
        repository.trackEvent(eventType, testInput.entryPoint)

        // Then
        coVerifySequence {
            getPrimaryUser()
            getAccountAgeInDays(user)
            getSubscriptionName(user.userId)
            telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate)
        }
    }

    private fun expectValidUserData() {
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(1)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("free").right()
    }

    companion object {

        private fun baseEntryPointInput(entryPoint: UpsellingEntryPoint) = arrayOf(
            TestInput(
                testName = "should return modal A.1 on ${entryPoint.getDimensionValue()}",
                entryPoint = entryPoint,
                expectedModalVersion = "A.1"
            )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            *baseEntryPointInput(UpsellingEntryPoint.Feature.Mailbox),
            *baseEntryPointInput(UpsellingEntryPoint.Feature.AutoDelete),
            *baseEntryPointInput(UpsellingEntryPoint.Feature.ContactGroups),
            *baseEntryPointInput(UpsellingEntryPoint.Feature.Folders),
            *baseEntryPointInput(UpsellingEntryPoint.Feature.Labels),
            *baseEntryPointInput(UpsellingEntryPoint.Feature.MobileSignature),
            *baseEntryPointInput(UpsellingEntryPoint.PostOnboarding)
        ).map { arrayOf(it.testName, it) }
    }

    data class TestInput(
        val testName: String,
        val entryPoint: UpsellingEntryPoint,
        val expectedModalVersion: String
    )
}
