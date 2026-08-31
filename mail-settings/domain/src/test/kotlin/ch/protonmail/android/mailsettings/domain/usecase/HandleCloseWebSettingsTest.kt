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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.repository.MailSettingsRefreshRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test

internal class HandleCloseWebSettingsTest {

    private val observePrimaryUserId = mockk<ObservePrimaryUserId>()
    private val eventLoopRepository = mockk<EventLoopRepository>()
    private val mailSettingsRefreshRepository = mockk<MailSettingsRefreshRepository>()

    private val handleCloseWebSettings = HandleCloseWebSettings(
        observePrimaryUserId,
        eventLoopRepository,
        mailSettingsRefreshRepository
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should not trigger the event loop nor refresh mail settings when the user id cannot be fetched`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(null)

        // When
        handleCloseWebSettings()

        // Then
        verify { eventLoopRepository wasNot called }
        verify { mailSettingsRefreshRepository wasNot called }
    }

    @Test
    fun `should trigger the event loop and refresh mail settings for the primary userId`() = runTest {
        // Given
        every { observePrimaryUserId() } returns flowOf(BaseUserId)
        coEvery { eventLoopRepository.trigger(BaseUserId) } just runs
        coEvery { mailSettingsRefreshRepository.refresh(BaseUserId) } just runs

        // When
        handleCloseWebSettings()

        // Then
        coVerify(exactly = 1) {
            eventLoopRepository.trigger(BaseUserId)
            mailSettingsRefreshRepository.refresh(BaseUserId)
        }
        confirmVerified(eventLoopRepository, mailSettingsRefreshRepository)
    }

    private companion object {

        val BaseUserId = UserId("userId")
    }
}
