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

package ch.protonmail.android.mailpagination.presentation.paging

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EmptyLabelInProgressSignalTest {

    private val emptyLabelInProgressSignal = EmptyLabelInProgressSignal()

    @Test
    fun `should keep the emitted event in the replay cache`() = runTest {
        // Given
        val expectedEmptyLabelId = EmptyLabelId("labelId")

        // When
        emptyLabelInProgressSignal.emitOperationSignal(expectedEmptyLabelId)

        // Then
        assertTrue(emptyLabelInProgressSignal.isEmptyLabelInProgress(expectedEmptyLabelId))
    }

    @Test
    fun `should not have the recently emitted event in the replay cache after a state reset`() = runTest {
        // Given
        val expectedEmptyLabelId = EmptyLabelId("labelId")

        // When
        emptyLabelInProgressSignal.emitOperationSignal(expectedEmptyLabelId)
        emptyLabelInProgressSignal.resetOperationSignal()

        // Then
        assertFalse(emptyLabelInProgressSignal.isEmptyLabelInProgress(expectedEmptyLabelId))
    }

    @Test
    fun `should ignore unrelated label id clear event in the replay cache`() = runTest {
        // Given
        val firstLabelId = EmptyLabelId("labelId")
        val secondLabelId = EmptyLabelId("labelId2")

        // When
        emptyLabelInProgressSignal.emitOperationSignal(firstLabelId)

        // Then
        assertFalse(emptyLabelInProgressSignal.isEmptyLabelInProgress(secondLabelId))
    }
}
