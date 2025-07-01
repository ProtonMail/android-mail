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
import androidx.compose.material.DrawerState
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import ch.protonmail.android.MainActivity
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.feature.account.SignOutAccountDialog
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsViewItemMode
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen2
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailcontact.presentation.contactdetails.ContactDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactform.ContactFormScreen
import ch.protonmail.android.mailcontact.presentation.contactgroupdetails.ContactGroupDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactgroupform.ContactGroupFormScreen
import ch.protonmail.android.mailcontact.presentation.contactlist.ui.ContactListScreen
import ch.protonmail.android.mailcontact.presentation.contactsearch.ContactSearchScreen
import ch.protonmail.android.mailcontact.presentation.managemembers.ManageMembersScreen
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.ui.MessageDetail
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.presentation.folderform.FolderFormScreen
import ch.protonmail.android.maillabel.presentation.folderlist.FolderListScreen
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormScreen
import ch.protonmail.android.maillabel.presentation.labellist.LabelListScreen
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.mailupselling.presentation.ui.postsubscription.PostSubscriptionScreen
import ch.protonmail.android.mailupselling.presentation.usecase.UpsellingVisibility
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.model.SavedStateKey
import me.proton.core.compose.navigation.get
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank


internal fun NavGraphBuilder.addConversationDetail(actions: ConversationDetail.Actions) {
    composable(route = Destination.Screen.Conversation.route) {
        ConversationDetailScreen(actions = actions)
    }
}

@Suppress("LongParameterList")
internal fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    drawerState: DrawerState,
    showOfflineSnackbar: () -> Unit,
    showNormalSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (String) -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    composable(route = Destination.Screen.Mailbox.route) {
        MailboxScreen(
            actions = MailboxScreen.Actions.Empty.copy(
                navigateToMailboxItem = { request ->
                    val destination = when (request.shouldOpenInComposer) {
                        true -> Destination.Screen.EditDraftComposer(MessageId(request.itemId.value))
                        false -> when (request.itemType) {
                            MailboxItemType.Message -> Destination.Screen.Message(MessageId(request.itemId.value))
                            MailboxItemType.Conversation ->
                                Destination.Screen.Conversation(
                                    ConversationId(request.itemId.value),
                                    request.subItemId?.let { mailboxItemId ->
                                        MessageId(mailboxItemId.value)
                                    },
                                    request.filterByLocation
                                )
                        }
                    }
                    navController.navigate(destination)
                },
                navigateToComposer = { navController.navigate(Destination.Screen.Composer.route) },
                showOfflineSnackbar = showOfflineSnackbar,
                showNormalSnackbar = showNormalSnackbar,
                showErrorSnackbar = showErrorSnackbar,
                onAddLabel = { navController.navigate(Destination.Screen.CreateLabel.route) },
                onAddFolder = { navController.navigate(Destination.Screen.CreateFolder.route) },
                onNavigateToStandaloneUpselling = { type ->
                    val destination = when (type) {
                        UpsellingVisibility.HIDDEN -> return@copy
                        UpsellingVisibility.PROMO -> Destination.Screen.Upselling.StandaloneMailboxPromo.route
                        UpsellingVisibility.NORMAL -> Destination.Screen.Upselling.StandaloneMailbox.route
                        UpsellingVisibility.DRIVE_SPOTLIGHT -> Destination.Screen.DriveSpotlight.route
                    }
                    navController.navigate(destination)
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
                navigateToCustomizeToolbar = {
                    navController.navigate(Screen.CustomizeToolbar.route)
                },
                navigateToNPSFeedback = {
                    navController.navigate(Screen.NPSFeedback.route)
                }
            ),
            drawerState = drawerState
        )
    }
}

internal fun NavGraphBuilder.addMessageDetail(actions: MessageDetail.Actions) {
    composable(route = Destination.Screen.Message.route) {
        MessageDetailScreen(actions = actions)
    }
}

internal fun NavGraphBuilder.addEntireMessageBody(
    navController: NavHostController,
    onOpenMessageBodyLink: (Uri) -> Unit
) {
    composable(route = Destination.Screen.EntireMessageBody.route) {
        EntireMessageBodyScreen(
            onBackClick = { navController.navigateBack() },
            onOpenMessageBodyLink = onOpenMessageBodyLink
        )
    }
}

internal fun NavGraphBuilder.addComposer(
    navController: NavHostController,
    activityActions: MainActivity.Actions,
    showDraftSavedSnackbar: (messasgeId: MessageId) -> Unit,
    showMessageSendingSnackbar: () -> Unit,
    showMessageSendingOfflineSnackbar: () -> Unit,
    showComposerV2: Boolean = false
) {
    val actions = ComposerScreen.Actions(
        onCloseComposerClick = navController::navigateBack,
        onSetMessagePasswordClick = { messageId, senderEmail ->
            navController.navigate(Destination.Screen.SetMessagePassword(messageId, senderEmail))
        },
        showDraftSavedSnackbar = showDraftSavedSnackbar,
        showMessageSendingSnackbar = showMessageSendingSnackbar,
        showMessageSendingOfflineSnackbar = showMessageSendingOfflineSnackbar
    )
    if (showComposerV2) {
        composable(route = Destination.Screen.Composer.route) { ComposerScreen2(actions) }
        composable(route = Destination.Screen.EditDraftComposer.route) { ComposerScreen2(actions) }
        composable(route = Destination.Screen.MessageActionComposer.route) { ComposerScreen2(actions) }
        composable(route = Destination.Screen.ShareFileComposer.route) {
            ComposerScreen2(
                actions.copy(
                    onCloseComposerClick = { activityActions.finishActivity() }
                )
            )
        }
    } else {
        composable(route = Destination.Screen.Composer.route) { ComposerScreen(actions) }
        composable(route = Destination.Screen.EditDraftComposer.route) { ComposerScreen(actions) }
        composable(route = Destination.Screen.MessageActionComposer.route) { ComposerScreen(actions) }
        composable(route = Destination.Screen.ShareFileComposer.route) {
            ComposerScreen(
                actions.copy(
                    onCloseComposerClick = { activityActions.finishActivity() }
                )
            )
        }
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
    composable(route = Destination.Screen.SetMessagePassword.route) {
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

internal fun NavGraphBuilder.addSettings(navController: NavHostController) {
    composable(route = Destination.Screen.Settings.route) {
        MainSettingsScreen(
            actions = MainSettingsScreen.Actions(
                onAccountClick = {
                    navController.navigate(Destination.Screen.AccountSettings.route)
                },
                onThemeClick = {
                    navController.navigate(Destination.Screen.ThemeSettings.route)
                },
                onPushNotificationsClick = {
                    navController.navigate(Destination.Screen.Notifications.route)
                },
                onAutoLockClick = {
                    navController.navigate(Destination.Screen.AutoLockSettings.route)
                },
                onAlternativeRoutingClick = {
                    navController.navigate(Destination.Screen.AlternativeRoutingSettings.route)
                },
                onAppLanguageClick = {
                    navController.navigate(Destination.Screen.LanguageSettings.route)
                },
                onCombinedContactsClick = {
                    navController.navigate(Destination.Screen.CombinedContactsSettings.route)
                },
                onCustomizeToolbarClick = {
                    navController.navigate(Destination.Screen.CustomizeToolbar.route)
                },
                onSwipeActionsClick = {
                    navController.navigate(Destination.Screen.SwipeActionsSettings.route)
                },
                onClearCacheClick = {},
                onExportLogsClick = { isInternalFeatureEnabled ->
                    if (isInternalFeatureEnabled) {
                        navController.navigate(Destination.Screen.ApplicationLogs.route)
                    } else {
                        navController.navigate(
                            Destination.Screen.ApplicationLogsView(ApplicationLogsViewItemMode.Events)
                        )
                    }
                },
                onBackClick = {
                    navController.navigateBack()
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addLabelList(
    navController: NavHostController,
    showLabelListErrorLoadingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.LabelList.route) {
        LabelListScreen(
            actions = LabelListScreen.Actions(
                onBackClick = {
                    navController.navigateBack()
                },
                onLabelSelected = { labelId ->
                    navController.navigate(Destination.Screen.EditLabel(labelId))
                },
                onAddLabelClick = {
                    navController.navigate(Destination.Screen.CreateLabel.route)
                },
                showLabelListErrorLoadingSnackbar = showLabelListErrorLoadingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addLabelForm(
    navController: NavHostController,
    showLabelSavedSnackbar: () -> Unit,
    showLabelDeletedSnackbar: () -> Unit,
    showUpsellingSnackbar: (String) -> Unit,
    showUpsellingErrorSnackbar: (String) -> Unit
) {
    val actions = LabelFormScreen.Actions.Empty.copy(
        onBackClick = {
            navController.navigateBack()
        },
        showLabelSavedSnackbar = showLabelSavedSnackbar,
        showLabelDeletedSnackbar = showLabelDeletedSnackbar,
        showUpsellingSnackbar = showUpsellingSnackbar,
        showUpsellingErrorSnackbar = showUpsellingErrorSnackbar
    )
    composable(route = Destination.Screen.CreateLabel.route) { LabelFormScreen(actions) }
    composable(route = Destination.Screen.EditLabel.route) { LabelFormScreen(actions) }
}

internal fun NavGraphBuilder.addFolderList(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    composable(route = Destination.Screen.FolderList.route) {
        FolderListScreen(
            actions = FolderListScreen.Actions(
                onBackClick = {
                    navController.navigateBack()
                },
                onFolderSelected = { labelId ->
                    navController.navigate(Destination.Screen.EditFolder(labelId))
                },
                onAddFolderClick = {
                    navController.navigate(Destination.Screen.CreateFolder.route)
                },
                exitWithErrorMessage = { message ->
                    navController.navigateBack()
                    showErrorSnackbar(message)
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addFolderForm(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit,
    showNormSnackbar: (String) -> Unit
) {
    val actions = FolderFormScreen.Actions.Empty.copy(
        onBackClick = {
            navController.navigateBack()
        },
        onFolderParentClick = { labelId, currentParentLabelId ->
            navController.navigate(Destination.Screen.ParentFolderList(labelId, currentParentLabelId))
        },
        exitWithSuccessMessage = { message ->
            navController.navigateBack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        },
        showUpsellingSnackbar = { showNormSnackbar(it) },
        showUpsellingErrorSnackbar = { showErrorSnackbar(it) }
    )
    composable(route = Destination.Screen.CreateFolder.route) {
        FolderFormScreen(
            actions,
            currentParentLabelId = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                SavedStateKey.CurrentParentFolderId.key
            )?.observeAsState()
        )
    }
    composable(route = Destination.Screen.EditFolder.route) {
        FolderFormScreen(
            actions,
            currentParentLabelId = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                SavedStateKey.CurrentParentFolderId.key
            )?.observeAsState()
        )
    }
}

internal fun NavGraphBuilder.addParentFolderList(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    val actions = ParentFolderListScreen.Actions.Empty.copy(
        onBackClick = {
            navController.navigateBack()
        },
        onFolderSelected = { labelId ->
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.CurrentParentFolderId.key,
                labelId.id
            )
            navController.navigateBack()
        },
        onNoneClick = {
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.CurrentParentFolderId.key,
                ""
            )
            navController.navigateBack()
        },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.ParentFolderList.route) {
        ParentFolderListScreen(actions)
    }
}

internal fun NavGraphBuilder.addContacts(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit,
    showNormalSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Contacts.route) {
        ContactListScreen(
            listActions = ContactListScreen.Actions(
                onNavigateToNewContactForm = {
                    navController.navigate(Destination.Screen.CreateContact.route)
                },
                onNavigateToNewGroupForm = {
                    navController.navigate(Destination.Screen.CreateContactGroup.route)
                },
                onNavigateToContactSearch = {
                    navController.navigate(Destination.Screen.ContactSearch.route)
                },
                openImportContact = {
                    showFeatureMissingSnackbar()
                },
                onContactSelected = { contactId ->
                    navController.navigate(Destination.Screen.ContactDetails(contactId))
                },
                onContactGroupSelected = { labelId ->
                    navController.navigate(Destination.Screen.ContactGroupDetails(labelId))
                },
                onBackClick = {
                    navController.navigateBack()
                },
                onSubscriptionUpgradeRequired = {
                    showNormalSnackbar(it)
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
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    val actions = ContactDetailsScreen.Actions.Empty.copy(
        onBackClick = { navController.navigateBack() },
        exitWithSuccessMessage = { message ->
            navController.navigateBack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        },
        onEditClick = { contactId ->
            navController.navigate(Destination.Screen.EditContact(contactId))
        },
        showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
        navigateToComposer = {
            navController.navigate(Destination.Screen.MessageActionComposer(DraftAction.ComposeToAddresses(listOf(it))))
        }
    )
    composable(route = Destination.Screen.ContactDetails.route) {
        ContactDetailsScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactForm(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit
) {

    val actions = ContactFormScreen.Actions.Empty.copy(
        onCloseClick = {
            navController.navigateBack()
        },
        exitWithSuccessMessage = { message ->
            navController.navigateBack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.CreateContact.route) {
        ContactFormScreen(actions)
    }
    composable(route = Destination.Screen.EditContact.route) {
        ContactFormScreen(actions)
    }
    composable(route = Destination.Screen.AddContact.route) {
        ContactFormScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactGroupDetails(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit,
    showNormSnackbar: (message: String) -> Unit
) {
    val actions = ContactGroupDetailsScreen.Actions(
        onBackClick = { navController.navigateBack() },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        },
        exitWithNormMessage = { message ->
            navController.navigateBack()
            showNormSnackbar(message)
        },
        showErrorMessage = { message ->
            showErrorSnackbar(message)
        },
        onEditClick = { labelId ->
            navController.navigate(Destination.Screen.EditContactGroup(labelId))
        },
        navigateToComposer = { emails ->
            navController.navigate(Destination.Screen.MessageActionComposer(DraftAction.ComposeToAddresses(emails)))
        }
    )
    composable(route = Destination.Screen.ContactGroupDetails.route) {
        ContactGroupDetailsScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactGroupForm(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit,
    showNormSnackbar: (message: String) -> Unit
) {
    val actions = ContactGroupFormScreen.Actions(
        onClose = { navController.navigateBack() },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        },
        exitWithSuccessMessage = { message ->
            navController.navigateBack()
            showSuccessSnackbar(message)
        },
        manageMembers = { selectedContactEmailsIds ->
            navController.currentBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.SelectedContactEmailIds.key,
                selectedContactEmailsIds.map { it.id }
            )
            navController.navigate(Destination.Screen.ManageMembers.route)
        },
        exitToContactsWithNormMessage = { message ->
            navController.popBackStack(Destination.Screen.Contacts.route, inclusive = false)
            showNormSnackbar(message)
        },
        showErrorMessage = { message ->
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.CreateContactGroup.route) {
        ContactGroupFormScreen(
            actions,
            selectedContactEmailsIds = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<String>>(
                SavedStateKey.SelectedContactEmailIds.key
            )?.observeAsState()
        )
    }
    composable(route = Destination.Screen.EditContactGroup.route) {
        ContactGroupFormScreen(
            actions,
            selectedContactEmailsIds = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<String>>(
                SavedStateKey.SelectedContactEmailIds.key
            )?.observeAsState()
        )
    }
}

internal fun NavGraphBuilder.addManageMembers(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    val actions = ManageMembersScreen.Actions(
        onDone = { selectedContactEmailsIds ->
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.SelectedContactEmailIds.key,
                selectedContactEmailsIds.map { it.id }
            )
            navController.navigateBack()
        },
        onClose = { navController.navigateBack() },
        exitWithErrorMessage = { message ->
            navController.navigateBack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.ManageMembers.route) {
        ManageMembersScreen(
            actions,
            selectedContactEmailsIds = navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<List<String>>(SavedStateKey.SelectedContactEmailIds.key)
                ?.observeAsState()
        )
    }
}

internal fun NavGraphBuilder.addContactSearch(navController: NavHostController) {
    val actions = ContactSearchScreen.Actions(
        onContactSelected = { contactId ->
            navController.navigate(Destination.Screen.ContactDetails(contactId))
        },
        onContactGroupSelected = { labelId ->
            navController.navigate(Destination.Screen.ContactGroupDetails(labelId))
        },
        onClose = { navController.navigateBack() }
    )
    composable(route = Destination.Screen.ContactSearch.route) {
        ContactSearchScreen(
            actions
        )
    }
}

fun NavGraphBuilder.addPostSubscription(onClose: () -> Unit) {
    composable(route = Destination.Screen.PostSubscription.route) {
        PostSubscriptionScreen(
            onClose = onClose
        )
    }
}
