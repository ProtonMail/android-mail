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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import ch.protonmail.android.MainActivity
import ch.protonmail.android.design.compose.navigation.get
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.feature.account.SignOutAccountDialog
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.SnackbarType
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailcontact.domain.model.ContactId
import ch.protonmail.android.mailcontact.presentation.contactdetails.ui.ContactDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactgroupdetails.ContactGroupDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactlist.ui.ContactListScreen
import ch.protonmail.android.mailcontact.presentation.contactsearch.ContactSearchScreen
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.ui.PagedConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.RawMessageDataScreen
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.domain.model.ToolbarType
import ch.protonmail.android.mailsettings.presentation.appsettings.AppSettingsScreen
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.transitions.RouteTransitionSpec
import ch.protonmail.android.navigation.transitions.composableWithTransitions
import ch.protonmail.android.uicomponents.fab.ProtonFabHostState
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountSwitchEvent
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank

@SuppressWarnings("LongMethod")
internal fun NavGraphBuilder.addConversationDetail(actions: ConversationDetail.Actions) {

    composableWithTransitions(
        route = Destination.Screen.Conversation.route,
        transitions = RouteTransitionSpec.Conversation
    ) {
        PagedConversationDetailScreen(
            actions = actions
        )
    }
}

@Suppress("LongMethod", "LongParameterList")
internal fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    fabHostState: ProtonFabHostState,
    openDrawerMenu: () -> Unit,
    setDrawerEnabled: (Boolean) -> Unit,
    onEvent: (AccountSwitchEvent) -> Unit,
    showSnackbar: (type: SnackbarType) -> Unit,
    showFeatureMissingSnackbar: () -> Unit,
    onActionBarVisibilityChanged: (Boolean) -> Unit,
    onShowRatingBooster: () -> Unit
) {
    composableWithTransitions(
        route = Destination.Screen.Mailbox.route,
        transitions = RouteTransitionSpec.Mailbox
    ) {
        MailboxScreen(
            actions = MailboxScreen.Actions.Empty.copy(
                navigateToMailboxItem = { request ->
                    val destination = when (request.shouldOpenInComposer) {
                        true -> Destination.Screen.EditDraftComposer(MessageId(request.itemId.value))
                        false -> Destination.Screen.Conversation(
                            conversationId = ConversationId(request.itemId.value),
                            scrollToMessageId = request.subItemId?.let { mailboxItemId ->
                                MessageId(mailboxItemId.value)
                            },
                            openedFromLocation = request.openedFromLocation,
                            locationViewModeIsConversation = request.locationViewModeIsConversation,
                            entryPoint = ConversationDetailEntryPoint.Mailbox
                        )
                    }
                    navController.navigate(destination)
                },
                navigateToComposer = { navController.navigate(Destination.Screen.Composer.route) },
                openDrawerMenu = openDrawerMenu,
                showSnackbar = showSnackbar,
                onAddLabel = { navController.navigate(Destination.Screen.FolderAndLabelSettings.route) },
                onAddFolder = { navController.navigate(Destination.Screen.FolderAndLabelSettings.route) },
                onAccountAvatarClicked = {
                    navController.navigate(Destination.Screen.AccountsManager.route)
                },
                onNavigateToUpselling = { entryPoint, type ->
                    navController.navigate(Destination.Screen.FeatureUpselling(entryPoint, type))
                },
                showMissingFeature = showFeatureMissingSnackbar,
                onEnterSearchMode = {
                    setDrawerEnabled(false)
                },
                onExitSearchMode = {
                    setDrawerEnabled(true)
                },
                onActionBarVisibilityChanged = onActionBarVisibilityChanged,
                onCustomizeToolbar = {
                    navController.navigate(Destination.Screen.EditToolbarScreen(ToolbarType.List))
                },
                onShowRatingBooster = onShowRatingBooster
            ),
            onEvent = onEvent,
            fabHostState = fabHostState
        )
    }
}

internal fun NavGraphBuilder.addEntireMessageBody(
    navController: NavHostController,
    onOpenMessageBodyLink: (Uri) -> Unit
) {
    composableWithTransitions(route = Destination.Screen.EntireMessageBody.route) {
        EntireMessageBodyScreen(
            onBackClick = { navController.navigateBack() },
            onOpenMessageBodyLink = onOpenMessageBodyLink
        )
    }
}

internal fun NavGraphBuilder.addRawMessageData(navController: NavHostController) {
    composableWithTransitions(route = Destination.Screen.RawMessageData.route) {
        RawMessageDataScreen(
            onBackClick = { navController.navigateBack() }
        )
    }
}

@Suppress("LongParameterList")
internal fun NavGraphBuilder.addComposer(
    navController: NavHostController,
    activityActions: MainActivity.Actions,
    showDraftSavedSnackbar: (messageId: MessageId) -> Unit,
    showMessageSendingSnackbar: () -> Unit,
    showMessageSendingOfflineSnackbar: () -> Unit,
    showMessageSchedulingSnackbar: () -> Unit,
    showMessageSchedulingOfflineSnackbar: () -> Unit,
    showDraftDiscardedSnackbar: () -> Unit
) {
    val actions = ComposerScreen.Actions(
        onCloseComposerClick = navController::navigateBack,
        onSetMessagePasswordClick = {
            navController.navigate(Destination.Screen.SetMessagePassword.route)
        },
        showDraftSavedSnackbar = showDraftSavedSnackbar,
        showMessageSendingSnackbar = showMessageSendingSnackbar,
        showMessageSendingOfflineSnackbar = showMessageSendingOfflineSnackbar,
        showMessageSchedulingSnackbar = { showMessageSchedulingSnackbar() },
        showMessageSchedulingOfflineSnackbar = { showMessageSchedulingOfflineSnackbar() },
        showDraftDiscardedSnackbar = showDraftDiscardedSnackbar,
        onNavigateToUpsell = { type, entryPoint ->
            navController.navigate(Destination.Screen.FeatureUpselling(entryPoint, type))
        }
    )
    composableWithTransitions(
        route = Destination.Screen.Composer.route,
        transitions = RouteTransitionSpec.ComposerFromFab
    ) { ComposerScreen(actions) }

    composableWithTransitions(
        route = Destination.Screen.EditDraftComposer.route,
        transitions = RouteTransitionSpec.ComposerFromDrafts

    ) { ComposerScreen(actions) }
    composableWithTransitions(route = Destination.Screen.MessageActionComposer.route) { ComposerScreen(actions) }
    composableWithTransitions(
        route = Destination.Screen.ShareFileComposer.route,
        arguments = listOf(
            navArgument("isExternal") {
                type = NavType.BoolType
                defaultValue = true
            }
        )
    ) { backStackEntry ->
        val isExternal = backStackEntry.arguments?.getBoolean("isExternal") ?: true

        ComposerScreen(
            actions.copy(
                onCloseComposerClick = {
                    if (isExternal) {
                        activityActions.finishActivity()
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addSignOutAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.SignOut.route) {
        SignOutAccountDialog(
            userId = it.get<String>(SignOutAccountDialog.USER_ID_KEY)?.takeIfNotBlank()?.let(::UserId),
            actions = SignOutAccountDialog.Actions(
                onSignedOut = { navController.navigateBack() },
                onRemoved = { navController.navigateBack() },
                onCancelled = { navController.navigateBack() }
            )
        )
    }
}

internal fun NavGraphBuilder.addSetMessagePassword(navController: NavHostController) {
    composableWithTransitions(route = Destination.Screen.SetMessagePassword.route) {
        SetMessagePasswordScreen(
            onBackClick = {
                navController.navigateBack()
            }
        )
    }
}

internal fun NavGraphBuilder.addRemoveAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.RemoveAccount.route) {
        RemoveAccountDialog(
            userId = it.get<String>(RemoveAccountDialog.USER_ID_KEY)?.takeIfNotBlank()?.let(::UserId),
            onRemoved = { navController.navigateBack() },
            onCancelled = { navController.navigateBack() }
        )
    }
}

internal fun NavGraphBuilder.addSettings(navController: NavHostController, activityActions: MainActivity.Actions) {
    composableWithTransitions(
        route = Destination.Screen.Settings.route,
        transitions = RouteTransitionSpec.Settings
    ) {
        ProtonInvertedTheme {
            MainSettingsScreen(
                actions = MainSettingsScreen.Actions(
                    onAccountClick = {
                        navController.navigate(Destination.Screen.AccountSettings.route)
                    },
                    onAppSettingsClick = {
                        navController.navigate(Destination.Screen.AppSettings.route)
                    },
                    onEmailSettingsClick = {
                        navController.navigate(Destination.Screen.EmailSettings.route)
                    },
                    onFolderAndLabelSettingsClicked = {
                        navController.navigate(Destination.Screen.FolderAndLabelSettings.route)
                    },
                    onSpamFilterSettingsClicked = {
                        navController.navigate(Destination.Screen.SpamFilterSettings.route)
                    },
                    onPrivacyAndSecuritySettingsClicked = {
                        navController.navigate(Destination.Screen.PrivacyAndSecuritySettings.route)
                    },
                    onSecurityKeysClicked = activityActions.openSecurityKeys,
                    onPasswordManagementClicked = activityActions.openPasswordManagement,
                    onAccountStorageClicked = activityActions.openSubscription,
                    onBackClick = {
                        navController.navigateBack()
                    },
                    onSignatureClicked = {
                        navController.navigate(Destination.Screen.SignatureSettingsMenu.route)
                    }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addAppSettings(navController: NavHostController, showFeatureMissingSnackbar: () -> Unit) {
    composableWithTransitions(
        route = Destination.Screen.AppSettings.route,
        transitions = RouteTransitionSpec.SettingsSubScreen
    ) {
        ProtonInvertedTheme {
            AppSettingsScreen(
                actions = AppSettingsScreen.Actions(
                    onThemeClick = {
                        navController.navigate(Destination.Screen.ThemeSettings.route)
                    },
                    onPushNotificationsClick = {
                        navController.navigate(Destination.Screen.Notifications.route)
                    },
                    onAutoLockClick = {
                        navController.navigate(Destination.Screen.AutoLockSettings.route)
                    },
                    onAppLanguageClick = {
                        navController.navigate(Destination.Screen.LanguageSettings.route)
                    },
                    onAppIconSettingsClick = {
                        navController.navigate(Destination.Screen.AppIconSettings.route)
                    },
                    onNavigateToSignatureSettings = {
                        navController.navigate(Destination.Screen.SignatureSettingsMenu.route)
                    },
                    onNavigateToUpselling = { entryPoint, type ->
                        navController.navigate(
                            Destination.Screen.FeatureUpselling(
                                entryPoint, type
                            )
                        )

                    },
                    onSwipeToNextEmailClick = showFeatureMissingSnackbar,
                    onSwipeActionsClick = showFeatureMissingSnackbar,
                    onViewApplicationLogsClick = {
                        navController.navigate(Destination.Screen.ApplicationLogs.route)
                    },
                    onCustomizeToolbarClick = {
                        navController.navigate(Destination.Screen.CustomizeToolbar.route)
                    },
                    onBackClick = {
                        navController.navigateBack()
                    }
                )
            )
        }
    }
}

internal fun NavGraphBuilder.addContacts(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composableWithTransitions(
        route = Destination.Screen.Contacts.route,
        transitions = RouteTransitionSpec.Contacts
    ) {
        ContactListScreen(
            listActions = ContactListScreen.Actions.Empty.copy(
                onNavigateToNewContactForm = {
                    navController.navigate(Destination.Screen.CreateContact.route)
                },
                onNavigateToNewGroupForm = showFeatureMissingSnackbar,
                onNavigateToContactSearch = {
                    navController.navigate(Destination.Screen.ContactSearch.route)
                },
                openImportContact = {
                    showFeatureMissingSnackbar()
                },
                onContactSelected = { contactId ->
                    navController.navigate(Destination.Screen.ContactDetails(contactId))
                },
                onContactGroupSelected = { contactGroupId ->
                    navController.navigate(Destination.Screen.ContactGroupDetails(contactGroupId))
                },
                onBackClick = {
                    navController.navigateBack()
                },
                onNewGroupClick = {
                    // Defined at the inner call site.
                },
                exitWithErrorMessage = { message ->
                    navController.navigateBack()
                    showErrorSnackbar(message)
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addContactDetails(
    navController: NavHostController,
    onShowErrorSnackbar: (String) -> Unit,
    onMessageContact: (String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composableWithTransitions(
        route = Destination.Screen.ContactDetails.route,
        transitions = RouteTransitionSpec.ContactDetails
    ) {
        ContactDetailsScreen(
            actions = ContactDetailsScreen.Actions(
                onBack = { navController.navigateBack() },
                onShowErrorSnackbar = onShowErrorSnackbar,
                onMessageContact = onMessageContact,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addContactSearch(navController: NavHostController) {
    val actions = ContactSearchScreen.Actions(
        onContactSelected = { contactId ->
            navController.navigate(Destination.Screen.ContactDetails(contactId))
        },
        onContactGroupSelected = { contactGroupId ->
            navController.navigate(Destination.Screen.ContactGroupDetails(contactGroupId))
        },
        onClose = { navController.navigateBack() }
    )
    composableWithTransitions(
        route = Destination.Screen.ContactSearch.route,
        transitions = RouteTransitionSpec.ContactSearch
    ) {
        ContactSearchScreen(
            actions
        )
    }
}

internal fun NavGraphBuilder.addContactGroupDetails(
    navController: NavHostController,
    onShowErrorSnackbar: (String) -> Unit,
    onSendGroupMessage: (List<String>) -> Unit,
    onOpenContact: (ContactId) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composableWithTransitions(route = Destination.Screen.ContactGroupDetails.route) {
        ContactGroupDetailsScreen(
            actions = ContactGroupDetailsScreen.Actions(
                onBack = { navController.navigateBack() },
                onShowErrorSnackbar = onShowErrorSnackbar,
                onSendGroupMessage = onSendGroupMessage,
                onOpenContact = onOpenContact,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
        )
    }
}
