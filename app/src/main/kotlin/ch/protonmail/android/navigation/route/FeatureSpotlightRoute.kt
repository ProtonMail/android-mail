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

package ch.protonmail.android.navigation.route

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.protonmail.android.mailspotlight.presentation.ui.FeatureSpotlightScreen
import ch.protonmail.android.navigation.model.Destination

/** [onShown] runs on the screen's first composition: the What's New is on screen, before any tap. */
internal fun NavGraphBuilder.addFeatureSpotlight(
    onShown: () -> Unit,
    onBack: () -> Unit,
    onTurnOn: () -> Unit
) {
    composable(Destination.Screen.FeatureSpotlight.route) {
        LaunchedEffect(Unit) { onShown() }
        FeatureSpotlightScreen(onDismiss = onBack, onTurnOn = onTurnOn)
    }
}
