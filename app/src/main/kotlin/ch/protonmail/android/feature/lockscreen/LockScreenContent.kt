/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.feature.lockscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailpinlock.presentation.autolock.model.AutoLockInsertionMode
import ch.protonmail.android.mailpinlock.presentation.autolock.standalone.LocalLockScreenEntryPointIsStandalone
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.route.addAutoLockOverlay
import ch.protonmail.android.navigation.route.addAutoLockPinScreen
import io.sentry.compose.withSentryObservableEffect

@Composable
internal fun LockScreenContent(onClose: () -> Unit) {
    ProtonTheme {
        val navController = rememberNavController().withSentryObservableEffect()
        val backgroundColor = ProtonTheme.colors.backgroundNorm

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            CompositionLocalProvider(LocalLockScreenEntryPointIsStandalone provides true) {
                NavHost(
                    navController = navController,
                    startDestination = Destination.Screen.AutoLockOverlay.route
                ) {
                    addAutoLockOverlay(
                        onClose = onClose,
                        onNavigateToPinLock = {
                            navController.navigate(
                                Destination.Screen.AutoLockPinScreen(AutoLockInsertionMode.VerifyPin)
                            )
                        }
                    )

                    addAutoLockPinScreen(
                        onClose = onClose,
                        onShowSuccessSnackbar = {}
                    )
                }
            }
        }
    }
}
