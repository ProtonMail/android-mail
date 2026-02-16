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

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.MainActivity
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.component.ProtonSnackbarHostState
import ch.protonmail.android.design.compose.component.ProtonSnackbarType
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.feature.spotlight.HomeFeatureSpotlightViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.SnackbarError
import ch.protonmail.android.mailcommon.presentation.SnackbarNormal
import ch.protonmail.android.mailcommon.presentation.SnackbarType
import ch.protonmail.android.mailcommon.presentation.SnackbarUndo
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.compose.UndoableOperationSnackbar
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.model.SendErrorReason
import ch.protonmail.android.mailcomposer.presentation.mapper.SendErrorReasonMapper
import ch.protonmail.android.maildetail.presentation.model.RawMessageDataType
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailnotifications.presentation.model.NotificationsPermissionStateType
import ch.protonmail.android.mailnotifications.presentation.viewmodel.NotificationsPermissionViewModel
import ch.protonmail.android.mailnotifications.ui.NotificationsPermissionBottomSheet
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingStepAction
import ch.protonmail.android.mailonboarding.presentation.viewmodel.OnboardingStepViewModel
import ch.protonmail.android.mailsession.data.mapper.toUserId
import ch.protonmail.android.mailsettings.domain.model.ToolbarType
import ch.protonmail.android.mailsidebar.presentation.Sidebar
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.viewmodel.BlackFridayModalUpsellViewModel
import ch.protonmail.android.mailupselling.presentation.viewmodel.SpringPromoModalUpsellViewModel
import ch.protonmail.android.navigation.deeplinks.DeepLinkNavigationEffect
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.onboarding.Onboarding
import ch.protonmail.android.navigation.route.addAppIconSettings
import ch.protonmail.android.navigation.route.addAppSettings
import ch.protonmail.android.navigation.route.addAutoLockIntervalSettings
import ch.protonmail.android.navigation.route.addAutoLockPinScreen
import ch.protonmail.android.navigation.route.addAutoLockSettings
import ch.protonmail.android.navigation.route.addBugReporting
import ch.protonmail.android.navigation.route.addCombinedContactsSetting
import ch.protonmail.android.navigation.route.addComposer
import ch.protonmail.android.navigation.route.addContactDetails
import ch.protonmail.android.navigation.route.addContactGroupDetails
import ch.protonmail.android.navigation.route.addContactSearch
import ch.protonmail.android.navigation.route.addContacts
import ch.protonmail.android.navigation.route.addConversationDetail
import ch.protonmail.android.navigation.route.addCustomizeToolbarSettings
import ch.protonmail.android.navigation.route.addEditSwipeActionsSettings
import ch.protonmail.android.navigation.route.addEmailSignatureSettings
import ch.protonmail.android.navigation.route.addEntireMessageBody
import ch.protonmail.android.navigation.route.addExportLogsSettings
import ch.protonmail.android.navigation.route.addFeatureFlagsOverrides
import ch.protonmail.android.navigation.route.addFeatureSpotlight
import ch.protonmail.android.navigation.route.addLanguageSettings
import ch.protonmail.android.navigation.route.addMailbox
import ch.protonmail.android.navigation.route.addMobileSignatureSettings
import ch.protonmail.android.navigation.route.addNotificationsSettings
import ch.protonmail.android.navigation.route.addPinDialog
import ch.protonmail.android.navigation.route.addPrivacySettings
import ch.protonmail.android.navigation.route.addRawMessageData
import ch.protonmail.android.navigation.route.addRemoveAccountDialog
import ch.protonmail.android.navigation.route.addSetMessagePassword
import ch.protonmail.android.navigation.route.addSettings
import ch.protonmail.android.navigation.route.addSignOutAccountDialog
import ch.protonmail.android.navigation.route.addSignatureMenuSettings
import ch.protonmail.android.navigation.route.addSwipeActionsSettings
import ch.protonmail.android.navigation.route.addThemeSettings
import ch.protonmail.android.navigation.route.addUpsellingRoutes
import ch.protonmail.android.navigation.route.addWebAccountSettings
import ch.protonmail.android.navigation.route.addWebEmailSettings
import ch.protonmail.android.navigation.route.addWebFolderAndLabelSettings
import ch.protonmail.android.navigation.route.addWebPrivacyAndSecuritySettings
import ch.protonmail.android.navigation.route.addWebSpamFilterSettings
import ch.protonmail.android.navigation.transitions.RouteTransitions
import ch.protonmail.android.uicomponents.fab.FabHost
import ch.protonmail.android.uicomponents.fab.ProtonFabHostState
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import io.sentry.compose.withSentryObservableEffect
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.android.core.accountmanager.presentation.manager.addAccountsManager
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountSwitchEvent
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ComplexMethod")
fun Home(
    activityActions: MainActivity.Actions,
    launcherActions: Launcher.Actions,
    viewModel: HomeViewModel = hiltViewModel(),
    onboardingStepViewModel: OnboardingStepViewModel = hiltViewModel(),
    notificationsPermissionViewModel: NotificationsPermissionViewModel = hiltViewModel(),
    blackFridayModalUpsellViewModel: BlackFridayModalUpsellViewModel = hiltViewModel(),
    springPromoModalUpsellViewModel: SpringPromoModalUpsellViewModel = hiltViewModel(),
    featureSpotlightViewModel: HomeFeatureSpotlightViewModel = hiltViewModel()
) {
    val navController = rememberNavController().withSentryObservableEffect()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = navBackStackEntry?.destination?.route

    val fabVisibleInLocation by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.route == Screen.Mailbox.route
        }
    }
    var isBottomBarVisible by remember { mutableStateOf(false) }

    val actionBarPadding by animateDpAsState(
        targetValue = if (isBottomBarVisible) MailDimens.BottomBarHeight else 0.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "actionBarPadding"
    )

    val updatePadding = { visible: Boolean -> isBottomBarVisible = visible }

    val fabHostState = remember { ProtonFabHostState() }
    val snackbarHost = remember { ProtonSnackbarHostState(defaultType = ProtonSnackbarType.NORM) }
    val isDrawerSwipeGestureEnabled = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle(HomeState.Initial)
    val onboardingEligibilityState by onboardingStepViewModel.onboardingEligibilityState.collectAsStateWithLifecycle()
    val notificationsPermissionsState by notificationsPermissionViewModel.state.collectAsStateWithLifecycle()
    val blackFridayEligibilityState by blackFridayModalUpsellViewModel.state.collectAsStateWithLifecycle()
    val springSaleEligibilityState by springPromoModalUpsellViewModel.state.collectAsStateWithLifecycle()
    val featureSpotlightState by featureSpotlightViewModel.state.collectAsStateWithLifecycle()

    val interstitialPriority by remember {
        derivedStateOf {
            resolveHomeInterstitialPriority(
                onboardingEligibilityState,
                notificationsPermissionsState,
                featureSpotlightState,
                blackFridayEligibilityState,
                springSaleEligibilityState
            )
        }
    }
    var bottomSheetType: BottomSheetType by remember { mutableStateOf(BottomSheetType.Onboarding) }
    var showBottomSheet by remember { mutableStateOf(false) }

    ConsumableLaunchedEffect(state.navigateToEffect) {
        viewModel.navigateTo(navController, it)
    }

    val featureMissingSnackbarMessage = stringResource(id = R.string.feature_coming_soon)
    fun showFeatureMissingSnackbar() {
        scope.launch {
            snackbarHost.showSnackbar(message = featureMissingSnackbarMessage, type = ProtonSnackbarType.NORM)
        }
    }

    fun showErrorSnackbar(text: String) = scope.launch {
        snackbarHost.showSnackbar(
            message = text,
            type = ProtonSnackbarType.ERROR
        )
    }

    fun showNormalSnackbar(text: String) = scope.launch {
        snackbarHost.showSnackbar(
            message = text,
            type = ProtonSnackbarType.NORM
        )
    }


    val undoActionEffect = remember { mutableStateOf(Effect.empty<ActionResult>()) }
    UndoableOperationSnackbar(snackbarHostState = snackbarHost, actionEffect = undoActionEffect.value)
    fun showUndoableOperationSnackbar(actionResult: ActionResult) = scope.launch {
        undoActionEffect.value = Effect.of(actionResult)
    }

    fun showSnackbar(snackbarType: SnackbarType) {
        when (snackbarType) {
            is SnackbarError -> showErrorSnackbar(snackbarType.message)
            is SnackbarNormal -> showNormalSnackbar(snackbarType.message)
            is SnackbarUndo -> showUndoableOperationSnackbar(snackbarType.result)
        }
    }

    val draftDiscardedMessage = stringResource(id = R.string.mailbox_draft_discarded)
    fun showDraftDiscardedSnackbar() = scope.launch {
        showNormalSnackbar(text = draftDiscardedMessage)
    }

    val draftSavedText = stringResource(id = R.string.mailbox_draft_saved)
    val draftSavedDiscardText = stringResource(id = R.string.mailbox_draft_discard)
    fun showDraftSavedSnackbar(messageId: MessageId) = scope.launch {
        val result = snackbarHost.showSnackbar(
            message = draftSavedText,
            type = ProtonSnackbarType.NORM,
            actionLabel = draftSavedDiscardText

        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                viewModel.discardDraft(messageId)
                showDraftDiscardedSnackbar()
            }

            SnackbarResult.Dismissed -> Unit
        }
    }

    val sendingMessageText = stringResource(id = R.string.mailbox_message_sending)
    fun showMessageSendingSnackbar() = scope.launch {
        snackbarHost.showSnackbar(message = sendingMessageText, type = ProtonSnackbarType.NORM)
    }

    val schedulingMessageText = stringResource(id = R.string.mailbox_message_scheduling)
    fun showMessageSchedulingSnackbar() = scope.launch {
        snackbarHost.showSnackbar(message = schedulingMessageText, type = ProtonSnackbarType.NORM)
    }

    val schedulingMessageOfflineText = stringResource(id = R.string.mailbox_message_scheduling_offline)
    fun showMessageSchedulingOfflineSnackbar() = scope.launch {
        snackbarHost.showSnackbar(message = schedulingMessageOfflineText, type = ProtonSnackbarType.NORM)
    }


    val undoActionText = stringResource(id = R.string.undo_button_label)
    val messageSentText = stringResource(id = R.string.mailbox_message_sending_success)
    fun showMessageSentWithUndoSnackbar(messageId: MessageId) = scope.launch {
        val result = snackbarHost.showSnackbar(
            type = ProtonSnackbarType.NORM,
            message = messageSentText,
            actionLabel = undoActionText,
            duration = SnackbarDuration.Indefinite
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoSendMessage(messageId)
            SnackbarResult.Dismissed -> Unit
        }
    }

    val messageScheduledText = stringResource(id = R.string.mailbox_message_scheduled_for_sending_success)
    fun showMessageScheduleSentWithUndoSnackbar(messageId: MessageId, formattedDate: String) = scope.launch {
        val message = messageScheduledText.format(formattedDate)
        val result = snackbarHost.showSnackbar(
            type = ProtonSnackbarType.NORM,
            message = message,
            actionLabel = undoActionText,
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoScheduleSendMessage(messageId)
            SnackbarResult.Dismissed -> Unit
        }
    }

    fun showMessageSentWithoutUndoSnackbar() = scope.launch {
        snackbarHost.showSnackbar(
            type = ProtonSnackbarType.NORM,
            message = messageSentText
        )
    }

    fun hideMessageSentSnackbar() = scope.launch {
        snackbarHost.snackbarHostState.currentSnackbarData?.dismiss()
    }

    val sendingMessageOfflineText = stringResource(id = R.string.mailbox_message_sending_offline)
    fun showMessageSendingOfflineSnackbar() = scope.launch {
        snackbarHost.showSnackbar(message = sendingMessageOfflineText, type = ProtonSnackbarType.NORM)
    }

    val context = LocalContext.current
    val errorSendingMessageActionText = stringResource(id = R.string.mailbox_message_sending_error_action)
    fun showErrorSendingMessageSnackbar(messageId: MessageId, reason: SendErrorReason) = scope.launch {
        val result = snackbarHost.showSnackbar(
            type = ProtonSnackbarType.ERROR,
            message = SendErrorReasonMapper.toSendErrorMessage(context, reason),
            actionLabel = errorSendingMessageActionText,
            duration = SnackbarDuration.Long
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.navigateToDraft(messageId)
            SnackbarResult.Dismissed -> Unit
        }
    }

    val undoSendErrorMessage = stringResource(R.string.undo_failure_message)
    fun showUndoSendErrorSnackbar() = showSnackbar(snackbarType = SnackbarError(undoSendErrorMessage))

    val cancellingScheduleSendMessage = stringResource(R.string.cancelling_scheduled_send_message)
    fun showCancellingScheduledMessageSnackbar() =
        showSnackbar(snackbarType = SnackbarNormal(cancellingScheduleSendMessage))

    ConsumableLaunchedEffect(state.messageSendingStatusEffect) { sendingStatus ->
        when (sendingStatus) {
            is MessageSendingStatus.MessageSentFinal -> {
                showMessageSentWithoutUndoSnackbar()
                viewModel.confirmMessageAsSeen(sendingStatus.messageId)
            }

            is MessageSendingStatus.SendMessageError ->
                showErrorSendingMessageSnackbar(sendingStatus.messageId, sendingStatus.reason)

            is MessageSendingStatus.NoStatus -> {}
            is MessageSendingStatus.MessageSentUndoable -> {
                showMessageSentWithUndoSnackbar(sendingStatus.messageId)

                // If a new event arrives, we need to make sure the dismissal is still happening.
                withContext(NonCancellable) {
                    delay(sendingStatus.timeRemainingForUndo.inWholeMilliseconds)
                    hideMessageSentSnackbar()
                    viewModel.confirmMessageAsSeen(sendingStatus.messageId)
                }
            }

            is MessageSendingStatus.MessageScheduledUndoable -> {
                val formattedDate = viewModel.formatTime(sendingStatus.deliveryTime)
                showMessageScheduleSentWithUndoSnackbar(sendingStatus.messageId, formattedDate)
                viewModel.confirmMessageAsSeen(sendingStatus.messageId)
            }

            is MessageSendingStatus.UndoSendError -> {
                showUndoSendErrorSnackbar()
            }

            is MessageSendingStatus.CancellingScheduleSend -> {
                showCancellingScheduledMessageSnackbar()
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Prevents the drawer to briefly flash by setting alpha to 0f, it's a workaround
    // due to M3 library DrawerState behaviour.
    // See https://issuetracker.google.com/issues/366272542#comment1
    var drawerAlpha by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(Unit) {
        delay(500.milliseconds)
        drawerAlpha = 1f
    }

    // To be kept in place as long as Drawer is part of the Home Scaffold
    LaunchedEffect(currentDestinationRoute) {
        if (currentDestinationRoute != null && currentDestinationRoute != Screen.Mailbox.route && drawerState.isOpen) {
            drawerState.close()
        }
    }

    DeepLinkNavigationEffect(
        navController = navController,
        onUserSwitched = { email ->
            Toast.makeText(
                context,
                context.getString(R.string.notification_switched_account, email),
                Toast.LENGTH_LONG
            ).show()
        }
    )

    var preventBottomSheetDismissal by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !preventBottomSheetDismissal }
    )

    val onBottomSheetDismissed: () -> Unit = {
        scope.launch { bottomSheetState.hide() }
            .invokeOnCompletion {
                when (bottomSheetType) {
                    is BottomSheetType.NotificationsPermissions -> {
                        notificationsPermissionViewModel.trackPermissionRequested()
                    }

                    BottomSheetType.Onboarding -> {
                        onboardingStepViewModel.submit(OnboardingStepAction.MarkOnboardingComplete)
                    }
                }

                if (!bottomSheetState.isVisible) {
                    showBottomSheet = false
                }
            }
    }

    LaunchedEffect(interstitialPriority) {
        // Home is a wrapper, so we can't rely on navigation here
        if (navController.currentDestination?.route != Screen.Mailbox.route) return@LaunchedEffect

        when (val priority = interstitialPriority) {
            is HomeInterstitialPriority.Loading -> Unit
            is HomeInterstitialPriority.Onboarding -> {
                bottomSheetType = BottomSheetType.Onboarding
                scope.launch { bottomSheetState.show() }
                    .invokeOnCompletion { showBottomSheet = true }
            }

            is HomeInterstitialPriority.NotificationsPermissions -> {
                bottomSheetType = BottomSheetType.NotificationsPermissions(priority.type)
                scope.launch { bottomSheetState.show() }
                    .invokeOnCompletion { showBottomSheet = true }
            }

            is HomeInterstitialPriority.FeatureSpotlight -> {
                navController.navigate(Screen.FeatureSpotlight.route)
            }

            is HomeInterstitialPriority.BlackFriday -> {
                navController.navigate(
                    Screen.FeatureUpselling(UpsellingEntryPoint.Feature.Navbar, priority.state.wave)
                )
            }

            is HomeInterstitialPriority.SpringPromo -> {
                springPromoModalUpsellViewModel.saveModalSeenTimestamp(priority.state.wave)
                navController.navigate(
                    Screen.FeatureUpselling(UpsellingEntryPoint.Feature.Navbar, priority.state.wave)
                )
            }

            is HomeInterstitialPriority.None -> {
                if (showBottomSheet) {
                    scope.launch { bottomSheetState.hide() }
                        .invokeOnCompletion { showBottomSheet = false }
                }
            }
        }
    }

    val eventLoopErrorString = stringResource(R.string.home_event_loop_error_snackbar)
    LaunchedEffect(Unit) {
        viewModel.eventLoopErrors.collect { error ->
            snackbarHost.showSnackbar(
                message = eventLoopErrorString,
                duration = SnackbarDuration.Long,
                type = ProtonSnackbarType.ERROR
            )
        }
    }

    val eventHandler: (AccountSwitchEvent) -> Unit = {
        when (it) {
            is AccountSwitchEvent.OnAccountSelected -> {
                launcherActions.onSwitchToAccount(it.userId.toUserId())
                navController.popBackStack(Screen.Mailbox.route, inclusive = false)
            }

            is AccountSwitchEvent.OnAddAccount ->
                launcherActions.onSignIn(null)

            is AccountSwitchEvent.OnManageAccount ->
                navController.navigate(Screen.AccountSettings.route)

            is AccountSwitchEvent.OnManageAccounts ->
                navController.navigate(Screen.AccountsManager.route)

            is AccountSwitchEvent.OnRemoveAccount ->
                navController.navigate(Dialog.RemoveAccount(it.userId.toUserId()))

            is AccountSwitchEvent.OnSignIn ->
                launcherActions.onSignIn(it.userId.toUserId())

            is AccountSwitchEvent.OnSignOut ->
                navController.navigate(Dialog.SignOut(it.userId.toUserId()))

            is AccountSwitchEvent.OnSetPrimaryAccountAvatar ->
                showErrorSnackbar(featureMissingSnackbarMessage)
        }
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        sheetState = bottomSheetState,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal) },
        sheetContent = {
            when (val type = bottomSheetType) {
                is BottomSheetType.NotificationsPermissions -> NotificationsPermissionBottomSheet(
                    onRequest = launcherActions.onRequestNotificationPermission,
                    uiModel = type.permissionsState.uiModel,
                    onDismiss = onBottomSheetDismissed
                ).also {
                    preventBottomSheetDismissal = false
                }

                is BottomSheetType.Onboarding -> Onboarding(
                    onExitOnboarding = {
                        onBottomSheetDismissed()
                        preventBottomSheetDismissal = false
                    },
                    onUpsellingError = {
                        showErrorSnackbar(it)
                    }
                ).also {
                    preventBottomSheetDismissal = true
                }
            }
        },
        dismissOnBack = false,
        onDismissed = onBottomSheetDismissed
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .alpha(drawerAlpha)
                        .widthIn(max = ProtonDimens.sideBarMaximumWidth),
                    drawerContentColor = ProtonTheme.colors.sidebarTextNorm,
                    drawerContainerColor = ProtonTheme.colors.sidebarBackground,
                    windowInsets = WindowInsets(0),
                    drawerShape = RectangleShape
                ) {
                    Sidebar(
                        drawerState = drawerState,
                        navigationActions = buildSidebarActions(navController, launcherActions)
                    )
                }

            },
            scrimColor = ProtonTheme.colors.blenderNorm,
            gesturesEnabled = currentDestinationRoute == Screen.Mailbox.route && isDrawerSwipeGestureEnabled.value
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                floatingActionButton = {
                    AnimatedVisibility(fabVisibleInLocation) {
                        FabHost(fabHostState = fabHostState)
                    }
                },
                snackbarHost = {
                    DismissableSnackbarHost(
                        modifier = Modifier.snackbarPadding(actionBarPadding, currentDestinationRoute),
                        protonSnackbarHostState = snackbarHost
                    )
                }
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Mailbox.route,
                    enterTransition = { RouteTransitions.defaultEnterTransition() },
                    exitTransition = { RouteTransitions.defaultExitTransition() },
                    popEnterTransition = { RouteTransitions.defaultPopEnterTransition() },
                    popExitTransition = { RouteTransitions.defaultPopExitTransition() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    // home
                    addConversationDetail(
                        navController = navController,
                        actions = ConversationDetail.Actions(
                            onExit = { notifyUserMessage ->
                                navController.navigateBack()
                                notifyUserMessage?.let { showUndoableOperationSnackbar(it) }
                                viewModel.recordViewOfMailboxScreen()
                            },
                            showUndoableOperationSnackbar = { notifyUserMessage ->
                                notifyUserMessage?.let {
                                    showUndoableOperationSnackbar(
                                        it
                                    )
                                }
                            },
                            openMessageBodyLink = activityActions.openInActivityInNewTask,
                            handleProtonCalendarRequest = activityActions.openProtonCalendarIntentValues,
                            onAddLabel = { navController.navigate(Screen.FolderAndLabelSettings.route) },
                            onAddFolder = { navController.navigate(Screen.FolderAndLabelSettings.route) },
                            showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
                            onReply = {
                                navController.navigate(Screen.MessageActionComposer(DraftAction.Reply(it)))
                            },
                            onReplyAll = {
                                navController.navigate(
                                    Screen.MessageActionComposer(
                                        DraftAction.ReplyAll(it)
                                    )
                                )
                            },
                            onForward = {
                                navController.navigate(
                                    Screen.MessageActionComposer(
                                        DraftAction.Forward(it)
                                    )
                                )
                            },
                            onViewContactDetails = { showFeatureMissingSnackbar() },
                            onAddContact = { _ ->
                                showFeatureMissingSnackbar()
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
                            openComposerForDraftMessage = { navController.navigate(Screen.EditDraftComposer(it)) },
                            showSnackbar = { message, type ->
                                scope.launch {
                                    snackbarHost.showSnackbar(
                                        message = message,
                                        type = type
                                    )
                                }
                            },
                            recordMailboxScreenView = { viewModel.recordViewOfMailboxScreen() },
                            onExitWithOpenInComposer = {
                                val popToMailboxOption = NavOptions.Builder()
                                    .setPopUpTo(Screen.Mailbox.route, false)
                                    .build()
                                navController.navigate(
                                    route = Screen.EditDraftComposer(it),
                                    navOptions = popToMailboxOption
                                )
                            },
                            onNavigateToUpselling = { entryPoint, type ->
                                navController.navigate(Screen.FeatureUpselling(entryPoint, type))
                            },
                            onActionBarVisibilityChanged = updatePadding,
                            onCustomizeToolbar = {
                                navController.navigate(Screen.EditToolbarScreen(ToolbarType.Conversation))
                            },
                            onCustomizeMessageToolbar = {
                                navController.navigate(Screen.EditToolbarScreen(ToolbarType.Message))
                            },
                            onViewEntireMessageClicked = { msgId, showInline, showRemote, vmPreference ->
                                navController.navigate(
                                    Screen.EntireMessageBody(
                                        messageId = msgId,
                                        shouldShowEmbeddedImages = showInline,
                                        shouldShowRemoteContent = showRemote,
                                        viewModePreference = vmPreference
                                    )
                                )
                            },
                            onViewMessageHeaders = { messageId ->
                                navController.navigate(Screen.RawMessageData(messageId, RawMessageDataType.Headers))
                            },
                            onViewMessageHtml = { messageId ->
                                navController.navigate(Screen.RawMessageData(messageId, RawMessageDataType.HTML))
                            }
                        )
                    )
                    addEntireMessageBody(
                        navController,
                        onOpenMessageBodyLink = activityActions.openInActivityInNewTask
                    )
                    addRawMessageData(
                        navController
                    )
                    addMailbox(
                        navController,
                        fabHostState,
                        openDrawerMenu = { scope.launch { drawerState.open() } },
                        setDrawerEnabled = { isDrawerSwipeGestureEnabled.value = it },
                        showSnackbar = { showSnackbar(it) },
                        onEvent = eventHandler,
                        showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
                        onActionBarVisibilityChanged = updatePadding,
                        onShowRatingBooster = activityActions.launchRatingBooster
                    )
                    addAccountsManager(
                        navController,
                        route = Screen.AccountsManager.route,
                        onCloseClicked = { navController.navigateBack() },
                        onEvent = eventHandler
                    )
                    addComposer(
                        navController,
                        activityActions,
                        showDraftSavedSnackbar = { showDraftSavedSnackbar(it) },
                        showMessageSendingSnackbar = { showMessageSendingSnackbar() },
                        showMessageSendingOfflineSnackbar = { showMessageSendingOfflineSnackbar() },
                        showMessageSchedulingSnackbar = { showMessageSchedulingSnackbar() },
                        showMessageSchedulingOfflineSnackbar = { showMessageSchedulingOfflineSnackbar() },
                        showDraftDiscardedSnackbar = { showDraftDiscardedSnackbar() }
                    )

                    addSetMessagePassword(navController)
                    addSignOutAccountDialog(navController)
                    addRemoveAccountDialog(navController)
                    addSettings(navController, activityActions)
                    addAppSettings(navController, showFeatureMissingSnackbar = { showFeatureMissingSnackbar() })
                    // settings
                    addWebAccountSettings(navController)
                    addWebEmailSettings(navController)
                    addWebFolderAndLabelSettings(navController)
                    addWebSpamFilterSettings(navController)
                    addWebPrivacyAndSecuritySettings(navController)
                    addContacts(
                        navController,
                        showErrorSnackbar = { message ->
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    message = message,
                                    type = ProtonSnackbarType.ERROR
                                )
                            }
                        },
                        showFeatureMissingSnackbar = {
                            showFeatureMissingSnackbar()
                        }
                    )
                    addContactDetails(
                        navController,
                        onShowErrorSnackbar = {
                            showErrorSnackbar(it)
                        },
                        onMessageContact = {
                            navController.navigate(
                                Screen.MessageActionComposer(
                                    DraftAction.ComposeToAddresses(
                                        listOf(it)
                                    )
                                )
                            )
                        },
                        showFeatureMissingSnackbar = {
                            showFeatureMissingSnackbar()
                        }
                    )
                    addContactSearch(
                        navController
                    )
                    addContactGroupDetails(
                        navController,
                        onShowErrorSnackbar = {
                            showErrorSnackbar(it)
                        },
                        onSendGroupMessage = {
                            navController.navigate(
                                Screen.MessageActionComposer(
                                    DraftAction.ComposeToAddresses(it)
                                )
                            )
                        },
                        onOpenContact = {
                            navController.navigate(Screen.ContactDetails(it))
                        },
                        showFeatureMissingSnackbar = {
                            showFeatureMissingSnackbar()
                        }
                    )
                    addCombinedContactsSetting(navController)
                    addEditSwipeActionsSettings(navController)
                    addLanguageSettings(navController)
                    addPrivacySettings(navController)
                    addAutoLockSettings(navController)
                    addMobileSignatureSettings(navController)
                    addEmailSignatureSettings(navController)
                    addSignatureMenuSettings(navController)
                    addAutoLockPinScreen(
                        onClose = { navController.navigateBack() },
                        onShowSuccessSnackbar = { showNormalSnackbar(it) }
                    )
                    addAppIconSettings(navController, onLearnMoreClick = activityActions.openInActivityInNewTask)
                    addPinDialog(navController)
                    addSwipeActionsSettings(navController)
                    addThemeSettings(navController)
                    addAutoLockIntervalSettings(navController)
                    addNotificationsSettings(navController)
                    addCustomizeToolbarSettings(navController)
                    addExportLogsSettings(navController)
                    addFeatureFlagsOverrides(navController)
                    addBugReporting(navController, onShowNormalSnackbar = { showNormalSnackbar(it) })
                    addUpsellingRoutes(
                        UpsellingScreen.Actions.Empty.copy(
                            onSuccess = { navController.navigateBack() },
                            onDismiss = { navController.navigateBack() },
                            onUpgrade = { message -> scope.launch { showNormalSnackbar(message) } },
                            onError = { message -> scope.launch { showErrorSnackbar(message) } }
                        )
                    )
                    addFeatureSpotlight(onBack = { navController.navigateBack() })
                }
            }
        }
    }
}

private fun buildSidebarActions(navController: NavHostController, launcherActions: Launcher.Actions) =
    Sidebar.NavigationActions(
        onSettings = { navController.navigate(Screen.Settings.route) },
        onLabelAdd = { navController.navigate(Screen.FolderAndLabelSettings.route) },
        onFolderAdd = { navController.navigate(Screen.FolderAndLabelSettings.route) },
        onSubscription = launcherActions.onSubscription,
        onContacts = { navController.navigate(Screen.Contacts.route) },
        onReportBug = { navController.navigate(Screen.BugReporting.route) },
        onUpselling = { entryPoint, type -> navController.navigate(Screen.FeatureUpselling(entryPoint, type)) }
    )

private sealed interface BottomSheetType {
    data object Onboarding : BottomSheetType
    data class NotificationsPermissions(val permissionsState: NotificationsPermissionStateType) : BottomSheetType
}

@Composable
private fun Modifier.snackbarPadding(
    actionBarPadding: Dp,
    currentDestinationRoute: String?,
    isMailboxRoute: Boolean = currentDestinationRoute == Screen.Mailbox.route
): Modifier {
    return this
        .padding(bottom = actionBarPadding)
        .then(
            if (isMailboxRoute) {
                Modifier // No padding - Snackbar is above FAB in Mailbox
            } else {
                Modifier.padding(
                    bottom = WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
            }
        )
}
