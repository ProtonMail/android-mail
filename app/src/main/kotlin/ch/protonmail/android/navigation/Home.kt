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

package ch.protonmail.android.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.MainActivity
import ch.protonmail.android.R
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel
import ch.protonmail.android.mailmailbox.presentation.sidebar.Sidebar
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.model.HomeAction
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.route.addAccountSettings
import ch.protonmail.android.navigation.route.addAlternativeRoutingSetting
import ch.protonmail.android.navigation.route.addCombinedContactsSetting
import ch.protonmail.android.navigation.route.addComposer
import ch.protonmail.android.navigation.route.addConversationDetail
import ch.protonmail.android.navigation.route.addConversationModeSettings
import ch.protonmail.android.navigation.route.addDeepLinkHandler
import ch.protonmail.android.navigation.route.addEditSwipeActionsSettings
import ch.protonmail.android.navigation.route.addLanguageSettings
import ch.protonmail.android.navigation.route.addMailbox
import ch.protonmail.android.navigation.route.addMessageDetail
import ch.protonmail.android.navigation.route.addRemoveAccountDialog
import ch.protonmail.android.navigation.route.addSettings
import ch.protonmail.android.navigation.route.addSwipeActionsSettings
import ch.protonmail.android.navigation.route.addThemeSettings
import io.sentry.compose.withSentryObservableEffect
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.network.domain.NetworkStatus

@Composable
fun Home(
    activityActions: MainActivity.Actions,
    launcherActions: Launcher.Actions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController().withSentryObservableEffect()
    val scaffoldState = rememberScaffoldState()
    val snackbarHostSuccessState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.SUCCESS)
    val snackbarHostWarningState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.WARNING)
    val snackbarHostNormState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.NORM)
    val snackbarHostErrorState = ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR)
    val scope = rememberCoroutineScope()
    val state = rememberAsState(flow = viewModel.state, initial = HomeState.Initial)

    val offlineSnackbarMessage = stringResource(id = R.string.you_are_offline)
    fun showOfflineSnackbar() = scope.launch {
        snackbarHostWarningState.showSnackbar(
            message = offlineSnackbarMessage,
            type = ProtonSnackbarType.WARNING
        )
    }
    ConsumableLaunchedEffect(state.value.networkStatusEffect) {
        if (it == NetworkStatus.Disconnected) {
            showOfflineSnackbar()
        }
    }

    val featureMissingSnackbarMessage = stringResource(id = R.string.feature_coming_soon)
    fun showFeatureMissingSnackbar() = scope.launch {
        snackbarHostNormState.showSnackbar(
            message = featureMissingSnackbarMessage,
            type = ProtonSnackbarType.NORM
        )
    }
    val refreshMailboxErrorMessage = stringResource(id = R.string.mailbox_error_message_generic)
    fun showRefreshErrorSnackbar() = scope.launch {
        snackbarHostErrorState.showSnackbar(
            message = refreshMailboxErrorMessage,
            type = ProtonSnackbarType.ERROR
        )
    }
    val draftSavedText = stringResource(id = R.string.mailbox_draft_saved)
    fun showDraftSavedSnackbar() = scope.launch {
        snackbarHostSuccessState.showSnackbar(message = draftSavedText, type = ProtonSnackbarType.SUCCESS)
    }
    val sendingMessageText = stringResource(id = R.string.mailbox_message_sending)
    fun showMessageSendingSnackbar() = scope.launch {
        snackbarHostNormState.showSnackbar(message = sendingMessageText, type = ProtonSnackbarType.NORM)
    }
    val sendingMessageOfflineText = stringResource(id = R.string.mailbox_message_sending_offline)
    fun showMessageSendingOfflineSnackbar() = scope.launch {
        snackbarHostNormState.showSnackbar(message = sendingMessageOfflineText, type = ProtonSnackbarType.NORM)
    }
    val successSendingMessageText = stringResource(id = R.string.mailbox_message_sending_success)
    fun showSuccessSendingMessageSnackbar(uiModel: MessageSendingUiModel.MessageSent) = scope.launch {
        snackbarHostSuccessState.showSnackbar(message = successSendingMessageText, type = ProtonSnackbarType.SUCCESS)
        viewModel.submit(HomeAction.MessageSentShown(uiModel))
    }
    val errorSendingMessageText = stringResource(id = R.string.mailbox_message_sending_error)
    fun showErrorSendingMessageSnackbar(uiModel: MessageSendingUiModel.SendMessageError) = scope.launch {
        snackbarHostErrorState.showSnackbar(message = errorSendingMessageText, type = ProtonSnackbarType.ERROR)
        viewModel.submit(HomeAction.MessageSendingErrorShown(uiModel))
    }
    ConsumableLaunchedEffect(state.value.messageSendingStatusEffect) { sendingStatus ->
        when (sendingStatus) {
            is MessageSendingUiModel.MessageSent -> showSuccessSendingMessageSnackbar(sendingStatus)
            is MessageSendingUiModel.SendMessageError -> showErrorSendingMessageSnackbar(sendingStatus)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        drawerShape = RectangleShape,
        drawerScrimColor = ProtonTheme.colors.blenderNorm,
        drawerContent = {
            Sidebar(
                drawerState = scaffoldState.drawerState,
                navigationActions = buildSidebarActions(navController, launcherActions)
            )
        },
        snackbarHost = {
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostSuccessState
            )
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostWarningState
            )
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostNormState
            )
            ProtonSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostErrorState
            )
        }
    ) { contentPadding ->
        Box(
            Modifier.padding(contentPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Mailbox.route
            ) {
                // home
                addConversationDetail(
                    navController = navController,
                    showSnackbar = { message ->
                        scope.launch {
                            snackbarHostNormState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                    },
                    openMessageBodyLink = activityActions.openInActivityInNewTask,
                    openAttachment = activityActions.openIntentChooser,
                    showFeatureMissingSnackbar = {
                        showFeatureMissingSnackbar()
                    }
                )
                addMailbox(
                    navController,
                    openDrawerMenu = { scope.launch { scaffoldState.drawerState.open() } },
                    showOfflineSnackbar = { showOfflineSnackbar() },
                    showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
                    showRefreshErrorSnackbar = { showRefreshErrorSnackbar() }
                )
                addMessageDetail(
                    navController = navController,
                    showSnackbar = { message ->
                        scope.launch {
                            snackbarHostNormState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                    },
                    openMessageBodyLink = activityActions.openInActivityInNewTask,
                    openAttachment = activityActions.openIntentChooser,
                    showFeatureMissingSnackbar = { showFeatureMissingSnackbar() }
                )
                addComposer(
                    navController,
                    onAddAttachments = launcherActions.onAddAttachments,
                    showDraftSavedSnackbar = { showDraftSavedSnackbar() },
                    showMessageSendingSnackbar = { showMessageSendingSnackbar() },
                    showMessageSendingOfflineSnackbar = { showMessageSendingOfflineSnackbar() }
                )
                addRemoveAccountDialog(navController)
                addSettings(
                    navController,
                    showFeatureMissingSnackbar = {
                        showFeatureMissingSnackbar()
                    }
                )
                // settings
                addAccountSettings(
                    navController,
                    launcherActions,
                    showFeatureMissingSnackbar = {
                        showFeatureMissingSnackbar()
                    }
                )
                addAlternativeRoutingSetting(navController)
                addCombinedContactsSetting(navController)
                addConversationModeSettings(navController)
                addEditSwipeActionsSettings(navController)
                addLanguageSettings(navController)
                addSwipeActionsSettings(navController)
                addThemeSettings(navController)
                addDeepLinkHandler(navController)
            }
        }
    }
}

private fun buildSidebarActions(navController: NavHostController, launcherActions: Launcher.Actions) =
    Sidebar.NavigationActions(
        onSignIn = launcherActions.onSignIn,
        onSignOut = launcherActions.onSignOut,
        onRemoveAccount = { navController.navigate(Dialog.RemoveAccount(it)) },
        onSwitchAccount = launcherActions.onSwitchAccount,
        onSettings = { navController.navigate(Screen.Settings.route) },
        onLabelsSettings = {},
        onSubscription = launcherActions.onSubscription,
        onReportBug = launcherActions.onReportBug
    )
