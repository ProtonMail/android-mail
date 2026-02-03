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

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import ch.protonmail.android.design.compose.navigation.require
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsPeekView
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsScreen
import ch.protonmail.android.mailbugreport.presentation.ui.report.BugReportScreen
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailfeatureflags.presentation.ui.FeatureFlagOverridesScreen
import ch.protonmail.android.mailpinlock.presentation.autolock.model.DialogType
import ch.protonmail.android.mailpinlock.presentation.autolock.ui.AutoLockIntervalDialog
import ch.protonmail.android.mailpinlock.presentation.autolock.ui.AutoLockSettingsScreen
import ch.protonmail.android.mailpinlock.presentation.autolock.ui.LockScreenOverlay
import ch.protonmail.android.mailpinlock.presentation.pin.ui.AutoLockPinScreen
import ch.protonmail.android.mailpinlock.presentation.pin.ui.dialog.AutoLockPinScreenDialog
import ch.protonmail.android.mailpinlock.presentation.pin.ui.dialog.AutoLockPinScreenDialogKeys.AutoLockPinDialogModeKey
import ch.protonmail.android.mailpinlock.presentation.pin.ui.dialog.AutoLockPinScreenDialogKeys.AutoLockPinDialogResultKey
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.presentation.settings.appicon.ui.AppIconSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.combinedcontacts.CombinedContactsSettingScreen
import ch.protonmail.android.mailsettings.presentation.settings.language.LanguageSettingsDialog
import ch.protonmail.android.mailsettings.presentation.settings.notifications.ui.PushNotificationsSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.privacy.PrivacySettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.signature.SignatureSettingsMenuScreen
import ch.protonmail.android.mailsettings.presentation.settings.signature.email.EmailSignatureSettingScreen
import ch.protonmail.android.mailsettings.presentation.settings.signature.mobile.MobileSignatureSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.EditSwipeActionPreferenceScreen.SWIPE_DIRECTION_KEY
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.SwipeActionsPreferenceScreen
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeSettingsDialog
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui.CustomizeToolbarEditScreen
import ch.protonmail.android.mailsettings.presentation.settings.toolbar.ui.CustomizeToolbarScreen
import ch.protonmail.android.mailsettings.presentation.webaccountsettings.WebAccountSettingScreen
import ch.protonmail.android.mailsettings.presentation.webemailsettings.WebEmailSettingScreen
import ch.protonmail.android.mailsettings.presentation.webfoldersettings.WebFoldersAndLabelsSettingScreen
import ch.protonmail.android.mailsettings.presentation.webprivacysettings.WebPrivacyAndSecuritySettingsScreen
import ch.protonmail.android.mailsettings.presentation.websettings.WebSettingsScreenActions
import ch.protonmail.android.mailsettings.presentation.webspamsettings.WebSpamFilterSettingsScreen
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.transitions.RouteTransitionSpec
import ch.protonmail.android.navigation.transitions.composableWithTransitions
import me.proton.core.util.kotlin.deserialize

fun NavGraphBuilder.addWebAccountSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.AccountSettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            WebAccountSettingScreen(
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

fun NavGraphBuilder.addWebEmailSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.EmailSettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            WebEmailSettingScreen(
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

fun NavGraphBuilder.addWebFolderAndLabelSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.FolderAndLabelSettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            WebFoldersAndLabelsSettingScreen(
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() },
                    onUpsellNavigation = { entryPoint, visibility ->
                        navController.navigate(Screen.FeatureUpselling(entryPoint, visibility))
                    }
                )
            )
        }
    }
}

fun NavGraphBuilder.addWebPrivacyAndSecuritySettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.PrivacyAndSecuritySettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            WebPrivacyAndSecuritySettingsScreen(
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

fun NavGraphBuilder.addWebSpamFilterSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.SpamFilterSettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            WebSpamFilterSettingsScreen(
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addCombinedContactsSetting(navController: NavHostController) {
    composableWithTransitions(route = Screen.CombinedContactsSettings.route) {
        CombinedContactsSettingScreen(
            modifier = Modifier,
            onBackClick = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addPrivacySettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.PrivacySettings.route) {
        PrivacySettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addAppIconSettings(navController: NavHostController, onLearnMoreClick: (Uri) -> Unit) {
    composableWithTransitions(route = Screen.AppIconSettings.route) {
        ProtonInvertedTheme {
            AppIconSettingsScreen(
                modifier = Modifier,
                onBackClick = { navController.navigateBack() }
            )
        }
    }
}

internal fun NavGraphBuilder.addMobileSignatureSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.MobileSignatureSettings.route,
        transitions = RouteTransitionSpec.ForwardBack
    ) {
        ProtonInvertedTheme {
            MobileSignatureSettingsScreen(
                modifier = Modifier,
                signatureActions = MobileSignatureSettingsScreen.Actions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addEmailSignatureSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.EmailSignatureSettings.route,
        transitions = RouteTransitionSpec.ForwardBack
    ) {
        ProtonInvertedTheme {
            EmailSignatureSettingScreen(
                modifier = Modifier,
                actions = WebSettingsScreenActions.Empty.copy(
                    onBackClick = { navController.navigateBack() }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addSignatureMenuSettings(navController: NavHostController) {
    composableWithTransitions(
        route = Screen.SignatureSettingsMenu.route,
        transitions = RouteTransitionSpec.SignatureSettingsMenu
    ) {
        ProtonInvertedTheme {
            SignatureSettingsMenuScreen(
                modifier = Modifier,
                actions = SignatureSettingsMenuScreen.Actions(
                    onBackClick = { navController.navigateBack() },
                    onNavigateToMobileSignatureSettings = {
                        navController.navigate(Screen.MobileSignatureSettings.route)
                    },
                    onNavigateToUpselling = { entryPoint, type ->
                        navController.navigate(Screen.FeatureUpselling(entryPoint, type))
                    },
                    onNavigateToEmailSignatureSettings = {
                        navController.navigate(Screen.EmailSignatureSettings.route)
                    }
                )
            )
        }
    }
}

@Suppress("ForbiddenComment")
internal fun NavGraphBuilder.addAutoLockSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.AutoLockSettings.route) {
        ProtonInvertedTheme {
            AutoLockSettingsScreen(
                modifier = Modifier,
                navController = navController,
                actions = AutoLockSettingsScreen.Actions(
                    onPinScreenNavigation = { navController.navigate(Screen.AutoLockPinScreen(it)) },
                    onBackClick = { navController.navigateBack() },
                    onChangeIntervalClick = { navController.navigate(Screen.AutoLockInterval.route) },
                    onDialogNavigation = { navController.navigate(Screen.AutoLockPinConfirmDialog(it)) }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addAutoLockOverlay(onClose: () -> Unit, onNavigateToPinLock: () -> Unit) {
    composableWithTransitions(route = Screen.AutoLockOverlay.route) {
        LockScreenOverlay(
            onClose = onClose,
            onNavigateToPinInsertion = onNavigateToPinLock
        )
    }
}

internal fun NavGraphBuilder.addAutoLockPinScreen(onClose: () -> Unit, onShowSuccessSnackbar: (String) -> Unit) {
    composableWithTransitions(route = Screen.AutoLockPinScreen.route) {
        AutoLockPinScreen(onClose = onClose, onShowSuccessSnackbar = onShowSuccessSnackbar)
    }
}

internal fun NavGraphBuilder.addEditSwipeActionsSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.EditSwipeActionSettings.route) {
        EditSwipeActionPreferenceScreen(
            modifier = Modifier,
            direction = SwipeActionDirection(it.require(SWIPE_DIRECTION_KEY)),
            onBack = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addLanguageSettings(navController: NavHostController) {
    dialog(route = Screen.LanguageSettings.route) {
        LanguageSettingsDialog(
            modifier = Modifier,
            onDismiss = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addPinDialog(navController: NavHostController) {
    dialog(route = Screen.AutoLockPinConfirmDialog.route) { backStackEntry ->
        val dialogType = backStackEntry.arguments?.getString(AutoLockPinDialogModeKey)?.deserialize<DialogType>()
            ?: DialogType.None

        AutoLockPinScreenDialog(
            dialogType = dialogType,
            onNavigateBack = { navController.popBackStack() },
            onSuccessWithResult = { resultKey ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(AutoLockPinDialogResultKey, resultKey)
                navController.popBackStack()
            }
        )
    }
}


internal fun NavGraphBuilder.addSwipeActionsSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.SwipeActionsSettings.route) {
        SwipeActionsPreferenceScreen(
            modifier = Modifier,
            actions = SwipeActionsPreferenceScreen.Actions(
                onBackClick = { navController.navigateBack() },
                onChangeSwipeLeftClick = {
                    navController.navigate(Screen.EditSwipeActionSettings(SwipeActionDirection.LEFT))
                },
                onChangeSwipeRightClick = {
                    navController.navigate(Screen.EditSwipeActionSettings(SwipeActionDirection.RIGHT))
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addAutoLockIntervalSettings(navController: NavHostController) {
    dialog(route = Screen.AutoLockInterval.route) {
        AutoLockIntervalDialog(
            modifier = Modifier,
            onDismiss = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addThemeSettings(navController: NavHostController) {
    dialog(route = Screen.ThemeSettings.route) {
        ThemeSettingsDialog(
            modifier = Modifier,
            onDismiss = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addNotificationsSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.Notifications.route) {
        PushNotificationsSettingsScreen(
            modifier = Modifier,
            onBackClick = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addCustomizeToolbarSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.CustomizeToolbar.route) {
        CustomizeToolbarScreen(
            modifier = Modifier,
            onBack = { navController.navigateBack() },
            onCustomize = { toolbarType ->
                navController.navigate(Screen.EditToolbarScreen(toolbarType))
            }
        )
    }
    composableWithTransitions(route = Screen.EditToolbarScreen.route) {
        CustomizeToolbarEditScreen(
            modifier = Modifier,
            onBackClick = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addExportLogsSettings(navController: NavHostController) {
    composableWithTransitions(route = Screen.ApplicationLogs.route) {
        ApplicationLogsScreen(
            actions = ApplicationLogsScreen.Actions(
                onBackClick = { navController.navigateBack() },
                onViewItemClick = { navController.navigate(Screen.ApplicationLogsView(it)) },
                onFeatureFlagsNavigation = {
                    navController.navigate(Screen.FeatureFlagsOverrides.route)
                }
            )
        )
    }
    composableWithTransitions(route = Screen.ApplicationLogsView.route) {
        ApplicationLogsPeekView(
            onBack = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addFeatureFlagsOverrides(navController: NavHostController) {
    composableWithTransitions(route = Screen.FeatureFlagsOverrides.route) {
        FeatureFlagOverridesScreen(
            onBack = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addBugReporting(navController: NavController, onShowNormalSnackbar: (String) -> Unit) {
    composableWithTransitions(route = Screen.BugReporting.route) {
        BugReportScreen(
            onBack = { navController.navigateBack() },
            onSuccess = {
                navController.navigateBack()
                onShowNormalSnackbar(it)
            }
        )
    }
}
