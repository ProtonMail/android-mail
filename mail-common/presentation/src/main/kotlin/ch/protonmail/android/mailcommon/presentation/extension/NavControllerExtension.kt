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

package ch.protonmail.android.mailcommon.presentation.extension

import androidx.navigation.NavController
import timber.log.Timber

/**
 * Navigates back by popping the backstack only if the current backstack entry is not the starting destination.
 * This avoids navigating back to a blank screen if the user taps back/exit too quickly.
 */
fun NavController.navigateBack() {
    val startDestination = graph.startDestinationId

    if (currentDestination?.id != startDestination) {
        Timber.tag("NavController").d("Navigating back from: ${currentDestination?.route}")
        popBackStack()
    } else {
        Timber.tag("NavController").d("Back navigation ignored, current location: ${currentDestination?.route}")
    }
}
