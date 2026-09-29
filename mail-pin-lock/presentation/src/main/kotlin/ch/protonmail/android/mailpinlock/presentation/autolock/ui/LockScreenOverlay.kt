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

package ch.protonmail.android.mailpinlock.presentation.autolock.ui

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailpinlock.presentation.R
import ch.protonmail.android.mailpinlock.presentation.autolock.model.AutoLockOverlayState
import ch.protonmail.android.mailpinlock.presentation.autolock.viewmodel.LockScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun LockScreenOverlay(
    onClose: () -> Unit,
    onNavigateToPinInsertion: () -> Unit,
    viewModel: LockScreenViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    when (state) {
        AutoLockOverlayState.Biometrics ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ProtonTheme.colors.backgroundNorm)
            ) {
                LockScreenBiometricsPrompt(
                    onClose = { viewModel.onSuccessfulBiometrics() },
                    onCloseAll = { activity?.finishAffinity() }
                )
            }

        AutoLockOverlayState.Error -> onClose()
        AutoLockOverlayState.Loading -> Unit
        AutoLockOverlayState.Pin -> onNavigateToPinInsertion()
    }
}

@Composable
private fun LockScreenBiometricsPrompt(onClose: () -> Unit, onCloseAll: () -> Unit) {
    val biometricAuthenticator = rememberBiometricAuthenticator(
        title = stringResource(R.string.mail_settings_biometrics_title_app_locked),
        subtitle = stringResource(R.string.mail_settings_biometrics_subtitle_app_locked),
        negativeButtonText = stringResource(R.string.mail_settings_biometrics_button_negative)
    )

    var authenticationTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authenticationTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(authenticationTrigger) {
        biometricAuthenticator.authenticate(
            onAuthenticationError = { errorCode, _ ->
                Timber.d("auto-lock: Biometric authentication error, code=$errorCode")
                when (errorCode) {
                    // Tapped outside - re-prompt after short delay
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        scope.launch {
                            @Suppress("MagicNumber")
                            delay(300)
                            authenticationTrigger++
                        }
                    }

                    BiometricPrompt.ERROR_CANCELED -> Unit
                    else -> onCloseAll()
                }
            },
            onAuthenticationFailed = {},
            onAuthenticationSucceeded = { onClose() }
        )
    }
}
