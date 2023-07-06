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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses.Error
import ch.protonmail.android.mailcomposer.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSender
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerEvent
import ch.protonmail.android.mailcomposer.presentation.model.ComposerOperation
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val storeDraftWithBody: StoreDraftWithBody,
    private val storeDraftWithSender: StoreDraftWithSender,
    private val reducer: ComposerReducer,
    private val isValidEmailAddress: IsValidEmailAddress,
    private val getPrimaryAddress: GetPrimaryAddress,
    private val getComposerSenderAddresses: GetComposerSenderAddresses,
    observePrimaryUserId: ObservePrimaryUserId,
    provideNewDraftId: ProvideNewDraftId
) : ViewModel() {

    private val messageId = MessageId(provideNewDraftId().id)
    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow(ComposerDraftState.empty(provideNewDraftId()))
    val state: StateFlow<ComposerDraftState> = mutableState

    init {
        primaryUserId.onEach { userId ->
            getPrimaryAddress(userId)
                .onLeft { emitNewStateFor(ComposerEvent.ErrorLoadingDefaultSenderAddress) }
                .onRight { emitNewStateFor(ComposerEvent.DefaultSenderReceived(SenderUiModel(it.email))) }
        }.launchIn(viewModelScope)
    }

    internal fun submit(action: ComposerAction) {
        viewModelScope.launch {
            when (action) {
                is ComposerAction.DraftBodyChanged -> emitNewStateFor(onDraftBodyChanged(action))
                is ComposerAction.SenderChanged -> emitNewStateFor(onSenderChanged(action))
                is ComposerAction.ChangeSenderRequested -> emitNewStateFor(onChangeSender())
                else -> emitNewStateFor(action)
            }
        }
    }

    fun validateEmailAddress(emailAddress: String): Boolean = isValidEmailAddress(emailAddress)

    private suspend fun onSenderChanged(action: ComposerAction.SenderChanged): ComposerOperation =
        storeDraftWithSender(messageId, SenderEmail(action.sender.email), primaryUserId.first()).fold(
            ifLeft = {
                Timber.e("Store draft $messageId with new sender ${action.sender.email} failed")
                ComposerEvent.ErrorStoringDraftSenderAddress
            },
            ifRight = { action }
        )

    private suspend fun onDraftBodyChanged(action: ComposerAction.DraftBodyChanged): ComposerOperation =
        storeDraftWithBody(messageId, action.draftBody, currentSenderEmail(), primaryUserId.first()).fold(
            ifLeft = { ComposerEvent.ErrorStoringDraftBody },
            ifRight = { ComposerAction.DraftBodyChanged(action.draftBody) }
        )

    private fun currentSenderEmail() = SenderEmail(state.value.fields.sender.email)

    private suspend fun onChangeSender() = getComposerSenderAddresses().fold(
        ifLeft = { changeSenderError ->
            when (changeSenderError) {
                Error.UpgradeToChangeSender -> ComposerEvent.ErrorFreeUserCannotChangeSender
                Error.FailedDeterminingUserSubscription,
                Error.FailedGettingPrimaryUser -> ComposerEvent.ErrorVerifyingPermissionsToChangeSender
            }
        },
        ifRight = { userAddresses ->
            ComposerEvent.SenderAddressesReceived(userAddresses.map { SenderUiModel(it.email) })
        }
    )

    private fun emitNewStateFor(operation: ComposerOperation) {
        val currentState = state.value
        mutableState.value = reducer.newStateFrom(currentState, operation)
    }

}
