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

import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StartContentIndexingTest {

    private val userId = UserId("user-1")
    private val scheduler = mockk<ContentIndexingScheduler>()
    private val settingsRepository = mockk<ContentSearchSettingsRepository>()
    private val getAllowContentSearchOnMobileData = mockk<GetAllowContentSearchOnMobileData> {
        coEvery { this@mockk.invoke() } returns false
    }
    private val startContentIndexing =
        StartContentIndexing(scheduler, settingsRepository, getAllowContentSearchOnMobileData)

    @Test
    fun `returns AlreadySynced and does not enqueue when rust reports Completed`() = runTest {
        coEvery { settingsRepository.getIndexingStatus(userId) } returns ContentIndexingState.Completed

        val result = startContentIndexing(userId)

        assertEquals(EnqueueIndexingResult.AlreadySynced, result)
        coVerify(exactly = 0) { scheduler.enqueue(any(), any()) }
    }

    @Test
    fun `enqueues the worker when rust reports a non-terminal status`() = runTest {
        coEvery { settingsRepository.getIndexingStatus(userId) } returns ContentIndexingState.Idle
        coEvery { scheduler.enqueue(userId, false) } returns EnqueueIndexingResult.Scheduled

        val result = startContentIndexing(userId)

        assertEquals(EnqueueIndexingResult.Scheduled, result)
        coVerify(exactly = 1) { scheduler.enqueue(userId, false) }
    }
}
