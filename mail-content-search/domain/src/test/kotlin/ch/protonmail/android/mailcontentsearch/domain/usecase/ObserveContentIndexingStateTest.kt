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
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingState
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentIndexingScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ObserveContentIndexingStateTest {

    private val userId = UserId("user-1")
    private val scheduler = mockk<ContentIndexingScheduler>()
    private val observeContentIndexingState = ObserveContentIndexingState(scheduler)

    @Test
    fun `emits the indexing states observed from the scheduler`() = runTest {
        // Given
        every { scheduler.observeState(userId) } returns
            flowOf(ContentIndexingState.Running(50.0), ContentIndexingState.Completed)

        // When + Then
        observeContentIndexingState(userId).test {
            assertEquals(ContentIndexingState.Running(50.0), awaitItem())
            assertEquals(ContentIndexingState.Completed, awaitItem())
            awaitComplete()
        }
    }
}
