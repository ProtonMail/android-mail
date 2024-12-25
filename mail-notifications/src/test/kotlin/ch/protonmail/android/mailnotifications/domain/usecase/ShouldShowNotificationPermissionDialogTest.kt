package ch.protonmail.android.mailnotifications.domain.usecase

import androidx.core.app.NotificationManagerCompat
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepository
import ch.protonmail.android.mailnotifications.domain.usecase.featureflag.IsNewNotificationPermissionFlowEnabled
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowNotificationPermissionDialogTest {

    private val isNewNotificationPermissionFlowEnabled = mockk<IsNewNotificationPermissionFlowEnabled>()
    private val notificationManager = mockk<NotificationManagerCompat>()
    private val notificationPermissionRepository = mockk<NotificationPermissionRepository>()

    private val shouldShowNotificationPermissionDialog = ShouldShowNotificationPermissionDialog(
        isNewNotificationPermissionFlowEnabled,
        notificationManager,
        notificationPermissionRepository
    )

    @Test
    fun `should return true when the FF is ON, notifications are not enabled, timestamp is not saved`() = runTest {
        // Given
        every { isNewNotificationPermissionFlowEnabled(null) } returns true
        every { notificationManager.areNotificationsEnabled() } returns false
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = shouldShowNotificationPermissionDialog()

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when the FF is OFF, notifications are not enabled, timestamp is not saved`() = runTest {
        // Given
        every { isNewNotificationPermissionFlowEnabled(null) } returns false
        every { notificationManager.areNotificationsEnabled() } returns false
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = shouldShowNotificationPermissionDialog()

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when the FF is ON, notifications are enabled, timestamp is not saved`() = runTest {
        // Given
        every { isNewNotificationPermissionFlowEnabled(null) } returns true
        every { notificationManager.areNotificationsEnabled() } returns true
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = shouldShowNotificationPermissionDialog()

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return false when the FF is ON, notifications are not enabled, timestamp is saved`() = runTest {
        // Given
        every { isNewNotificationPermissionFlowEnabled(null) } returns true
        every { notificationManager.areNotificationsEnabled() } returns false
        coEvery { notificationPermissionRepository.getNotificationPermissionTimestamp() } returns 123L.right()

        // When
        val actual = shouldShowNotificationPermissionDialog()

        // Then
        assertFalse(actual)
    }
}
