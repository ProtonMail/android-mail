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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationPermissionOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var rationalePermissionRequester: ActivityResultLauncher<String>? = null
    private var intentPermissionRequester: ActivityResultLauncher<Intent>? = null

    fun requestPermissionIfRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rationalePermissionRequester?.launch(NOTIFICATION_PERMISSION)
        } else {
            navigateToNotificationSettings()
        }
    }

    fun register(caller: AppCompatActivity) {
        rationalePermissionRequester = caller.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // As we can't reliably determine the "don't show again" scenario, we navigate to Settings to make sure
            // the requester is not a no-op.
            if (
                !granted &&
                !caller.shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION)
            ) {
                navigateToNotificationSettings()
            }
        }

        intentPermissionRequester = caller.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}
    }

    fun unregister() {
        rationalePermissionRequester?.unregister()
        intentPermissionRequester?.unregister()

        rationalePermissionRequester = null
        intentPermissionRequester = null
    }

    private fun navigateToNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        intentPermissionRequester?.launch(intent)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private const val NOTIFICATION_PERMISSION = Manifest.permission.POST_NOTIFICATIONS
