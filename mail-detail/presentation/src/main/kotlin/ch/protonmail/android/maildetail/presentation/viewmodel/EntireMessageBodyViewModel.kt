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

package ch.protonmail.android.maildetail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyAction
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.util.kotlin.deserializeOrNull
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EntireMessageBodyViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val getDecryptedMessageBody: GetDecryptedMessageBody,
    private val getEmbeddedImageAvoidDuplicatedExecution: GetEmbeddedImageAvoidDuplicatedExecution,
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper,
    private val observeMessage: ObserveMessage,
    private val observePrivacySettings: ObservePrivacySettings,
    private val savedStateHandle: SavedStateHandle,
    private val updateLinkConfirmationSetting: UpdateLinkConfirmationSetting
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )
        .filterNotNull()

    private val messageId = requireMessageId()
    private val inputParams = requireInputParams()

    private val mutableState = MutableStateFlow(EntireMessageBodyState.Initial)
    val state: StateFlow<EntireMessageBodyState> = mutableState.asStateFlow()

    init {
        getMessageBody(messageId)
        observeMessage()
        observePrivacySettings()
    }

    fun submit(action: EntireMessageBodyAction) = when (action) {
        is EntireMessageBodyAction.MessageBodyLinkClicked -> handleMessageBodyLinkClicked(action)
        is EntireMessageBodyAction.DoNotAskLinkConfirmationAgain -> handleDoNotAskLinkConfirmationAgain()
        is EntireMessageBodyAction.ReloadMessageBody -> getMessageBody(messageId)
    }

    fun loadEmbeddedImage(messageId: MessageId, contentId: String): GetEmbeddedImageResult? {
        return runBlocking {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId = primaryUserId.first(),
                messageId = messageId,
                contentId = contentId,
                coroutineContext = viewModelScope.coroutineContext
            )
        }
    }

    private fun requireMessageId(): MessageId {
        val messageIdParam = savedStateHandle.get<String>(EntireMessageBodyScreen.MESSAGE_ID_KEY)
            ?: throw IllegalStateException("No Message id given")

        return MessageId(messageIdParam)
    }

    private fun requireInputParams(): EntireMessageBodyScreen.InputParams {
        return savedStateHandle.get<String>(
            EntireMessageBodyScreen.INPUT_PARAMS_KEY
        )?.deserializeOrNull() ?: throw IllegalStateException("No input params given")
    }

    private fun getMessageBody(messageId: MessageId) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            getDecryptedMessageBody(userId, messageId).fold(
                ifLeft = { getDecryptedMessageBodyError ->
                    when (getDecryptedMessageBodyError) {
                        is GetDecryptedMessageBodyError.Decryption -> {
                            mutableState.value = mutableState.value.copy(
                                messageBodyState = MessageBodyState.Error.Decryption(
                                    encryptedMessageBody = messageBodyUiModelMapper.toUiModel(
                                        getDecryptedMessageBodyError
                                    ).copy(
                                        shouldShowEmbeddedImages = inputParams.shouldShowEmbeddedImages,
                                        shouldShowRemoteContent = inputParams.shouldShowRemoteContent,
                                        viewModePreference = inputParams.viewModePreference
                                    )
                                )
                            )
                        }
                        is GetDecryptedMessageBodyError.Data -> {
                            mutableState.value = mutableState.value.copy(
                                messageBodyState = MessageBodyState.Error.Data(
                                    isNetworkError = getDecryptedMessageBodyError.dataError == DataError.Remote.Http(
                                        NetworkError.NoNetwork
                                    )
                                )
                            )
                        }
                    }
                },
                ifRight = {
                    mutableState.value = mutableState.value.copy(
                        messageBodyState = MessageBodyState.Data(
                            messageBodyUiModel = messageBodyUiModelMapper.toUiModel(userId, it).copy(
                                shouldShowEmbeddedImages = inputParams.shouldShowEmbeddedImages,
                                shouldShowRemoteContent = inputParams.shouldShowRemoteContent,
                                viewModePreference = inputParams.viewModePreference
                            )
                        )
                    )
                }
            )
        }
    }

    private fun observeMessage() = primaryUserId.flatMapLatest { userId ->
        observeMessage(userId, messageId).mapLatest { either ->
            either.fold(
                ifLeft = { Timber.d("Error getting message $messageId for user $userId") },
                ifRight = { message ->
                    mutableState.value = mutableState.value.copy(
                        subject = message.subject,
                        requestPhishingLinkConfirmation = message.isPhishing()
                    )
                }
            )
        }
    }.launchIn(viewModelScope)

    private fun observePrivacySettings() = primaryUserId.flatMapLatest { userId ->
        observePrivacySettings(userId).mapLatest { either ->
            either.fold(
                ifLeft = { Timber.e("Error getting Privacy Settings for user: $userId") },
                ifRight = { privacySettings ->
                    mutableState.value = mutableState.value.copy(
                        requestLinkConfirmation = privacySettings.requestLinkConfirmation
                    )
                }
            )
        }
    }.launchIn(viewModelScope)

    private fun handleMessageBodyLinkClicked(action: EntireMessageBodyAction.MessageBodyLinkClicked) {
        mutableState.value = mutableState.value.copy(
            openMessageBodyLinkEffect = Effect.of(action.uri)
        )
    }

    private fun handleDoNotAskLinkConfirmationAgain() {
        viewModelScope.launch { updateLinkConfirmationSetting(false) }
    }
}
