package ch.protonmail.android.mailbugreport.data

import java.io.File
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class LogsFileHandlerImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var logsFileHandler: LogsFileHandlerImpl
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var cacheDir: File
    private lateinit var logDir: File

    @BeforeTest
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        cacheDir = tempFolder.newFolder()
        logDir = File(cacheDir, "logs").apply { mkdirs() }

        context = mockk {
            every { cacheDir } returns this@LogsFileHandlerImplTest.cacheDir
        }

        logsFileHandler = LogsFileHandlerImpl(
            context = context,
            coroutineDispatcher = testDispatcher
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getParentPath returns correct directory`() {
        // When
        val parentPath = logsFileHandler.getParentPath()

        // Then
        assertEquals("logs", parentPath.name)
        assertTrue(parentPath.exists())
        assertTrue(parentPath.isDirectory)
    }

    @Test
    fun `writeLog creates new file when no files exist`() = runTest(testDispatcher) {
        // When
        logsFileHandler.writeLog("Test message")
        advanceUntilIdle()

        val files = logDir.listFiles()

        // Then
        assertNotNull(files)
        assertEquals(1, files.size)
        assertTrue(files[0].name.startsWith("log-"))
        assertTrue(files[0].readText().contains("Test message"))
    }

    @Test
    fun `calling close properly closes file writer and cancels coroutine scope`() = runTest(testDispatcher) {
        // When
        logsFileHandler.writeLog("Test message")
        advanceUntilIdle()

        logsFileHandler.close()

        // Write after close, won't create new content
        logsFileHandler.writeLog("After close")
        advanceUntilIdle()

        val files = logDir.listFiles()

        // Then
        assertNotNull(files)
        assertEquals(1, files.size)
        assertTrue(files[0].readText().contains("Test message"))
        assertFalse(files[0].readText().contains("After close"))
    }
}
