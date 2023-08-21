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

package ch.protonmail.android.mailnotifications.permissions

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

internal class NotificationsPermissionsOrchestratorImpl @Inject constructor(
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy
) : NotificationsPermissionsOrchestrator {

    private val permissionResultFlow =
        MutableStateFlow(NotificationsPermissionsOrchestrator.Companion.PermissionResult.CHECKING)
    private var permissionRequester: ActivityResultLauncher<String>? = null

    override fun permissionResult(): Flow<NotificationsPermissionsOrchestrator.Companion.PermissionResult> =
        permissionResultFlow

    override fun requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionResultFlow.value = NotificationsPermissionsOrchestrator.Companion.PermissionResult.GRANTED
            return
        }

        if (notificationManagerCompatProxy.areNotificationsEnabled()) {
            permissionResultFlow.value = NotificationsPermissionsOrchestrator.Companion.PermissionResult.GRANTED
        } else {
            permissionRequester?.launch(NotificationsPermissionsOrchestrator.NOTIFICATION_PERMISSION)
        }
    }

    override fun register(caller: AppCompatActivity) {
        permissionRequester = caller.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionResultFlow.value = if (granted) {
                NotificationsPermissionsOrchestrator.Companion.PermissionResult.GRANTED
            } else {
                if (caller.shouldShowRequestPermissionRationale(
                        NotificationsPermissionsOrchestrator.NOTIFICATION_PERMISSION
                    )
                ) {
                    NotificationsPermissionsOrchestrator.Companion.PermissionResult.SHOW_RATIONALE
                } else {
                    NotificationsPermissionsOrchestrator.Companion.PermissionResult.DENIED
                }
            }
        }
    }
}
