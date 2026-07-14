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

import arrow.core.right
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class MarkContentSearchBottomSheetShownTest {

    private val repository = mockk<ContentSearchPreferencesRepository>()
    private val markContentSearchBottomSheetShown = MarkContentSearchBottomSheetShown(repository)

    @Test
    fun `marks the bottom sheet as shown through the repository`() = runTest {
        // Given
        coEvery { repository.markBottomSheetShown() } returns Unit.right()

        // When
        markContentSearchBottomSheetShown()

        // Then
        coVerify { repository.markBottomSheetShown() }
    }
}
