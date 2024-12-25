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

package ch.protonmail.android.mailnotifications.presentation

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ch.protonmail.android.mailnotifications.permissions.NotificationsPermissionsOrchestrator
import javax.inject.Inject

class NewNotificationPermissionOrchestrator @Inject constructor() {

    private var permissionRequester: ActivityResultLauncher<String>? = null

    fun requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        permissionRequester?.launch(NotificationsPermissionsOrchestrator.NOTIFICATION_PERMISSION)
    }

    fun register(caller: AppCompatActivity) {
        permissionRequester = caller.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
    }
}
