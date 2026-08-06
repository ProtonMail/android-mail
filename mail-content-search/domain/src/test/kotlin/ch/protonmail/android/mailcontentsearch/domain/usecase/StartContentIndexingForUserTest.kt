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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.ContentIndexingScheduler
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StartContentIndexingForUserTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRepository>()
    private val scheduler = mockk<ContentIndexingScheduler>(relaxUnitFun = true)

    private val startContentIndexingForUser = StartContentIndexingForUser(repository, scheduler)

    @Test
    fun `puts a worker behind the account it just started`() = runTest {
        // Given - turning the toggle on is very often followed by leaving the app, and without a
        // foreground service the process is free to die on the way out.
        coEvery { repository.startIndexingForUser(userId) } returns Unit.right()

        // When
        val result = startContentIndexingForUser(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { repository.startIndexingForUser(userId) }
        coVerify(exactly = 1) { scheduler.ensureWorkerRunning() }
    }

    @Test
    fun `does not enqueue a worker for an account the orchestrator refused`() = runTest {
        // Given
        val error = DataError.Local.Unknown
        coEvery { repository.startIndexingForUser(userId) } returns error.left()

        // When
        val result = startContentIndexingForUser(userId)

        // Then
        assertEquals(error.left(), result)
        coVerify(exactly = 0) { scheduler.ensureWorkerRunning() }
    }
}
