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

package ch.protonmail.android.mailsession.domain.wrapper

import arrow.core.right
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.FeatureFlagsRefreshOutcome
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionRefreshFeatureFlagsResult
import uniffi.mail_uniffi.ProtonError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MailUserSessionWrapperRefreshFeatureFlagsTest {

    private val userSession = mockk<MailUserSession>()
    private val wrapper = MailUserSessionWrapper(userSession)

    @Test
    fun `returns the outcome and passes the timeout in milliseconds`() = runTest {
        // Given
        coEvery { userSession.refreshFeatureFlags(3_000uL) } returns
            MailUserSessionRefreshFeatureFlagsResult.Ok(FeatureFlagsRefreshOutcome.TIMED_OUT)

        // When
        val result = wrapper.refreshFeatureFlags(3.seconds)

        // Then
        assertEquals(FeatureFlagsRefreshOutcome.TIMED_OUT.right(), result)
    }

    @Test
    fun `returns an error when the refresh fails`() = runTest {
        // Given
        coEvery { userSession.refreshFeatureFlags(any()) } returns
            MailUserSessionRefreshFeatureFlagsResult.Error(ProtonError.Network)

        // When
        val result = wrapper.refreshFeatureFlags(3.seconds)

        // Then
        assertTrue(result.isLeft())
    }
}
