/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailbyoe.domain.model.ByoeImportError
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportState
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportStatus
import ch.protonmail.android.mailbyoe.domain.repository.ByoeImportRepository
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ObserveByoeImportPausedForStorageTest {

    private val byoeImportRepository = mockk<ByoeImportRepository>()
    private val isByoeMobileKillswitchOn = mockk<FeatureFlag<Boolean>> {
        coEvery { get() } returns false
    }
    private val observeByoeImportPausedForStorage =
        ObserveByoeImportPausedForStorage(byoeImportRepository, isByoeMobileKillswitchOn)

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an import paused for storage, when observing, then it is paused`() = runTest {
        // Given
        every { byoeImportRepository.observeImportStatuses(userId) } returns
            flowOf(listOf(status(ByoeImportState.Paused, ByoeImportError.StorageLimit)))

        // When
        val result = observeByoeImportPausedForStorage(userId).first()

        // Then
        assertTrue(result)
    }

    @Test
    fun `given an import paused for its connection, when observing, then it is not paused for storage`() = runTest {
        // Given
        every { byoeImportRepository.observeImportStatuses(userId) } returns
            flowOf(listOf(status(ByoeImportState.Paused, ByoeImportError.Connection)))

        // When
        val result = observeByoeImportPausedForStorage(userId).first()

        // Then
        assertFalse(result)
    }

    @Test
    fun `given the import resumes after an upgrade, when observing, then it stops being paused`() = runTest {
        // Given
        every { byoeImportRepository.observeImportStatuses(userId) } returns flowOf(
            listOf(status(ByoeImportState.Paused, ByoeImportError.StorageLimit)),
            listOf(status(ByoeImportState.Running, error = null))
        )

        // When
        val result = observeByoeImportPausedForStorage(userId).toList()

        // Then
        assertEquals(listOf(true, false), result)
    }

    @Test
    fun `given BYOE is killed on mobile, when observing, then it is not paused`() = runTest {
        // Given
        coEvery { isByoeMobileKillswitchOn.get() } returns true

        // When
        val result = observeByoeImportPausedForStorage(userId).first()

        // Then
        assertFalse(result)
        verify(exactly = 0) { byoeImportRepository.observeImportStatuses(any()) }
    }

    private fun status(state: ByoeImportState, error: ByoeImportError?) = ByoeImportStatus(
        addressId = "address",
        state = state,
        error = error,
        processed = 10,
        total = 100
    )

    private companion object {

        val userId = UserId("user")
    }
}
