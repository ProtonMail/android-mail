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

package ch.protonmail.android.mailsession.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import uniffi.mail_uniffi.FeatureFlagsRefreshOutcome
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class RefreshFeatureFlagsTest {

    private val userSessionRepository = mockk<UserSessionRepository>()
    private val refreshFeatureFlags = RefreshFeatureFlags(userSessionRepository)

    @Test
    fun `refreshes the user's flags with a 3 second timeout`() = runTest {
        // Given
        coEvery { userSessionRepository.refreshFeatureFlags(UserIdTestData.userId, 3.seconds) } returns
            FeatureFlagsRefreshOutcome.REFRESHED.right()

        // When
        refreshFeatureFlags(UserIdTestData.userId)

        // Then
        coVerify(exactly = 1) { userSessionRepository.refreshFeatureFlags(UserIdTestData.userId, 3.seconds) }
    }

    @Test
    fun `completes when the refresh fails`() = runTest {
        // Given
        coEvery { userSessionRepository.refreshFeatureFlags(any(), any()) } returns
            DataError.Local.NoUserSession.left()

        // When
        refreshFeatureFlags(UserIdTestData.userId)

        // Then
        coVerify(exactly = 1) { userSessionRepository.refreshFeatureFlags(UserIdTestData.userId, any()) }
    }
}
