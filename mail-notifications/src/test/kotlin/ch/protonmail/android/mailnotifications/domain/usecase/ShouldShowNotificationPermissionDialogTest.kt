package ch.protonmail.android.mailnotifications.domain.usecase

import androidx.core.app.NotificationManagerCompat
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowNotificationPermissionDialogTest {

    private val notificationManager = mockk<NotificationManagerCompat>()
    private val notificationPermissionRepository = mockk<NotificationPermissionRepository>()

    private val shouldShowNotificationPermissionDialog = ShouldShowNotificationPermissionDialog(
        notificationManager,
        notificationPermissionRepository
    )

    @Test
    fun `should return true when notifications are not enabled, timestamp is not saved`() = runTest {
        // Given
        every { notificationManager.areNotificationsEnabled() } returns false
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = shouldShowNotificationPermissionDialog(
            currentTimeMillis = 0,
            isMessageSent = false
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `should return false when notifications are enabled, timestamp is not saved`() = runTest {
        // Given
        every { notificationManager.areNotificationsEnabled() } returns true
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = shouldShowNotificationPermissionDialog(
            currentTimeMillis = 0,
            isMessageSent = false
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should return true when timestamp is saved, 20 days passed since first show and a message was sent`() =
        runTest {
            // Given
            every { notificationManager.areNotificationsEnabled() } returns false
            coEvery {
                notificationPermissionRepository.getNotificationPermissionTimestamp()
            } returns 1_737_123_238_000L.right()
            coEvery { notificationPermissionRepository.getShouldStopShowingPermissionDialog() } returns false.right()

            // When
            val actual = shouldShowNotificationPermissionDialog(
                currentTimeMillis = 1_739_801_638_000,
                isMessageSent = true
            )

            // Then
            assertTrue(actual)
        }

    @Test
    fun `should return false when 20 days passed since first show, a message was sent, stop showing value is true`() =
        runTest {
            // Given
            every { notificationManager.areNotificationsEnabled() } returns false
            coEvery {
                notificationPermissionRepository.getNotificationPermissionTimestamp()
            } returns 1_737_123_238_000L.right()
            coEvery { notificationPermissionRepository.getShouldStopShowingPermissionDialog() } returns true.right()

            // When
            val actual = shouldShowNotificationPermissionDialog(
                currentTimeMillis = 1_739_801_638_000,
                isMessageSent = true
            )

            // Then
            assertFalse(actual)
        }

    @Test
    fun `should return false when 20 days passed since first show and a message was not sent`() = runTest {
        // Given
        every { notificationManager.areNotificationsEnabled() } returns false
        coEvery {
            notificationPermissionRepository.getNotificationPermissionTimestamp()
        } returns 1_737_123_238_000L.right()
        coEvery { notificationPermissionRepository.getShouldStopShowingPermissionDialog() } returns false.right()

        // When
        val actual = shouldShowNotificationPermissionDialog(
            currentTimeMillis = 1_739_801_638_000,
            isMessageSent = false
        )

        // Then
        assertFalse(actual)
    }
}
