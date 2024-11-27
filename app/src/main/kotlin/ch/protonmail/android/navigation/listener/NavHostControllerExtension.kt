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

package ch.protonmail.android.navigation.listener

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavHostController
import timber.log.Timber

@Composable
@NonRestartableComposable
fun NavHostController.withDestinationChangedObservableEffect(): NavHostController {

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, this) {
        val observer = NavigationLifeCycleObserver(
            this@withDestinationChangedObservableEffect,
            navListener = { _, destination, _ ->
                Timber.tag("NavController").d("Navigating to ${destination.route}")
            }
        )

        lifecycle.addObserver(observer)

        onDispose {
            observer.dispose()
            lifecycle.removeObserver(observer)
        }
    }
    return this
}
