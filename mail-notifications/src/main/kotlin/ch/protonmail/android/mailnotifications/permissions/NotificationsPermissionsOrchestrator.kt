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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.Flow

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
