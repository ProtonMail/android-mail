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

package ch.protonmail.android.mailnotifications.data.repository

import java.time.Instant
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEvent
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventDimensions
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventType
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogType
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class NotificationPermissionTelemetryRepositoryImplTest {

    private val getPrimaryUser = mockk<GetPrimaryUser>()
    private val telemetryManager = mockk<TelemetryManager>()
    private val dispatcherProvider = TestDispatcherProvider(UnconfinedTestDispatcher())
    private val scopeProvider = TestCoroutineScopeProvider(dispatcherProvider)

    private val repository = NotificationPermissionTelemetryRepositoryImpl(
        getPrimaryUser, telemetryManager, scopeProvider
    )

    @BeforeTest
    fun setup() {
        mockInstant()
    }

    @Test
    fun `should not track events when primary user is not found`() {
        // Given
        coEvery { getPrimaryUser() } returns null

        // When
        repository.trackEvent(
            NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                NotificationPermissionDialogType.PostOnboarding
            )
        )

        // Then
        verify(exactly = 0) { telemetryManager.enqueue(any(), any(), any()) }
    }

    @Test
    fun `should track dialog displayed event`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user

        // When
        repository.trackEvent(
            NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                NotificationPermissionDialogType.PostOnboarding
            )
        )

        // Then
        val dimensions = NotificationPermissionTelemetryEventDimensions()
        dimensions.addNotificationPermissionDialogType("post_onboarding")
        val event = NotificationPermissionTelemetryEvent.NotificationPermissionDialogDisplayed(dimensions)
            .toTelemetryEvent()
        verify { telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate) }
    }

    @Test
    fun `should track enable button clicked event`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user

        // When
        repository.trackEvent(
            NotificationPermissionTelemetryEventType.NotificationPermissionDialogEnable(
                NotificationPermissionDialogType.PostSending
            )
        )

        // Then
        val dimensions = NotificationPermissionTelemetryEventDimensions()
        dimensions.addNotificationPermissionDialogType("post_sending")
        val event = NotificationPermissionTelemetryEvent.NotificationPermissionDialogEnable(dimensions)
            .toTelemetryEvent()
        verify { telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate) }
    }

    @Test
    fun `should track dismiss button clicked event`() {
        // Given
        val user = UserTestData.Primary
        coEvery { getPrimaryUser() } returns user

        // When
        repository.trackEvent(
            NotificationPermissionTelemetryEventType.NotificationPermissionDialogDismiss(
                NotificationPermissionDialogType.PostOnboarding
            )
        )

        // Then
        val dimensions = NotificationPermissionTelemetryEventDimensions()
        dimensions.addNotificationPermissionDialogType("post_onboarding")
        val event = NotificationPermissionTelemetryEvent.NotificationPermissionDialogDismiss(dimensions)
            .toTelemetryEvent()
        verify { telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate) }
    }

    private fun mockInstant() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { epochSecond } returns 0 }
    }
}
