/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ObserveOngoingIndexingUserIdTest {

    private val userId = UserId("user-1")
    private val scheduler = mockk<ContentIndexingScheduler>()
    private val observeOngoingIndexingUserId = ObserveOngoingIndexingUserId(scheduler)

    @Test
    fun `emits the ongoing user id and null observed from the scheduler`() = runTest {
        // Given
        every { scheduler.observeOngoingUserId() } returns flowOf(userId, null)

        // When + Then
        observeOngoingIndexingUserId().test {
            assertEquals(userId, awaitItem())
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
