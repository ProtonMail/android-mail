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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.LockScreenActivity
import ch.protonmail.android.MainActivity
import ch.protonmail.android.R
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.UndoableOperationSnackbar
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.maildetail.presentation.ui.MessageDetail
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventType
import ch.protonmail.android.mailnotifications.presentation.EnablePushNotificationsDialog
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import ch.protonmail.android.mailsidebar.presentation.Sidebar
import ch.protonmail.android.mailupselling.presentation.ui.drivespotlight.DriveSpotlightScreen
import ch.protonmail.android.mailupselling.presentation.ui.npsfeedback.NPSFeedbackScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.navigation.listener.withDestinationChangedObservableEffect
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.route.addAccountSettings
import ch.protonmail.android.navigation.route.addAlternativeRoutingSetting
import ch.protonmail.android.navigation.route.addAutoDeleteSettings
import ch.protonmail.android.navigation.route.addAutoLockPinScreen
import ch.protonmail.android.navigation.route.addAutoLockSettings
import ch.protonmail.android.navigation.route.addCombinedContactsSetting
import ch.protonmail.android.navigation.route.addComposer
import ch.protonmail.android.navigation.route.addContactDetails
import ch.protonmail.android.navigation.route.addContactForm
import ch.protonmail.android.navigation.route.addContactGroupDetails
import ch.protonmail.android.navigation.route.addContactGroupForm
import ch.protonmail.android.navigation.route.addContactSearch
import ch.protonmail.android.navigation.route.addContacts
import ch.protonmail.android.navigation.route.addConversationDetail
import ch.protonmail.android.navigation.route.addConversationModeSettings
import ch.protonmail.android.navigation.route.addCustomizeToolbar
import ch.protonmail.android.navigation.route.addDeepLinkHandler
import ch.protonmail.android.navigation.route.addDefaultEmailSettings
import ch.protonmail.android.navigation.route.addDisplayNameSettings
import ch.protonmail.android.navigation.route.addDriveSpotlightRoute
import ch.protonmail.android.navigation.route.addEditSwipeActionsSettings
import ch.protonmail.android.navigation.route.addEntireMessageBody
import ch.protonmail.android.navigation.route.addExportLogsSettings
import ch.protonmail.android.navigation.route.addFolderForm
import ch.protonmail.android.navigation.route.addFolderList
import ch.protonmail.android.navigation.route.addLabelForm
import ch.protonmail.android.navigation.route.addLabelList
import ch.protonmail.android.navigation.route.addLanguageSettings
import ch.protonmail.android.navigation.route.addMailbox
import ch.protonmail.android.navigation.route.addManageMembers
import ch.protonmail.android.navigation.route.addMessageDetail
import ch.protonmail.android.navigation.route.addNPSFeedbackRoute
import ch.protonmail.android.navigation.route.addNotificationsSettings
import ch.protonmail.android.navigation.route.addParentFolderList
import ch.protonmail.android.navigation.route.addPrivacySettings
import ch.protonmail.android.navigation.route.addRemoveAccountDialog
import ch.protonmail.android.navigation.route.addSetMessagePassword
import ch.protonmail.android.navigation.route.addSettings
import ch.protonmail.android.navigation.route.addSignOutAccountDialog
import ch.protonmail.android.navigation.route.addSwipeActionsSettings
import ch.protonmail.android.navigation.route.addThemeSettings
import ch.protonmail.android.navigation.route.addUpsellingRoutes
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import ch.protonmail.android.uicomponents.snackbar.shouldGoInTwoRows
import io.sentry.compose.withSentryObservableEffect
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.network.domain.NetworkStatus

@Composable
@Suppress("ComplexMethod")
fun Home(
    activityActions: MainActivity.Actions,
    launcherActions: Launcher.Actions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
        .withSentryObservableEffect()
        .withDestinationChangedObservableEffect()
    var isNavHostReady by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = navBackStackEntry?.destination?.route

    val scaffoldState = rememberScaffoldState()
    val snackbarHostSuccessState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.SUCCESS) }
    val snackbarHostWarningState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.WARNING) }
    val snackbarHostNormState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.NORM) }
    val snackbarHostErrorState = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.ERROR) }
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val offlineSnackbarMessage = stringResource(id = R.string.you_are_offline)
    fun showOfflineSnackbar() = scope.launch {
        snackbarHostWarningState.showSnackbar(
            message = offlineSnackbarMessage,
            type = ProtonSnackbarType.WARNING
        )
    }

    ConsumableLaunchedEffect(state.networkStatusEffect) {
        if (it == NetworkStatus.Disconnected) {
            showOfflineSnackbar()
        }
    }

    // Ensure that the navigation graph is defined and the composable routes attached to it.
    LaunchedEffect(state.navigateToEffect, isNavHostReady) {
        if (!isNavHostReady) return@LaunchedEffect

        state.navigateToEffect.consume()?.let {
            viewModel.navigateTo(navController, it)
        }
    }

    val featureMissingSnackbarMessage = stringResource(id = R.string.feature_coming_soon)
    fun showFeatureMissingSnackbar() = scope.launch {
        snackbarHostNormState.showSnackbar(
            message = featureMissingSnackbarMessage,
            type = ProtonSnackbarType.NORM
        )
    }

    fun showErrorSnackbar(text: String) = scope.launch {
        snackbarHostErrorState.showSnackbar(
            message = text,
            type = ProtonSnackbarType.ERROR
        )
    }

    fun showNormalSnackbar(text: String) = scope.launch {
        snackbarHostErrorState.showSnackbar(
            message = text,
            type = ProtonSnackbarType.NORM
        )
    }

    val draftSavedText = stringResource(id = R.string.mailbox_draft_saved)
    val draftSavedDiscardText = stringResource(id = R.string.mailbox_draft_discard)
    fun showDraftSavedSnackbar(messageId: MessageId) = scope.launch {
        val result = snackbarHostSuccessState.showSnackbar(
            message = draftSavedText,
            type = ProtonSnackbarType.SUCCESS,
            actionLabel = draftSavedDiscardText

        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.discardDraft(messageId)
            SnackbarResult.Dismissed -> Unit
        }
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
    fun showSuccessSendingMessageSnackbar() = scope.launch {
        snackbarHostSuccessState.showSnackbar(message = successSendingMessageText, type = ProtonSnackbarType.SUCCESS)
    }

    val errorSendingMessageText = stringResource(id = R.string.mailbox_message_sending_error)
    val addressDisabledErrorMessageText = stringResource(id = R.string.mailbox_message_sending_error_address_disabled)
    val errorSendingMessageActionText = stringResource(id = R.string.mailbox_message_sending_error_action)
    fun showErrorSendingMessageSnackbar(error: SendingError?) = scope.launch {
        val message = when (error) {
            is SendingError.GenericLocalized -> error.apiMessage
            is SendingError.ExternalAddressSendDisabled -> error.apiMessage ?: errorSendingMessageText
            is SendingError.SendPreferences -> {
                val addressDisabled = error.errors.values.any {
                    it == SendingError.SendPreferencesError.AddressDisabled
                }
                if (addressDisabled) {
                    addressDisabledErrorMessageText
                } else {
                    errorSendingMessageText
                }
            }
            else -> errorSendingMessageText
        }

        val shouldShowAction = viewModel.shouldNavigateToDraftsOnSendingFailure(navController.currentDestination)
        val result = snackbarHostErrorState.showSnackbar(
            type = ProtonSnackbarType.ERROR,
            message = message,
            actionLabel = if (shouldShowAction) errorSendingMessageActionText else null,
            duration = if (shouldShowAction) SnackbarDuration.Long else SnackbarDuration.Short
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.navigateToDrafts(navController)
            SnackbarResult.Dismissed -> Unit
        }
    }

    val errorUploadAttachmentText = stringResource(id = R.string.mailbox_attachment_uploading_error)
    fun showErrorUploadAttachmentSnackbar() = scope.launch {
        snackbarHostErrorState.showSnackbar(message = errorUploadAttachmentText, type = ProtonSnackbarType.ERROR)
    }

    val labelSavedText = stringResource(id = R.string.label_saved)
    fun showLabelSavedSnackbar() = scope.launch {
        snackbarHostSuccessState.showSnackbar(message = labelSavedText, type = ProtonSnackbarType.SUCCESS)
    }

    val labelDeletedText = stringResource(id = R.string.label_deleted)
    fun showLabelDeletedSnackbar() = scope.launch {
        snackbarHostSuccessState.showSnackbar(message = labelDeletedText, type = ProtonSnackbarType.SUCCESS)
    }

    fun showUpsellingSnackbar(message: String) = scope.launch {
        snackbarHostNormState.showSnackbar(
            message = message,
            type = ProtonSnackbarType.NORM
        )
    }

    fun showUpsellingErrorSnackbar(message: String) = scope.launch {
        snackbarHostErrorState.showSnackbar(
            message = message,
            type = ProtonSnackbarType.ERROR
        )
    }

    val labelListErrorLoadingText = stringResource(id = R.string.label_list_loading_error)
    fun showLabelListErrorLoadingSnackbar() = scope.launch {
        snackbarHostErrorState.showSnackbar(message = labelListErrorLoadingText, type = ProtonSnackbarType.ERROR)
    }

    val undoActionEffect = remember { mutableStateOf(Effect.empty<ActionResult>()) }
    UndoableOperationSnackbar(snackbarHostState = snackbarHostNormState, actionEffect = undoActionEffect.value)
    fun showUndoableOperationSnackbar(actionResult: ActionResult) = scope.launch {
        undoActionEffect.value = Effect.of(actionResult)
    }

    ConsumableLaunchedEffect(state.messageSendingStatusEffect) { sendingStatus ->
        when (sendingStatus) {
            is MessageSendingStatus.MessageSent -> showSuccessSendingMessageSnackbar()
            is MessageSendingStatus.SendMessageError -> showErrorSendingMessageSnackbar(sendingStatus.error)
            is MessageSendingStatus.UploadAttachmentsError -> showErrorUploadAttachmentSnackbar()
            is MessageSendingStatus.None -> {}
        }
    }

    when (val notificationPermissionDialogState = state.notificationPermissionDialogState) {
        is NotificationPermissionDialogState.Hidden -> Unit
        is NotificationPermissionDialogState.Shown -> {
            EnablePushNotificationsDialog(
                state = notificationPermissionDialogState,
                onEnable = {
                    launcherActions.onRequestNotificationPermission()
                    viewModel.closeNotificationPermissionDialog()
                    viewModel.trackTelemetryEvent(
                        NotificationPermissionTelemetryEventType.NotificationPermissionDialogEnable(
                            notificationPermissionDialogState.type
                        )
                    )
                },
                onDismiss = {
                    viewModel.closeNotificationPermissionDialog()
                    viewModel.trackTelemetryEvent(
                        NotificationPermissionTelemetryEventType.NotificationPermissionDialogDismiss(
                            notificationPermissionDialogState.type
                        )
                    )
                }
            )
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
        drawerGesturesEnabled = currentDestinationRoute == Screen.Mailbox.route,
        snackbarHost = {
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostSuccess),
                protonSnackbarHostState = snackbarHostSuccessState
            )
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostWarning),
                protonSnackbarHostState = snackbarHostWarningState
            )
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostNormal),
                protonSnackbarHostState = snackbarHostNormState
            )
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHostError),
                protonSnackbarHostState = snackbarHostErrorState,
                customSnackbar = { data ->
                    ProtonSnackbar(
                        snackbarData = data,
                        type = snackbarHostErrorState.type,
                        actionOnNewLine = data.shouldGoInTwoRows()
                    )
                }
            )
        }
    ) { contentPadding ->
        Box(
            Modifier.padding(contentPadding)
        ) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Screen.Mailbox.route
            ) {
                // home
                addConversationDetail(
                    actions = ConversationDetail.Actions(
                        onExit = { notifyUserMessage ->
                            navController.navigateBack()
                            notifyUserMessage?.let { showUndoableOperationSnackbar(it) }
                            viewModel.recordViewOfMailboxScreen()
                        },
                        openMessageBodyLink = activityActions.openInActivityInNewTask,
                        openAttachment = activityActions.openIntentChooser,
                        handleProtonCalendarRequest = activityActions.openProtonCalendarIntentValues,
                        onAddLabel = { navController.navigate(Screen.LabelList.route) },
                        onAddFolder = { navController.navigate(Screen.FolderList.route) },
                        showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
                        onReply = { navController.navigate(Screen.MessageActionComposer(DraftAction.Reply(it))) },
                        onReplyAll = { navController.navigate(Screen.MessageActionComposer(DraftAction.ReplyAll(it))) },
                        onForward = { navController.navigate(Screen.MessageActionComposer(DraftAction.Forward(it))) },
                        onViewContactDetails = { navController.navigate(Screen.ContactDetails(it)) },
                        onAddContact = { basicContactInfo ->
                            navController.navigate(Screen.AddContact(basicContactInfo))
                        },
                        onComposeNewMessage = {
                            navController.navigate(
                                Screen.MessageActionComposer(
                                    DraftAction.ComposeToAddresses(
                                        listOf(it)
                                    )
                                )
                            )
                        },
                        navigateToCustomizeToolbar = {
                            navController.navigate(Screen.CustomizeToolbar.route)
                        },
                        openComposerForDraftMessage = { navController.navigate(Screen.EditDraftComposer(it)) },
                        showSnackbar = { message ->
                            scope.launch {
                                snackbarHostNormState.showSnackbar(
                                    message = message,
                                    type = ProtonSnackbarType.NORM
                                )
                            }
                        },
                        recordMailboxScreenView = { viewModel.recordViewOfMailboxScreen() },
                        onViewEntireMessageClicked =
                        { messageId, shouldShowEmbeddedImages, shouldShowRemoteContent, viewModePreference ->
                            navController.navigate(
                                Screen.EntireMessageBody(
                                    messageId, shouldShowEmbeddedImages, shouldShowRemoteContent, viewModePreference
                                )
                            )
                        }
                    )
                )
                addMailbox(
                    navController,
                    drawerState = scaffoldState.drawerState,
                    showOfflineSnackbar = { showOfflineSnackbar() },
                    showNormalSnackbar = { showNormalSnackbar(it) },
                    showErrorSnackbar = { showErrorSnackbar(it) },
                    onRequestNotificationPermission = launcherActions.onRequestNotificationPermission
                )
                addMessageDetail(
                    actions = MessageDetail.Actions(
                        onExit = { notifyUserMessage ->
                            navController.navigateBack()
                            notifyUserMessage?.let { showUndoableOperationSnackbar(it) }
                            viewModel.recordViewOfMailboxScreen()
                        },
                        openMessageBodyLink = activityActions.openInActivityInNewTask,
                        openAttachment = activityActions.openIntentChooser,
                        handleProtonCalendarRequest = activityActions.openProtonCalendarIntentValues,
                        onAddLabel = { navController.navigate(Screen.LabelList.route) },
                        onAddFolder = { navController.navigate(Screen.FolderList.route) },
                        showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
                        onReply = { navController.navigate(Screen.MessageActionComposer(DraftAction.Reply(it))) },
                        onReplyAll = { navController.navigate(Screen.MessageActionComposer(DraftAction.ReplyAll(it))) },
                        onForward = { navController.navigate(Screen.MessageActionComposer(DraftAction.Forward(it))) },
                        onViewContactDetails = { navController.navigate(Screen.ContactDetails(it)) },
                        onAddContact = { basicContactInfo ->
                            navController.navigate(Screen.AddContact(basicContactInfo))
                        },
                        onComposeNewMessage = {
                            navController.navigate(
                                Screen.MessageActionComposer(
                                    DraftAction.ComposeToAddresses(
                                        listOf(it)
                                    )
                                )
                            )
                        },
                        showSnackbar = { message ->
                            scope.launch {
                                snackbarHostNormState.showSnackbar(
                                    message = message,
                                    type = ProtonSnackbarType.NORM
                                )
                            }
                        },
                        navigateToCustomizeToolbar = {
                            navController.navigate(Screen.CustomizeToolbar.route)
                        },
                        recordMailboxScreenView = { viewModel.recordViewOfMailboxScreen() },
                        onViewEntireMessageClicked =
                        { messageId, shouldShowEmbeddedImages, shouldShowRemoteContent, viewModePreference ->
                            navController.navigate(
                                Screen.EntireMessageBody(
                                    messageId, shouldShowEmbeddedImages, shouldShowRemoteContent, viewModePreference
                                )
                            )
                        }
                    )
                )
                addEntireMessageBody(
                    navController,
                    onOpenMessageBodyLink = activityActions.openInActivityInNewTask
                )
                addComposer(
                    navController,
                    activityActions,
                    showDraftSavedSnackbar = { showDraftSavedSnackbar(it) },
                    showMessageSendingSnackbar = { showMessageSendingSnackbar() },
                    showMessageSendingOfflineSnackbar = { showMessageSendingOfflineSnackbar() },
                    showComposerV2 = viewModel.isComposerV2Enabled
                )

                addSetMessagePassword(navController)
                addSignOutAccountDialog(navController)
                addRemoveAccountDialog(navController)
                addSettings(navController)
                addLabelList(
                    navController,
                    showLabelListErrorLoadingSnackbar = { showLabelListErrorLoadingSnackbar() }
                )
                addLabelForm(
                    navController,
                    showLabelSavedSnackbar = { showLabelSavedSnackbar() },
                    showLabelDeletedSnackbar = { showLabelDeletedSnackbar() },
                    showUpsellingSnackbar = { showUpsellingSnackbar(it) },
                    showUpsellingErrorSnackbar = { showUpsellingErrorSnackbar(it) }
                )
                addFolderList(
                    navController,
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    }
                )
                addFolderForm(
                    navController,
                    showSuccessSnackbar = { message ->
                        scope.launch {
                            snackbarHostSuccessState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.SUCCESS
                            )
                        }
                    },
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    },
                    showNormSnackbar = { message ->
                        scope.launch {
                            snackbarHostNormState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                    }
                )
                addParentFolderList(
                    navController,
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    }
                )
                // settings
                addAccountSettings(navController, launcherActions, activityActions)
                addContacts(
                    navController,
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    },
                    showNormalSnackbar = {
                        showNormalSnackbar(it)
                    },
                    showFeatureMissingSnackbar = {
                        showFeatureMissingSnackbar()
                    }
                )
                addContactDetails(
                    navController,
                    showSuccessSnackbar = { message ->
                        scope.launch {
                            snackbarHostSuccessState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.SUCCESS
                            )
                        }
                    },
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    },
                    showFeatureMissingSnackbar = {
                        showFeatureMissingSnackbar()
                    }
                )
                addContactForm(
                    navController,
                    showSuccessSnackbar = { message ->
                        scope.launch {
                            snackbarHostSuccessState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.SUCCESS
                            )
                        }
                    },
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    }
                )
                addContactGroupDetails(
                    navController,
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    },
                    showNormSnackbar = { message ->
                        scope.launch {
                            snackbarHostNormState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                    }
                )
                addContactGroupForm(
                    navController,
                    showSuccessSnackbar = { message ->
                        scope.launch {
                            snackbarHostSuccessState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.SUCCESS
                            )
                        }
                    },
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    },
                    showNormSnackbar = { message ->
                        scope.launch {
                            snackbarHostNormState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.NORM
                            )
                        }
                    }
                )
                addManageMembers(
                    navController,
                    showErrorSnackbar = { message ->
                        scope.launch {
                            snackbarHostErrorState.showSnackbar(
                                message = message,
                                type = ProtonSnackbarType.ERROR
                            )
                        }
                    }
                )
                addContactSearch(
                    navController
                )
                addAlternativeRoutingSetting(navController)
                addCombinedContactsSetting(navController)
                addConversationModeSettings(navController)
                addAutoDeleteSettings(navController)
                addDefaultEmailSettings(navController)
                addDisplayNameSettings(navController)
                addEditSwipeActionsSettings(navController)
                addLanguageSettings(navController)
                addCustomizeToolbar(navController)
                addPrivacySettings(navController)
                addAutoLockSettings(navController)
                addAutoLockPinScreen(
                    onBack = { navController.navigateBack() },
                    onShowSuccessSnackbar = {
                        scope.launch {
                            snackbarHostSuccessState.showSnackbar(message = it, type = ProtonSnackbarType.SUCCESS)
                        }
                    },
                    activityActions = LockScreenActivity.Actions.Empty
                )
                addSwipeActionsSettings(navController)
                addThemeSettings(navController)
                addNotificationsSettings(navController)
                addExportLogsSettings(navController)
                addDeepLinkHandler(navController)
                addUpsellingRoutes(
                    UpsellingScreen.Actions.Empty.copy(
                        onDismiss = { navController.navigateBack() },
                        onUpgrade = { message -> scope.launch { showNormalSnackbar(message) } },
                        onError = { message -> scope.launch { showErrorSnackbar(message) } }
                    )
                )
                addDriveSpotlightRoute(
                    DriveSpotlightScreen.Actions(
                        onError = { message -> scope.launch { showErrorSnackbar(message) } },
                        onDismiss = { navController.navigateBack() }
                    )
                )
                addNPSFeedbackRoute(
                    NPSFeedbackScreen.Actions(
                        onSubmitted = { message ->
                            scope.launch { showNormalSnackbar(message) }
                            navController.navigateBack()
                        },
                        onDismiss = { navController.navigateBack() }
                    )
                )

                isNavHostReady = true
            }
        }
    }
}

private fun buildSidebarActions(navController: NavHostController, launcherActions: Launcher.Actions) =
    Sidebar.NavigationActions(
        onSignIn = launcherActions.onSignIn,
        onSignOut = { navController.navigate(Dialog.SignOut(it)) },
        onUpsell = { navController.navigate(Screen.Upselling.StandaloneNavbar.route) },
        onRemoveAccount = { navController.navigate(Dialog.RemoveAccount(it)) },
        onSwitchAccount = launcherActions.onSwitchAccount,
        onSettings = { navController.navigate(Screen.Settings.route) },
        onLabelList = { navController.navigate(Screen.LabelList.route) },
        onFolderList = { navController.navigate(Screen.FolderList.route) },
        onLabelAdd = { navController.navigate(Screen.CreateLabel.route) },
        onFolderAdd = { navController.navigate(Screen.CreateFolder.route) },
        onSubscription = launcherActions.onSubscription,
        onContacts = { navController.navigate(Screen.Contacts.route) },
        onReportBug = launcherActions.onReportBug
    )
