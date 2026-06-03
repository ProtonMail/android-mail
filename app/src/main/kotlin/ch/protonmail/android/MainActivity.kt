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
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.feature.lockscreen.LockScreenActivity
import ch.protonmail.android.mailcommon.data.file.IntentExtraKeys
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities
import ch.protonmail.android.mailcommon.presentation.system.LocalDeviceCapabilitiesProvider
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.presentation.util.ProtonCalendarUtil
import ch.protonmail.android.navigation.Launcher
import ch.protonmail.android.navigation.LauncherViewModel
import ch.protonmail.android.navigation.model.LauncherState
import ch.protonmail.android.navigation.share.NewIntentObserver
import coil.Coil
import coil.ImageLoader
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.proton.android.core.payment.domain.IconResourceManager
import me.proton.android.core.payment.presentation.IconResourceFetcher
import me.proton.android.core.payment.presentation.IconResourceKeyer
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var deviceCapabilities: DeviceCapabilities

    @Inject
    lateinit var newIntentObserver: NewIntentObserver

    @Inject
    lateinit var iconResourceManager: IconResourceManager

    @Inject
    lateinit var reviewManager: ReviewManager

    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            launcherViewModel.state.value == LauncherState.Processing
        }
        // Enable edge to edge (all API levels)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Register activities for result.
        launcherViewModel.register(this)

        // Check if this is a share intent on initial creation
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        disableRecentAppsScreenshotPreview()

        configureCoil()

        setContent {
            ProtonTheme {
                CompositionLocalProvider(
                    LocalDeviceCapabilitiesProvider provides deviceCapabilities.getCapabilities()
                ) {
                    Launcher(
                        Actions(
                            openInActivityInNewTask = { openInActivityInNewTask(it) },
                            openProtonCalendarIntentValues = { handleProtonCalendarRequest(it) },
                            openSecurityKeys = {
                                launcherViewModel.submit(LauncherViewModel.Action.OpenSecurityKeys)
                            },
                            openSubscription = {
                                launcherViewModel.submit(LauncherViewModel.Action.OpenSubscription)
                            },
                            finishActivity = { finishAndRemoveTask() },
                            openPasswordManagement = {
                                launcherViewModel.submit(LauncherViewModel.Action.OpenPasswordManagement(it))
                            },
                            onNavigateToLockScreen = {
                                window.decorView.post { // Ensure that activity launch happens in the correct stage.
                                    val intent = Intent(this@MainActivity, LockScreenActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    startActivity(intent)
                                }
                            },
                            launchRatingBooster = {
                                handleLaunchInAppReview()
                            }
                        ),
                        launcherViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun configureCoil() {
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .components {
                    add(IconResourceKeyer())
                    add(IconResourceFetcher.Factory(applicationContext, iconResourceManager))
                }
                .crossfade(true)
                .build()
        )
    }

    private fun handleIncomingIntent(intent: Intent) {
        val callingPackage = referrer?.host
        val isExternalShare = callingPackage != packageName

        lifecycleScope.launch {
            Timber.d("Handling intent with action: ${intent.action}")
            intent.putExtra(IntentExtraKeys.EXTRA_EXTERNAL_SHARE, isExternalShare)
            newIntentObserver.onNewIntent(intent)
        }
    }

    private fun disableRecentAppsScreenshotPreview() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(BuildConfig.DEBUG)
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


    private fun handleProtonCalendarRequest(values: OpenProtonCalendarIntentValues) {
        val intent = when (values) {
            is OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar ->
                ProtonCalendarUtil.getIntentToOpenIcsInProtonCalendar(
                    values.uriToIcsAttachment,
                    values.sender,
                    values.recipient
                )

            is OpenProtonCalendarIntentValues.OpenUriInProtonCalendar -> {
                ProtonCalendarUtil.getIntentToOpenEventInProtonCalendar(
                    values.eventId,
                    values.calendarId,
                    values.recurrenceId
                )
            }

            is OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore ->
                ProtonCalendarUtil.getIntentToProtonCalendarOnPlayStore()
        }
        startActivity(intent)
    }

    private fun handleLaunchInAppReview() {
        val request = reviewManager.requestReviewFlow()
        Timber.d("ReviewFlow - Requesting Review Flow...")
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    Timber.d("ReviewFlow - Rating In-App Review flow completed.")
                }
            } else {
                when (val exception = task.exception) {
                    is ReviewException -> Timber.e("ReviewFlow - In app review error: ${exception.errorCode}")
                    else -> Timber.e("ReviewFlow - Generic error while requesting in app review: $exception")
                }
            }
        }
    }

    data class Actions(
        val openInActivityInNewTask: (uri: Uri) -> Unit,
        val openProtonCalendarIntentValues: (values: OpenProtonCalendarIntentValues) -> Unit,
        val onNavigateToLockScreen: () -> Unit,
        val launchRatingBooster: () -> Unit,
        val openSecurityKeys: () -> Unit,
        val openPasswordManagement: (userId: UserId?) -> Unit,
        val openSubscription: () -> Unit,
        val finishActivity: () -> Unit
    )
}
