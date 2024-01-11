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

package ch.protonmail.android.mailmailbox.data.repository

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailmailbox.data.local.StorageLimitLocalDataSourceImpl
import ch.protonmail.android.mailmailbox.domain.model.StorageLimitPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class StorageLimitRepositoryImplTest {

    private val storageLimitPreference = StorageLimitPreference(
        firstLimitWarningConfirmed = false,
        secondLimitWarningConfirmed = true
    ).right()

    private val storageLimitPreferenceFlow = flowOf(storageLimitPreference)

    private val storageLimitLocalDataSource: StorageLimitLocalDataSourceImpl = mockk {
        every { observe() } returns storageLimitPreferenceFlow
        coEvery { saveFirstLimitWarningPreference(any()) } returns Unit.right()
        coEvery { saveSecondLimitWarningPreference(any()) } returns Unit.right()
    }

    private val storageLimitRepository = StorageLimitRepositoryImpl(storageLimitLocalDataSource)

    @Test
    fun `returns storage preferences from the local data source`() = runTest {
        // When
        storageLimitRepository.observe().test {

            // Then
            Assert.assertEquals(storageLimitPreference, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `calls the local data source save method with the correct preference`() = runTest {
        // Given
        val firstLimitWarningConfirmed = true
        val secondLimitWarningConfirmed = false

        // When
        storageLimitRepository.saveFirstLimitWarningPreference(firstLimitWarningConfirmed)
        storageLimitRepository.saveSecondLimitWarningPreference(secondLimitWarningConfirmed)

        // Then
        coVerify { storageLimitRepository.saveFirstLimitWarningPreference(firstLimitWarningConfirmed) }
        coVerify { storageLimitRepository.saveSecondLimitWarningPreference(secondLimitWarningConfirmed) }
    }
}
