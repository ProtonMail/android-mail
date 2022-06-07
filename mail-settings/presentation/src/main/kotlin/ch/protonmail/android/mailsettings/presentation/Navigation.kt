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

package ch.protonmail.android.mailsettings.presentation

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingScreen
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsScreen

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change the app language.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 * @param route the route this composable will be added at
 */
fun NavGraphBuilder.addLanguageSettings(navController: NavHostController, route: String) =
    composable(
        route = route
    ) {
        LanguageSettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change conversation mode.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 * @param route the route this composable will be added at
 */
fun NavGraphBuilder.addConversationModeSettings(navController: NavHostController, route: String) =
    composable(route = route) {
        ConversationModeSettingScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change theme.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 * @param route the route this composable will be added at
 */
fun NavGraphBuilder.addThemeSettings(navController: NavHostController, route: String) = composable(
    route = route
) {
    ThemeSettingsScreen(
        modifier = Modifier,
        onBackClick = { navController.popBackStack() }
    )
}

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change the Swipe Actions.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 * @param route the route this composable will be added at
 */
fun NavGraphBuilder.addSwipeActionsSettings(navController: NavHostController, route: String) =
    composable(
        route = route
    ) {
        SwipeActionsPreferenceScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() },
            onChangeSwipeRightClick = {},
            onChangeSwipeLeftClick = {}
        )
    }
