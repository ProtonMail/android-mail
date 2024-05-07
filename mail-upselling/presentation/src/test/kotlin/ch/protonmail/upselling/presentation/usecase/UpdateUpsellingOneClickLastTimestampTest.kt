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

package ch.protonmail.upselling.presentation.usecase

import arrow.core.right
import ch.protonmail.android.mailupselling.domain.repository.UpsellingVisibilityRepository
import ch.protonmail.android.mailupselling.presentation.usecase.UpdateUpsellingOneClickLastTimestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class UpdateUpsellingOneClickLastTimestampTest {

    private val repo = mockk<UpsellingVisibilityRepository> {
        coEvery { this@mockk.update(123L) } returns Unit.right()
    }

    private val updateUpsellingOneClickLastTimestamp = UpdateUpsellingOneClickLastTimestamp(repo)

    @Test
    fun `should pass the value down to the repository`() = runTest {
        // Given
        val value = 123L

        // When
        updateUpsellingOneClickLastTimestamp(value)

        // Then
        coVerify(exactly = 1) { repo.update(value) }
    }
}
