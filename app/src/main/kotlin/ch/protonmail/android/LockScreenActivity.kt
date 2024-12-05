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

import java.util.concurrent.Executors
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.BiometricPromptCallback
import ch.protonmail.android.navigation.listener.withDestinationChangedObservableEffect
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.route.addAutoLockPinScreen
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.compose.withSentryObservableEffect
import me.proton.core.compose.theme.ProtonTheme

@AndroidEntryPoint
internal class LockScreenActivity : AppCompatActivity() {

    private var biometricPrompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtonTheme {
                val navController = rememberNavController()
                    .withSentryObservableEffect()
                    .withDestinationChangedObservableEffect()

                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    startDestination = Destination.Screen.AutoLockPinScreen.route
                ) {
                    addAutoLockPinScreen(
                        onShowSuccessSnackbar = {},
                        onBack = { this@LockScreenActivity.finish() },
                        activityActions = Actions(
                            finishActivity = { finish() },
                            showBiometricPrompt = { callback ->
                                showBiometricPrompt(callback)
                            }
                        )
                    )
                }

                navController.navigate(
                    Destination.Screen.AutoLockPinScreen(AutoLockInsertionMode.VerifyPin)
                ) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
        }
    }

    private fun showBiometricPrompt(callback: BiometricPromptCallback) {
        val executor = Executors.newSingleThreadExecutor()
        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onAuthenticationError()

                    if (!this@LockScreenActivity.isFinishing &&
                        lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
                    ) {
                        biometricPrompt?.cancelAuthentication()
                        biometricPrompt = null
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    callback.onAuthenticationSucceeded()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_locked))
            .setDescription(getString(R.string.log_in_using_biometric_credential))
            .setNegativeButtonText(getString(R.string.use_pin_instead))
            .build()
        biometricPrompt?.authenticate(promptInfo)
    }


    data class Actions(
        val finishActivity: () -> Unit,
        val showBiometricPrompt: (BiometricPromptCallback) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                finishActivity = {},
                showBiometricPrompt = {}
            )
        }
    }
}
