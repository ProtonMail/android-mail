package ch.protonmail.android.mailnotifications.domain.usecase

import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveShouldStopShowingPermissionDialogTest {

    private val notificationPermissionRepository = mockk<NotificationPermissionRepository>(relaxUnitFun = true)

    private val saveShouldStopShowingPermissionDialog = SaveShouldStopShowingPermissionDialog(
        notificationPermissionRepository
    )

    @Test
    fun `should call repository method to save stop showing permission dialog value`() = runTest {
        // When
        saveShouldStopShowingPermissionDialog()

        // Then
        coVerify {
            notificationPermissionRepository.saveShouldStopShowingPermissionDialog(
                shouldStopShowingPermissionDialog = true
            )
        }
    }
}
