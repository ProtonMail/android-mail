package ch.protonmail.android.mailnotifications.data.repository

import ch.protonmail.android.mailnotifications.data.local.NotificationPermissionLocalDataSource
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class NotificationPermissionRepositoryImplTest {

    private val notificationPermissionLocalDataSource = mockk<NotificationPermissionLocalDataSource>(relaxed = true)

    private val notificationPermissionRepository = NotificationPermissionRepositoryImpl(
        notificationPermissionLocalDataSource
    )

    @Test
    fun `should call the local data source method when getting notification permission timestamp`() = runTest {
        // When
        notificationPermissionRepository.getNotificationPermissionTimestamp()

        // Then
        coVerify { notificationPermissionLocalDataSource.getNotificationPermissionTimestamp() }
    }

    @Test
    fun `should call the local data source method when saving notification permission timestamp`() = runTest {
        // Given
        val timestamp = 123L

        // When
        notificationPermissionRepository.saveNotificationPermissionTimestamp(timestamp)

        // Then
        coVerify { notificationPermissionLocalDataSource.saveNotificationPermissionTimestamp(timestamp) }
    }
}
