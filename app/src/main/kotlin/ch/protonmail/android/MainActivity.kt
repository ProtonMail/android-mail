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

package ch.protonmail.android

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.navigation.Launcher
import ch.protonmail.android.navigation.LauncherViewModel
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.navigation.share.ShareIntentObserver
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.notification.presentation.deeplink.DeeplinkManager
import me.proton.core.notification.presentation.deeplink.onActivityCreate
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    @Inject
    lateinit var deviceCapabilities: DeviceCapabilities

    @Inject
    lateinit var shareIntentObserver: ShareIntentObserver

    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            launcherViewModel.state.value == LauncherState.Processing
        }
        super.onCreate(savedInstanceState)
        deeplinkManager.onActivityCreate(this, savedInstanceState)

        // Register activities for result.
        launcherViewModel.register(this)

        shareIntentObserver.onNewIntent(intent)

        setContent {
            ProtonTheme {
                CompositionLocalProvider(
                    LocalDeviceCapabilitiesProvider provides deviceCapabilities.getCapabilities()
                ) {
                    Launcher(
                        Actions(
                            openInActivityInNewTask = { openInActivityInNewTask(it) },
                            openIntentChooser = { openIntentChooser(it) }
                        ),
                        launcherViewModel
                    )
                }
            }
        }
    }

    private fun openInActivityInNewTask(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.d(e, "Failed to open a browser app")

            Toast.makeText(
                this,
                getString(R.string.intent_failure_no_app_found_to_handle_this_action),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openIntentChooser(intentValues: OpenAttachmentIntentValues) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(intentValues.uri, intentValues.mimeType)
            .putExtra(Intent.EXTRA_STREAM, intentValues.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.d(e, "Failed to open intent for file type")
            startActivity(Intent.createChooser(intent, null))
        }
    }

    data class Actions(
        val openInActivityInNewTask: (uri: Uri) -> Unit,
        val openIntentChooser: (values: OpenAttachmentIntentValues) -> Unit
    )
}
