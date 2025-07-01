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

package ch.protonmail.android.navigation.route

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.ui.drivespotlight.DriveSpotlightScreen
import ch.protonmail.android.mailupselling.presentation.ui.npsfeedback.NPSFeedbackScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.navigation.model.Destination

fun NavGraphBuilder.addUpsellingRoutes(actions: UpsellingScreen.Actions) {
    composable(route = Destination.Screen.Upselling.StandaloneMailbox.route) {
        UpsellingScreen(
            bottomSheetActions = actions,
            entryPoint = UpsellingEntryPoint.Feature.Mailbox
        )
    }
    composable(route = Destination.Screen.Upselling.StandaloneMailboxPromo.route) {
        UpsellingScreen(
            bottomSheetActions = actions,
            entryPoint = UpsellingEntryPoint.Feature.MailboxPromo
        )
    }
    composable(route = Destination.Screen.Upselling.StandaloneNavbar.route) {
        UpsellingScreen(
            bottomSheetActions = actions,
            entryPoint = UpsellingEntryPoint.Feature.Navbar
        )
    }
}

fun NavGraphBuilder.addDriveSpotlightRoute(actions: DriveSpotlightScreen.Actions) {
    dialog(route = Destination.Screen.DriveSpotlight.route) {
        DriveSpotlightScreen(actions)
    }
}

fun NavGraphBuilder.addNPSFeedbackRoute(actions: NPSFeedbackScreen.Actions) {
    composable(route = Destination.Screen.NPSFeedback.route) {
        NPSFeedbackScreen(actions)
    }
}
