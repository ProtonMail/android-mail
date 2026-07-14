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
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HasShownContentSearchBottomSheetTest {

    private val repository = mockk<ContentSearchPreferencesRepository>()
    private val hasShownContentSearchBottomSheet = HasShownContentSearchBottomSheet(repository)

    @Test
    fun `returns the value from the repository when shown`() = runTest {
        // Given
        coEvery { repository.hasShownBottomSheet() } returns true.right()

        // When
        val result = hasShownContentSearchBottomSheet()

        // Then
        assertTrue(result)
    }

    @Test
    fun `returns false when the repository fails`() = runTest {
        // Given
        coEvery { repository.hasShownBottomSheet() } returns PreferencesError.left()

        // When
        val result = hasShownContentSearchBottomSheet()

        // Then
        assertFalse(result)
    }
}
