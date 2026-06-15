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

import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class CancelContentIndexingTest {

    private val userId = UserId("user-1")
    private val scheduler = mockk<ContentIndexingScheduler>()
    private val cancelContentIndexing = CancelContentIndexing(scheduler)

    @Test
    fun `cancels the scheduled indexing for the given user`() {
        // Given
        every { scheduler.cancel(userId) } just runs

        // When
        cancelContentIndexing(userId)

        // Then
        verify { scheduler.cancel(userId) }
    }
}
