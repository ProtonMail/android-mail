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

import ch.protonmail.android.mailcontentsearch.domain.model.EnqueueIndexingResult
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ResumeContentIndexingSweepTest {

    private val scheduler = mockk<ContentIndexingScheduler>()
    private val isContentSearchAllowedOnMobileData = mockk<IsContentSearchAllowedOnMobileData> {
        coEvery { this@mockk.invoke() } returns true
    }
    private val resumeContentIndexingSweep = ResumeContentIndexingSweep(scheduler, isContentSearchAllowedOnMobileData)

    @Test
    fun `enqueues a sweep with the keep policy and the mobile-data preference`() = runTest {
        // Given
        coEvery { scheduler.enqueueSweep(allowMobileData = true, replaceExisting = false) } returns
            EnqueueIndexingResult.Scheduled

        // When
        val result = resumeContentIndexingSweep()

        // Then
        assertEquals(EnqueueIndexingResult.Scheduled, result)
        coVerify { scheduler.enqueueSweep(allowMobileData = true, replaceExisting = false) }
    }
}
