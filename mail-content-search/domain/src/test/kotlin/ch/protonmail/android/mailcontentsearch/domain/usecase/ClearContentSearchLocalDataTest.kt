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
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ClearContentSearchLocalDataTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRepository>()
    private val clearContentSearchLocalData = ClearContentSearchLocalData(repository)

    @Test
    fun `delegates clearing the local data to the repository`() = runTest {
        // Given
        coEvery { repository.clearLocalData(userId) } returns Unit.right()

        // When
        val result = clearContentSearchLocalData(userId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { repository.clearLocalData(userId) }
    }

    @Test
    fun `returns the error when the repository fails to clear the local data`() = runTest {
        // Given
        val error = DataError.Local.Unknown
        coEvery { repository.clearLocalData(userId) } returns error.left()

        // When
        val result = clearContentSearchLocalData(userId)

        // Then
        assertEquals(error.left(), result)
    }
}
