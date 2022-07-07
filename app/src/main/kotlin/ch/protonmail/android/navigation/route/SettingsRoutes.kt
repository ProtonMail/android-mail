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

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingScreen
import ch.protonmail.android.mailsettings.presentation.accountsettings.conversationmode.ConversationModeSettingScreen
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsScreen
import ch.protonmail.android.navigation.Launcher
import ch.protonmail.android.navigation.model.Destination.Screen
import timber.log.Timber

fun NavGraphBuilder.addAccountSettings(navController: NavHostController, launcherActions: Launcher.Actions) {
    composable(route = Screen.AccountSettings.route) {
        AccountSettingScreen(
            actions = AccountSettingScreen.Actions(
                onBackClick = { navController.popBackStack() },
                onPasswordManagementClick = launcherActions.onPasswordManagement,
                onRecoveryEmailClick = launcherActions.onRecoveryEmail,
                onConversationModeClick = { navController.navigate(Screen.ConversationModeSettings.route) },
                onDefaultEmailAddressClick = { Timber.i("Default email address setting clicked") },
                onDisplayNameClick = { Timber.i("Display name setting clicked") },
                onPrivacyClick = { Timber.i("Privacy setting clicked") },
                onSearchMessageContentClick = { Timber.i("Search message content setting clicked") },
                onLabelsFoldersClick = { Timber.i("Labels folders setting clicked") },
                onLocalStorageClick = { Timber.i("Local storage setting clicked") },
                onSnoozeNotificationsClick = { Timber.i("Snooze notification setting clicked") }
            )
        )
    }
}


/**
 * Adds to this [NavGraphBuilder] a screen which allows to change conversation mode.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 */
internal fun NavGraphBuilder.addConversationModeSettings(navController: NavHostController) {
    composable(route = Screen.ConversationModeSettings.route) {
        ConversationModeSettingScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }
}

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change the app language.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 */
internal fun NavGraphBuilder.addLanguageSettings(navController: NavHostController) {
    composable(route = Screen.LanguageSettings.route) {
        LanguageSettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }
}

/**
 * Adds to this [NavGraphBuilder] a screen which allows to visualise swiper actions.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 *  back button is pressed and navigate to EditSwipeActionPreferenceScreen
 */
internal fun NavGraphBuilder.addSwipeActionsSettings(navController: NavHostController) {
    composable(route = Screen.SwipeActionsSettings.route) {
        SwipeActionsPreferenceScreen(
            modifier = Modifier,
            actions = SwipeActionsPreferenceScreen.Actions(
                onBackClick = { navController.popBackStack() },
                onChangeSwipeLeftClick = {},
                onChangeSwipeRightClick = {}
            )
        )
    }
}

/**
 * Adds to this [NavGraphBuilder] a screen which allows to change theme.
 * @param navController the NavHostController which hosts this screen. Used to popBackStack when
 * back button is pressed
 */
internal fun NavGraphBuilder.addThemeSettings(navController: NavHostController) {
    composable(route = Screen.ThemeSettings.route) {
        ThemeSettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.popBackStack() }
        )
    }
}
