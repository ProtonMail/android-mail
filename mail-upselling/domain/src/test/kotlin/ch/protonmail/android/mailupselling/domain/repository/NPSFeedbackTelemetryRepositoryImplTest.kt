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

package ch.protonmail.android.mailupselling.domain.repository

import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserSample
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEvent.Skipped
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEvent.SubmitButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.AccountAge
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.SubscriptionName
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.toUpsellingTelemetryDimensionValue
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName.GetSubscriptionNameError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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

internal class NPSFeedbackTelemetryRepositoryImplTest {

    private val getAccountAgeInDays = mockk<GetAccountAgeInDays>()
    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val getSubscriptionName = mockk<GetSubscriptionName>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val getAppLocale = mockk<GetAppLocale>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)
    private val getInstalledProtonApps = mockk<GetInstalledProtonApps>()

    private val sut: NPSFeedbackTelemetryRepository
        get() = NPSFeedbackTelemetryRepositoryImpl(
            getAccountAgeInDays,
            getPrimaryUser,
            getSubscriptionName,
            telemetryManager,
            getAppLocale = getAppLocale,
            scopeProvider = scopeProvider,
            getInstalledProtonApps = getInstalledProtonApps
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
    fun `should not track events when primary user is not found`() {
        // Given
        coEvery { getPrimaryUser() } returns null

        // When
        sut.trackEvent(NPSFeedbackTelemetryEventType.Skipped)

        // Then
        verify(exactly = 0) { telemetryManager.enqueue(any(), any(), any()) }
    }

    @Test
    fun `should not track events when subscription name is not found`() {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(3)
        coEvery { getSubscriptionName(user.userId) } returns GetSubscriptionNameError.left()

        // When
        sut.trackEvent(NPSFeedbackTelemetryEventType.Skipped)

        // Then
        verify(exactly = 0) { telemetryManager.enqueue(any(), any(), any()) }
    }

    @Test
    fun `should track skipped event`() = runTest {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(7)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("free").right()
        every { getAppLocale() } returns Locale.ENGLISH
        every { getInstalledProtonApps() } returns emptySet()

        val expectedDimensions = NPSFeedbackEventDimensions().apply {
            addPlanName("free")
            addDaysSinceAccountCreation(AccountAge(7).toUpsellingTelemetryDimensionValue())
            addTerritory(Locale.ENGLISH, TimeZone.getDefault())
            addInstalledApps(emptySet())
            addNoRatingValue()
        }
        val expectedEvent = Skipped(expectedDimensions).toTelemetryEvent()

        // When
        sut.trackEvent(NPSFeedbackTelemetryEventType.Skipped)

        // Then
        coVerify(exactly = 1) { telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate) }
    }

    @Test
    fun `should track submit tap event without comment`() = runTest {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(2)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("unlimited").right()
        every { getAppLocale() } returns Locale.FRENCH
        every { getInstalledProtonApps() } returns setOf(InstalledProtonApp.Calendar, InstalledProtonApp.VPN)

        val ratingValue = 4
        val expectedDimensions = NPSFeedbackEventDimensions().apply {
            addPlanName("unlimited")
            addDaysSinceAccountCreation(AccountAge(2).toUpsellingTelemetryDimensionValue())
            addTerritory(Locale.FRENCH, TimeZone.getDefault())
            addInstalledApps(setOf(InstalledProtonApp.Calendar, InstalledProtonApp.VPN))
            addRatingValue(ratingValue)
        }
        val expectedEvent = SubmitButtonTapped(expectedDimensions).toTelemetryEvent()

        // When
        sut.trackEvent(NPSFeedbackTelemetryEventType.SubmitTap(ratingValue, null))

        // Then
        coVerify(exactly = 1) { telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate) }
    }

    @Test
    fun `should track submit tap event with comment`() = runTest {
        // Given
        coEvery { getPrimaryUser() } returns user
        every { getAccountAgeInDays(user) } returns AccountAge(5)
        coEvery { getSubscriptionName(user.userId) } returns SubscriptionName("plus").right()
        every { getAppLocale() } returns Locale.GERMAN
        every { getInstalledProtonApps() } returns setOf(InstalledProtonApp.Drive, InstalledProtonApp.VPN)

        val ratingValue = 10
        val comment = "Great app!"
        val expectedDimensions = NPSFeedbackEventDimensions().apply {
            addPlanName("plus")
            addDaysSinceAccountCreation(AccountAge(5).toUpsellingTelemetryDimensionValue())
            addTerritory(Locale.GERMAN, TimeZone.getDefault())
            addInstalledApps(setOf(InstalledProtonApp.Drive, InstalledProtonApp.VPN))
            addComment(comment)
            addRatingValue(ratingValue)
        }
        val expectedEvent = SubmitButtonTapped(expectedDimensions).toTelemetryEvent()

        // When
        sut.trackEvent(NPSFeedbackTelemetryEventType.SubmitTap(ratingValue, comment))

        // Then
        coVerify(exactly = 1) { telemetryManager.enqueue(user.userId, expectedEvent, TelemetryPriority.Immediate) }
    }

    private fun mockInstant() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { epochSecond } returns 0 }
    }
}
