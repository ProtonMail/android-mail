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

package ch.protonmail.android.mailbugreport.data

import java.io.File
import java.io.IOException
import ch.protonmail.android.mailbugreport.data.provider.BugReportLogProviderImpl
import ch.protonmail.android.mailbugreport.domain.usecase.GetAggregatedEventsZipFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class BugReportProviderImplTest {

    private val getAggregatedEventsZipFile = mockk<GetAggregatedEventsZipFile>()
    private val bugReportLogProviderImpl = BugReportLogProviderImpl(getAggregatedEventsZipFile)

    @Test
    fun `should proxy the call to getAggregatedEventsZipFile and return the file when successful`() = runTest {
        // Given
        coEvery { getAggregatedEventsZipFile() } returns Result.success(File(""))
        // When
        val result = bugReportLogProviderImpl.getLog()

        // Then
        assertNotNull(result)
        coVerify(exactly = 1) { getAggregatedEventsZipFile() }
    }

    @Test
    fun `should proxy the call to getAggregatedEventsZipFile and return null when unsuccessful`() = runTest {
        // Given
        coEvery { getAggregatedEventsZipFile() } returns Result.failure(IOException())

        // When
        val result = bugReportLogProviderImpl.getLog()

        // Then
        assertNull(result)
        coVerify(exactly = 1) { getAggregatedEventsZipFile() }
    }
}
