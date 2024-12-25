package ch.protonmail.android.mailnotifications.domain.usecase

import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SavePermissionDialogTimestampTest {

    private val notificationPermissionRepository = mockk<NotificationPermissionRepository>(relaxUnitFun = true)

    private val savePermissionDialogTimestamp = SavePermissionDialogTimestamp(notificationPermissionRepository)

    @Test
    fun `should call repository method to save the notification permission dialog timestamp`() = runTest {
        // Given
        val timestamp = 123L

        // When
        savePermissionDialogTimestamp(timestamp)

        // Then
        coVerify { notificationPermissionRepository.saveNotificationPermissionTimestamp(timestamp) }
    }
}
