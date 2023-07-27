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

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.NOTIFICATION_PERMISSION
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator.Companion.PermissionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

interface NotificationsPermissionsOrchestrator {

    fun permissionResult(): Flow<PermissionResult>

    fun requestPermissionIfRequired()

    fun register(caller: AppCompatActivity)

    companion object {
        enum class PermissionResult {
            CHECKING,
            GRANTED,
            DENIED,
            SHOW_RATIONALE
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        internal const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS
    }
}

internal class NotificationsPermissionsOrchestratorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationsPermissionsOrchestrator {

    private val permissionResultFlow = MutableStateFlow(PermissionResult.CHECKING)
    private var permissionRequester: ActivityResultLauncher<String>? = null

    override fun permissionResult(): Flow<PermissionResult> = permissionResultFlow

    override fun requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionResultFlow.value = PermissionResult.GRANTED
            return
        }

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            permissionResultFlow.value = PermissionResult.GRANTED
        } else {
            permissionRequester?.launch(NOTIFICATION_PERMISSION)
        }
    }

    override fun register(caller: AppCompatActivity) {
        permissionRequester = caller.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionResultFlow.value = if (granted) {
                PermissionResult.GRANTED
            } else {
                if (caller.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION)) {
                    PermissionResult.SHOW_RATIONALE
                } else {
                    PermissionResult.DENIED
                }
            }
        }
    }
}
