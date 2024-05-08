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
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.ResetSendingMessagesStatus
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
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
import kotlinx.coroutines.launch
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val observeSendingMessagesStatus: ObserveSendingMessagesStatus,
    private val resetSendingMessageStatus: ResetSendingMessagesStatus,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val discardDraft: DiscardDraft,
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
        selectedMailLabelId.set(MailLabelId.System.Drafts)
    }

    fun discardDraft(messageId: MessageId) {
        viewModelScope.launch {
            primaryUser.firstOrNull()?.let {
                discardDraft(it.userId, messageId)
            } ?: Timber.e("Primary user is not available!")
        }
    }

    private fun emitNewStateFor(messageSendingStatus: MessageSendingStatus) {
        if (messageSendingStatus == MessageSendingStatus.None) {
            // Emitting a None status to UI would override the previously emitted effect and cause snack not to show
            return
        }
        val currentState = state.value
        mutableState.value = currentState.copy(messageSendingStatusEffect = Effect.of(messageSendingStatus))
    }

    private fun emitNewStateForIntent(intent: Intent) {
        val currentState = state.value
        if (intent.isStartedFromLauncher()) {

            mutableState.value = currentState.copy(startedFromLauncher = true)
        } else if (!currentState.startedFromLauncher) {
            val intentShareInfo = intent.getShareInfo()
            if (intentShareInfo.isNotEmpty()) {
                emitNewStateForShareVia(intentShareInfo)
            }
        } else {
            Timber.d("Share intent is not processed as this instance was started from launcher!")
        }
    }

    private fun emitNewStateForShareVia(intentShareInfo: IntentShareInfo) {
        val currentState = state.value
        mutableState.value = currentState.copy(
            navigateToEffect = Effect.of(
                Destination.Screen.ShareFileComposer(DraftAction.PrefillForShare(intentShareInfo.encode()))
            )
        )
    }

    private fun emitNewStateFor(networkStatus: NetworkStatus) {
        val currentState = state.value
        mutableState.value = currentState.copy(networkStatusEffect = Effect.of(networkStatus))
    }

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()

    companion object {

        const val NetworkStatusUpdateDelay = 5000L
    }
}
