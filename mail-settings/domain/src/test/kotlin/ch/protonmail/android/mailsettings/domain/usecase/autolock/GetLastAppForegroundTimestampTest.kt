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

package ch.protonmail.android.mailsettings.domain.usecase.autolock

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class GetLastAppForegroundTimestampTest {

    private val autoLockRepository = mockk<AutoLockRepository>()
    private val getLastForegroundTimestamp = GetLastAppForegroundTimestamp(autoLockRepository)

    @Test
    fun `should return time zero when no value can be fetched`() = runTest {
        // Given
        val expectedItem = AutoLockLastForegroundMillis(0L)
        coEvery {
            autoLockRepository.observeAutoLockLastForegroundMillis()
        } returns flowOf(AutoLockPreferenceError.DataStoreError.left())

        // When
        val actual = getLastForegroundTimestamp()

        // Then
        assertEquals(expectedItem, actual)
    }

    @Test
    fun `should return the correct value when it is fetched with success`() = runTest {
        // Given
        val expectedItem = AutoLockLastForegroundMillis(1200L)
        coEvery {
            autoLockRepository.observeAutoLockLastForegroundMillis()
        } returns flowOf(expectedItem.right())

        // When
        val actual = getLastForegroundTimestamp()

        // Then
        assertEquals(expectedItem, actual)
    }
}
