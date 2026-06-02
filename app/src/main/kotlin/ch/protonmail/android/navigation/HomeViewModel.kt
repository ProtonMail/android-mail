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
import androidx.navigation.NavOptions
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.usecase.FormatFullDate
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.usecase.DiscardDraft
import ch.protonmail.android.mailcomposer.domain.usecase.MarkMessageSendingStatusesAsSeen
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingMessagesStatus
import ch.protonmail.android.mailcomposer.domain.usecase.UndoSendMessage
import ch.protonmail.android.mailmailbox.domain.usecase.RecordMailboxScreenView
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.CancelScheduleSendMessage
import ch.protonmail.android.mailsession.domain.eventloop.EventLoopErrorSignal
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.navigation.mapper.IntentMapper
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.model.NavigationEffect
import ch.protonmail.android.navigation.reducer.HomeNavigationEventsReducer
import ch.protonmail.android.navigation.share.NewIntentObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Instant

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeSendingMessagesStatus: ObserveSendingMessagesStatus,
    private val recordMailboxScreenView: RecordMailboxScreenView,
    private val discardDraft: DiscardDraft,
    private val undoSendMessage: UndoSendMessage,
    private val markMessageSendingStatusesAsSeen: MarkMessageSendingStatusesAsSeen,
    private val formatFullDate: FormatFullDate,
    private val cancelScheduleSendMessage: CancelScheduleSendMessage,
    eventLoopErrorSignal: EventLoopErrorSignal,
    observePrimaryUserId: ObservePrimaryUserId,
    newIntentObserver: NewIntentObserver,
    private val intentMapper: IntentMapper,
    private val navigationEventsReducer: HomeNavigationEventsReducer
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow(HomeState.Initial)

    val state: StateFlow<HomeState> = mutableState

    val eventLoopErrors = eventLoopErrorSignal.observeEventLoopErrors()

    init {
        primaryUserId.flatMapLatest { userId ->
            observeSendingMessagesStatus(userId)
        }.onEach {
            emitNewStateFor(it)
        }.launchIn(viewModelScope)

        newIntentObserver()
            .onEach { emitNewStateForIntent(it) }
            .launchIn(viewModelScope)
    }

    fun navigateTo(navController: NavController, navigationEffect: NavigationEffect) {
        when (navigationEffect) {
            is NavigationEffect.NavigateTo -> navController.navigate(
                route = navigationEffect.route,
                navigationEffect.navOptions
            )

            is NavigationEffect.PopBackStack -> navController.popBackStack()
            is NavigationEffect.PopBackStackTo -> navController.popBackStack(
                route = navigationEffect.route,
                inclusive = navigationEffect.inclusive
            )

            is NavigationEffect.NavigateToUri -> navController.navigate(
                navigationEffect.uri,
                navigationEffect.navOptions
            )
        }
    }

    fun navigateToDraft(messageId: MessageId) {
        navigateToDraftInComposer(messageId)
    }

    fun discardDraft(messageId: MessageId) {
        viewModelScope.launch {
            primaryUserId.firstOrNull()?.let { userId ->
                discardDraft(userId, messageId)
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun undoSendMessage(messageId: MessageId) {
        viewModelScope.launch {
            primaryUserId.firstOrNull()?.let {
                undoSendMessage(it, messageId)
                    .onRight { navigateToDraftInComposer(messageId) }
                    .onLeft { error -> showUndoSendError(messageId, error) }
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun undoScheduleSendMessage(messageId: MessageId) {
        viewModelScope.launch {
            primaryUserId.firstOrNull()?.let { userId ->
                showCancellingScheduleSend(messageId)
                cancelScheduleSendMessage(userId, messageId)
                    .onRight { navigateToDraftInComposer(messageId) }
                    .onLeft { showUndoSendError(messageId) }
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun confirmMessageAsSeen(messageId: MessageId) {
        viewModelScope.launch {
            primaryUserId.firstOrNull()?.let {
                markMessageSendingStatusesAsSeen(it, listOf(messageId))
            } ?: Timber.e("Primary user is not available!")
        }
    }

    fun recordViewOfMailboxScreen() = recordMailboxScreenView()

    fun formatTime(time: Instant) = formatFullDate(time)

    private fun showCancellingScheduleSend(messageId: MessageId) {
        mutableState.update {
            it.copy(messageSendingStatusEffect = Effect.of(MessageSendingStatus.CancellingScheduleSend(messageId)))
        }
    }

    private fun showUndoSendError(messageId: MessageId, error: UndoSendError? = null) {
        val badRequestMessage = (error as? UndoSendError.BadRequest)?.message
        mutableState.update {
            it.copy(
                messageSendingStatusEffect = Effect.of(
                    MessageSendingStatus.UndoSendError(messageId, badRequestMessage)
                )
            )
        }
    }

    private fun navigateToDraftInComposer(messageId: MessageId) {
        val popUpToMailbox = NavOptions.Builder()
            .setPopUpTo(route = Destination.Screen.Mailbox.route, inclusive = false, saveState = false)
            .build()
        val navigateToComposer = NavigationEffect.NavigateTo(
            route = Destination.Screen.EditDraftComposer(messageId),
            navOptions = popUpToMailbox
        )
        mutableState.update { it.copy(navigateToEffect = Effect.of(navigateToComposer)) }
    }

    private fun emitNewStateFor(messageSendingStatus: MessageSendingStatus) {
        if (messageSendingStatus is MessageSendingStatus.NoStatus) {
            // Emitting a None status to UI would override the previously emitted effect and cause snack not to show
            return
        }
        mutableState.update { it.copy(messageSendingStatusEffect = Effect.of(messageSendingStatus)) }
    }

    private fun emitNewStateForIntent(intent: Intent) {
        Timber.tag("intent-navigation").d("Processing intent: $intent")

        val navIntent = intentMapper.map(intent)

        mutableState.update { current ->
            navigationEventsReducer.reduce(current, navIntent)
        }
    }
}
