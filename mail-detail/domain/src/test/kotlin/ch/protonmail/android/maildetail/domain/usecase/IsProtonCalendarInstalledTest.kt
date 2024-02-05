package ch.protonmail.android.maildetail.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled.Companion.PROTON_CALENDAR_PACKAGE_NAME
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsProtonCalendarInstalledTest {

    private val mockContext = mockk<Context>()
    private val mockPackageManager = mockk<PackageManager>()

    private val isProtonCalendarInstalled = IsProtonCalendarInstalled(mockContext)

    @Test
    fun `invoke should return true if proton calendar is installed`() {
        // Given
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getLaunchIntentForPackage(PROTON_CALENDAR_PACKAGE_NAME) } returns mockk {
            every { action } returns "android.intent.action.MAIN"
        }

        // When
        val result = isProtonCalendarInstalled()

        // Then
        assertTrue(result)
    }

    @Test
    fun `invoke should return false if proton calendar is not installed`() {
        // Given
        every { mockContext.packageManager } returns mockPackageManager
        every { mockPackageManager.getLaunchIntentForPackage(PROTON_CALENDAR_PACKAGE_NAME) } returns null

        // When
        val result = isProtonCalendarInstalled()

        // Then
        assertFalse(result)
    }

}
