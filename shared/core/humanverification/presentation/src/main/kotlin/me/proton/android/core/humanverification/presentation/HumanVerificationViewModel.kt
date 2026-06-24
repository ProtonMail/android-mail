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

package me.proton.android.core.humanverification.presentation

import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.UiEventFlow
import ch.protonmail.android.mailsession.domain.model.RustApiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.proton.android.core.humanverification.domain.ChallengeNotifierCallback
import me.proton.android.core.humanverification.presentation.HV3ResponseMessage.MessageType
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_uniffi.ApiConfig
import uniffi.mail_uniffi.AppDetails
import uniffi.mail_uniffi.ChallengeLoader
import uniffi.mail_uniffi.HumanVerificationScreenId
import uniffi.mail_uniffi.HumanVerificationStatus
import uniffi.mail_uniffi.HumanVerificationViewLoadingStatus
import uniffi.mail_uniffi.NewChallengeLoaderResult
import uniffi.mail_uniffi.newChallengeLoader
import uniffi.mail_uniffi.recordHumanVerificationResult
import uniffi.mail_uniffi.recordHumanVerificationScreenView
import uniffi.mail_uniffi.recordHumanVerificationViewLoadingResult
import javax.inject.Inject

@HiltViewModel
class HumanVerificationViewModel @Inject constructor(
    private val isWebViewDebuggingEnabled: IsWebViewDebuggingEnabled,
    private val updateHumanVerificationURL: UpdateHumanVerificationURL,
    private val challengeNotifierCallback: ChallengeNotifierCallback,
    private val rustApiConfig: RustApiConfig
) : BaseViewModel<HumanVerificationAction, HumanVerificationViewState>(
    initialAction = HumanVerificationAction.NoOp,
    initialState = HumanVerificationViewState.Idle
) {
    val uiEvent = UiEventFlow<HumanVerificationViewEvent>()

    override fun onAction(action: HumanVerificationAction): Flow<HumanVerificationViewState> = when (action) {
        is HumanVerificationAction.NoOp -> flowOf(HumanVerificationViewState.Idle)
        is HumanVerificationAction.Load -> with(action) {
            onLoad(
                url = url,
                originalHost = action.originalHost,
                alternativeHost = action.alternativeHost,
                defaultCountry = defaultCountry,
                recoveryPhone = recoveryPhone,
                locale = locale,
                headers = headers
            )
        }

        is HumanVerificationAction.Verify -> onWebviewEvent(action.result)
        is HumanVerificationAction.Cancel -> onCancel()
        is HumanVerificationAction.Failure.ResourceLoadingError -> {
            onResourceLoadingError(
                message = action.message,
                error = action.error
            )
        }
    }

    override suspend fun FlowCollector<HumanVerificationViewState>.onError(throwable: Throwable) {
        emit(HumanVerificationViewState.GenericError(throwable.localizedMessage))
    }

    fun onScreenView() {
        viewModelScope.launch {
            recordHumanVerificationScreenView(screenId = HumanVerificationScreenId.V3)
        }
    }

    @Suppress("LongParameterList")
    private fun onLoad(
        url: String,
        originalHost: String?,
        alternativeHost: String?,
        defaultCountry: String?,
        recoveryPhone: String?,
        locale: String?,
        headers: List<Pair<String, String>>?
    ): Flow<HumanVerificationViewState> = flow {
        val loader = when (
            val result = newChallengeLoader(
                ApiConfig(rustApiConfig.userAgent, rustApiConfig.envId, rustApiConfig.proxy, null),
                AppDetails(rustApiConfig.platform, rustApiConfig.product, rustApiConfig.appVersion)
            )
        ) {
            is NewChallengeLoaderResult.Error -> {
                CoreLogger.e(LogTag.DEFAULT, result.v1.toString())
                null
            }

            is NewChallengeLoaderResult.Ok -> result.v1
        }

        if (loader == null) {
            emit(HumanVerificationViewState.GenericError(message = null))
        } else {
            emitAll(
                updateUrl(
                    loader = loader,
                    url = url,
                    originalHost = originalHost,
                    alternativeHost = alternativeHost,
                    defaultCountry = defaultCountry,
                    recoveryPhone = recoveryPhone,
                    locale = locale,
                    headers = headers
                )
            )
        }
    }

    private fun onCancel(): Flow<HumanVerificationViewState> = flow {
        challengeNotifierCallback.onHumanVerificationCancel()
        recordHumanVerificationResult(HumanVerificationStatus.CANCELLED)
        emit(HumanVerificationViewState.Cancel)
    }

    private fun onResourceLoadingError(message: String?, error: WebResponseError?): Flow<HumanVerificationViewState> =
        flow {
            recordHumanVerificationViewLoadingResult(status = error.toHumanVerificationViewLoadingStatus())
            emit(HumanVerificationViewState.GenericError(message))
        }

    private fun onFailure(message: String?): Flow<HumanVerificationViewState> = flow {
        challengeNotifierCallback.onHumanVerificationFailed()
        recordHumanVerificationResult(HumanVerificationStatus.FAILED)
        emit(HumanVerificationViewState.GenericError(message))
    }

    private fun onWebviewEvent(result: HV3ResponseMessage): Flow<HumanVerificationViewState> = flow {
        when (result.type) {
            HV3ResponseMessage.Type.Success -> {
                val token = requireNotNull(result.payload?.token)
                val tokenType = requireNotNull(result.payload?.type)
                challengeNotifierCallback.onHumanVerificationSuccess(tokenType, token)
                recordHumanVerificationResult(HumanVerificationStatus.SUCCEEDED)
                uiEvent.emit(HumanVerificationViewEvent.Success(token, tokenType))
            }

            HV3ResponseMessage.Type.Notification -> {
                val message = requireNotNull(result.payload?.text)
                val messageType = requireNotNull(result.payload?.type?.let { MessageType.map[it] })
                uiEvent.emit(HumanVerificationViewEvent.HvNotification(messageType, message))
            }

            HV3ResponseMessage.Type.Close -> {
                onCancel()
            }

            HV3ResponseMessage.Type.Error -> {
                onFailure(result.payload?.text)
            }

            HV3ResponseMessage.Type.Loaded -> {
                recordHumanVerificationViewLoadingResult(status = HumanVerificationViewLoadingStatus.HTTP2XX)
            }

            HV3ResponseMessage.Type.Resize -> {
                // No action needed
            }
        }
    }

    @Suppress("LongParameterList")
    private fun updateUrl(
        loader: ChallengeLoader,
        url: String,
        originalHost: String?,
        alternativeHost: String?,
        defaultCountry: String?,
        recoveryPhone: String?,
        locale: String?,
        headers: List<Pair<String, String>>?
    ): Flow<HumanVerificationViewState> = flow {
        emit(
            HumanVerificationViewState.Load(
                extraHeaders = headers,
                fullUrl = updateHumanVerificationURL(
                    url = url,
                    defaultCountry = defaultCountry,
                    recoveryPhone = recoveryPhone,
                    locale = locale
                ),
                isWebViewDebuggingEnabled = isWebViewDebuggingEnabled(),
                loader = loader,
                originalHost = originalHost,
                alternativeHost = alternativeHost
            )
        )
    }
}
