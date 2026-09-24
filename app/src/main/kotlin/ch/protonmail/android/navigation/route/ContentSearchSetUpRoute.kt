/*
 * Copyright (c) 2026 Proton Technologies AG
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ch.protonmail.android.mailcontentsearch.presentation.ui.DECLINE_CONTENT_SEARCH_KEY
import ch.protonmail.android.mailcontentsearch.presentation.ui.TURN_ON_CONTENT_SEARCH_KEY
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailspotlight.presentation.ui.HowContentSearchWorksScreen
import ch.protonmail.android.navigation.model.Destination

/**
 * "How content search works" as a standalone page, reached from the search screen's no-results state.
 *
 * "Turn on" leaves the page and asks the screen underneath for the content search sheet, so the
 * sheet comes up over the search results rather than over this page.
 * "Not now" leaves a note for the screen underneath too, so it can stop showing its set-up banner.
 */
internal fun NavGraphBuilder.addContentSearchSetUp(navController: NavHostController) {
    composable(Destination.Screen.ContentSearchSetUp.route) {
        HowContentSearchWorksScreen(
            onTurnOn = {
                navController.previousBackStackEntry?.savedStateHandle?.set(TURN_ON_CONTENT_SEARCH_KEY, true)
                navController.navigateBack()
            },
            onNotNow = {
                navController.previousBackStackEntry?.savedStateHandle?.set(DECLINE_CONTENT_SEARCH_KEY, true)
                navController.navigateBack()
            }
        )
    }
}
