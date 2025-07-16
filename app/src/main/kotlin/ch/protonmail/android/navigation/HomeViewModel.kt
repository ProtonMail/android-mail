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

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import ch.protonmail.android.mailcommon.data.file.getShareInfo
import ch.protonmail.android.mailcommon.data.file.isStartedFromLauncher
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import ch.protonmail.android.mailcommon.domain.model.encode
import ch.protonmail.android.mailcommon.domain.model.isNotEmpty
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.annotation.IsComposerV2Enabled
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.ResetSendingMessagesStatus
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetDraftLabelId
import ch.protonmail.android.mailmailbox.domain.usecase.RecordMailboxScreenView
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventType
import ch.protonmail.android.mailnotifications.domain.usecase.SavePermissionDialogTimestamp
import ch.protonmail.android.mailnotifications.domain.usecase.SaveShouldStopShowingPermissionDialog
import ch.protonmail.android.mailnotifications.domain.usecase.ShouldShowNotificationPermissionDialog
import ch.protonmail.android.mailnotifications.domain.usecase.TrackNotificationPermissionTelemetryEvent
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogState
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogType
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.share.ShareIntentObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val observeSendingMessagesStatus: ObserveSendingMessagesStatus,
    private val recordMailboxScreenView: RecordMailboxScreenView,
    private val resetSendingMessageStatus: ResetSendingMessagesStatus,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val discardDraft: DiscardDraft,
    private val shouldShowNotificationPermissionDialog: ShouldShowNotificationPermissionDialog,
    private val savePermissionDialogTimestamp: SavePermissionDialogTimestamp,
    private val saveShouldStopShowingPermissionDialog: SaveShouldStopShowingPermissionDialog,
    private val trackNotificationPermissionTelemetryEvent: TrackNotificationPermissionTelemetryEvent,
    @IsComposerV2Enabled val isComposerV2Enabled: Boolean,
    private val getDraftLabelId: GetDraftLabelId,
    observePrimaryUser: ObservePrimaryUser,
    shareIntentObserver: ShareIntentObserver
) : ViewModel() {

    private val primaryUser = observePrimaryUser().filterNotNull()

    private val mutableState = MutableStateFlow(HomeState.Initial)

    val state: StateFlow<HomeState> = mutableState

    init {
        observeNetworkStatus().onEach { networkStatus ->
            if (networkStatus == NetworkStatus.Disconnected) {
                delay(NetworkStatusUpdateDelay)
                emitNewStateFor(networkManager.networkStatus)
            } else {
                emitNewStateFor(networkStatus)
            }
        }.launchIn(viewModelScope)

        primaryUser.flatMapLatest { user ->
            observeSendingMessagesStatus(user.userId)
        }.onEach {
            emitNewStateFor(it)
            resetSendingMessageStatus(primaryUser.first().userId)
        }.launchIn(viewModelScope)

        shareIntentObserver()
            .onEach { intent ->
                emitNewStateForIntent(intent)
            }
            .launchIn(viewModelScope)

        showNotificationPermissionDialogIfNeeded(isMessageSent = false)
    }

    fun navigateTo(navController: NavController, route: String) {
        navController.navigate(route = route)
    }

    /**
     * Navigate to Drafts only when:
     * - we are outside of Mailbox
     * - we are in Mailbox but not in Drafts
     */
    fun shouldNavigateToDraftsOnSendingFailure(currentNavDestination: NavDestination?): Boolean =
        currentNavDestination?.route != Destination.Screen.Mailbox.route ||
            selectedMailLabelId.flow.value.labelId != MailLabelId.System.AllDrafts.labelId &&
            selectedMailLabelId.flow.value.labelId != MailLabelId.System.Drafts.labelId

    fun navigateToDrafts(navController: NavController) {
        if (navController.currentDestination?.route != Destination.Screen.Mailbox.route) {
            navController.popBackStack(Destination.Screen.Mailbox.route, inclusive = false)
        }
        viewModelScope.launch {
            primaryUser.firstOrNull()?.let { user ->
                selectedMailLabelId.set(getDraftLabelId.invoke(user.userId))
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun discardDraft(messageId: MessageId) {
        viewModelScope.launch {
            primaryUser.firstOrNull()?.let {
                discardDraft(it.userId, messageId)
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun recordViewOfMailboxScreen() = recordMailboxScreenView()

    fun closeNotificationPermissionDialog() {
        mutableState.update { currentState ->
            currentState.copy(
                notificationPermissionDialogState = NotificationPermissionDialogState.Hidden
            )
        }
    }

    fun trackTelemetryEvent(eventType: NotificationPermissionTelemetryEventType) =
        trackNotificationPermissionTelemetryEvent(eventType)

    private fun emitNewStateFor(messageSendingStatus: MessageSendingStatus) {
        if (messageSendingStatus == MessageSendingStatus.None) {
            // Emitting a None status to UI would override the previously emitted effect and cause snack not to show
            return
        }

        if (messageSendingStatus == MessageSendingStatus.MessageSent) {
            showNotificationPermissionDialogIfNeeded(isMessageSent = true)
        }

        mutableState.update { currentState ->
            currentState.copy(
                messageSendingStatusEffect = Effect.of(messageSendingStatus)
            )
        }
    }

    private fun emitNewStateForIntent(intent: Intent) {
        if (intent.isStartedFromLauncher()) {
            mutableState.update { currentState ->
                currentState.copy(startedFromLauncher = true)
            }
        } else if (!mutableState.value.startedFromLauncher) {
            val intentShareInfo = intent.getShareInfo()
            if (intentShareInfo.isNotEmpty()) {
                emitNewStateForShareVia(intentShareInfo)
            }
        } else {
            Timber.d("Share intent is not processed as this instance was started from launcher!")
        }
    }

    private fun emitNewStateForShareVia(intentShareInfo: IntentShareInfo) {
        mutableState.update { currentState ->
            currentState.copy(
                navigateToEffect = Effect.of(
                    Destination.Screen.ShareFileComposer(DraftAction.PrefillForShare(intentShareInfo.encode()))
                )
            )
        }
    }

    private fun emitNewStateFor(networkStatus: NetworkStatus) {
        mutableState.update { currentState ->
            currentState.copy(networkStatusEffect = Effect.of(networkStatus))
        }
    }

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()

    private fun showNotificationPermissionDialogIfNeeded(isMessageSent: Boolean) {
        viewModelScope.launch {
            if (!shouldShowNotificationPermissionDialog(System.currentTimeMillis(), isMessageSent)) return@launch

            val notificationPermissionDialogType = if (isMessageSent) {
                NotificationPermissionDialogType.PostSending
            } else {
                NotificationPermissionDialogType.PostOnboarding
            }

            mutableState.update { currentState ->
                currentState.copy(
                    notificationPermissionDialogState = NotificationPermissionDialogState.Shown(
                        type = notificationPermissionDialogType
                    )
                )
            }

            trackTelemetryEvent(
                NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed(
                    notificationPermissionDialogType
                )
            )

            if (isMessageSent) {
                saveShouldStopShowingPermissionDialog()
            } else {
                savePermissionDialogTimestamp(System.currentTimeMillis())
            }
        }
    }

    companion object {

        const val NetworkStatusUpdateDelay = 5000L
    }
}
