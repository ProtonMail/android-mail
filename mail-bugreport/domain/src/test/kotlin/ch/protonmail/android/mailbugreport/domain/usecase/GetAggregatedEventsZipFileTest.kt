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

package ch.protonmail.android.mailbugreport.domain.usecase

import java.io.File
import android.content.Context
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailbugreport.domain.provider.LogcatProvider
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import javax.inject.Provider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAggregatedEventsZipFileTest {

    private val context: Context = mockk(relaxed = true)
    private val logcatProvider = mockk<LogcatProvider>()
    private val logsFileHandler = mockk<LogsFileHandler>()
    private val logsExportFeatureSetting = mockk<Provider<LogsExportFeatureSetting>> {
        every { this@mockk.get() } returns DefaultExportSettings
    }
    private val getAggregatedEventsZipFile: GetAggregatedEventsZipFile
        get() = GetAggregatedEventsZipFile(context, logcatProvider, logsFileHandler, logsExportFeatureSetting.get())

    @Before
    fun setup() {
        unmockkAll()
        val tempCacheDir = File(System.getProperty("java.io.tmpdir"), "test-cache")
            .apply { mkdirs() }
        every { context.cacheDir } returns tempCacheDir
    }

    @Test
    fun `invoke creates zip file with correct name`() = runTest {
        // Arrange
        val mockLogcatDir = mockk<File>()
        val mockLogsDir = mockk<File>()

        coEvery { logcatProvider.getLogcatFile() } returns mockk()
        every { logcatProvider.getParentPath() } returns mockLogcatDir
        every { logsFileHandler.getParentPath() } returns mockLogsDir
        every { mockLogcatDir.isDirectory } returns true
        every { mockLogsDir.isDirectory } returns true
        every { mockLogcatDir.listFiles() } returns emptyArray()
        every { mockLogsDir.listFiles() } returns emptyArray()

        // Act
        val result = getAggregatedEventsZipFile()

        // Assert
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { zipFile ->
            assertEquals("protonmail_events.zip", zipFile.name)
            assertTrue(zipFile.exists())
        }
    }

    @Test
    fun `invoke skips logcat when the internal feature flag is disabled`() = runTest {
        // Arrange
        val mockLogsDir = mockk<File>()

        every { logsExportFeatureSetting.get() } returns
            LogsExportFeatureSetting(enabled = true, internalEnabled = false)

        every { logsFileHandler.getParentPath() } returns mockLogsDir
        every { mockLogsDir.isDirectory } returns true
        every { mockLogsDir.listFiles() } returns emptyArray()

        // Act
        val result = getAggregatedEventsZipFile()

        // Assert
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { zipFile ->
            assertEquals("protonmail_events.zip", zipFile.name)
            assertTrue(zipFile.exists())
        }
        verify { logcatProvider wasNot called }
    }

    @Test
    fun `invoke fails when logcat directory does not exist`() = runTest {
        // Arrange
        val mockLogcatDir = mockk<File>()
        val mockLogsDir = mockk<File>()

        every { logcatProvider.getParentPath() } returns mockLogcatDir
        every { logsFileHandler.getParentPath() } returns mockLogsDir
        every { mockLogcatDir.exists() } returns false

        // Act
        val result = getAggregatedEventsZipFile()

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `invoke fails when events directory does not exist`() = runTest {
        // Arrange
        val mockLogcatDir = mockk<File>()
        val mockLogsDir = mockk<File>()

        every { logcatProvider.getParentPath() } returns mockLogcatDir
        every { logsFileHandler.getParentPath() } returns mockLogsDir
        every { mockLogcatDir.exists() } returns true
        every { mockLogsDir.exists() } returns false

        // Act
        val result = getAggregatedEventsZipFile()

        // Assert
        assertTrue(result.isFailure)
    }

    private companion object {

        val DefaultExportSettings = LogsExportFeatureSetting(enabled = true, internalEnabled = true)
    }
}

