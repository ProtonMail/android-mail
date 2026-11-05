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

package ch.protonmail.android.mailbyoe.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test

internal class ReportByoeTelemetryEventTest {

    private val byoeGuidanceRepository = mockk<ByoeGuidanceRepository>()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a user, when reporting an event, then it is reported for that user`() = runTest {
        // Given
        val event = ByoeTelemetryEvent.ExploreClicked(ByoeSurface.SidebarBanner)
        coEvery { byoeGuidanceRepository.report(userId, event) } returns Unit.right()
        val reportByoeTelemetryEvent = reportByoeTelemetryEvent()

        // When
        reportByoeTelemetryEvent(event, userId)

        // Then
        coVerify(exactly = 1) { byoeGuidanceRepository.report(userId, event) }
    }

    @Test
    fun `given no user, when reporting an event, then it is reported for the primary user`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(primaryUserId)
        coEvery { byoeGuidanceRepository.report(primaryUserId, ByoeTelemetryEvent.InboxViewed) } returns Unit.right()
        val reportByoeTelemetryEvent = reportByoeTelemetryEvent()

        // When
        reportByoeTelemetryEvent(ByoeTelemetryEvent.InboxViewed)

        // Then
        coVerify(exactly = 1) { byoeGuidanceRepository.report(primaryUserId, ByoeTelemetryEvent.InboxViewed) }
    }

    @Test
    fun `given no primary user, when reporting an event, then nothing is reported`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)
        val reportByoeTelemetryEvent = reportByoeTelemetryEvent()

        // When
        reportByoeTelemetryEvent(ByoeTelemetryEvent.InboxViewed)

        // Then
        coVerify(exactly = 0) { byoeGuidanceRepository.report(any(), any()) }
    }

    @Test
    fun `given the report fails, when reporting an event, then the failure is not surfaced`() = runTest {
        // Given
        coEvery {
            byoeGuidanceRepository.report(userId, ByoeTelemetryEvent.SettingsView)
        } returns DataError.Local.NoUserSession.left()
        val reportByoeTelemetryEvent = reportByoeTelemetryEvent()

        // When
        reportByoeTelemetryEvent(ByoeTelemetryEvent.SettingsView, userId)

        // Then
        coVerify(exactly = 1) { byoeGuidanceRepository.report(userId, ByoeTelemetryEvent.SettingsView) }
    }

    private fun TestScope.reportByoeTelemetryEvent() = ReportByoeTelemetryEvent(
        byoeGuidanceRepository,
        observePrimaryUserId,
        appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private companion object {

        val userId = UserId("user")
        val primaryUserId = UserId("primary")
    }
}
