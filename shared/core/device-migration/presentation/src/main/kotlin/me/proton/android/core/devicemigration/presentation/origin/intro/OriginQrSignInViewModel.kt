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

package me.proton.android.core.devicemigration.presentation.origin.intro

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.core.devicemigration.presentation.R
import me.proton.android.core.devicemigration.presentation.origin.qr.QrScanOutput
import me.proton.android.core.devicemigration.presentation.origin.usecase.ForkSessionIntoTargetDevice
import me.proton.core.biometric.data.StrongAuthenticatorsResolver
import me.proton.core.biometric.domain.BiometricAuthErrorCode
import me.proton.core.biometric.domain.BiometricAuthResult
import me.proton.core.compose.effect.Effect
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.displayName
import me.proton.core.util.kotlin.coroutine.flowWithResultContext
import uniffi.mail_account_uniffi.QrLoginScanScreenViewTotalScreenId
import uniffi.mail_account_uniffi.qrLoginScanScreenTotal
import javax.inject.Inject

@HiltViewModel
internal class OriginQrSignInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val forkSessionIntoTargetDevice: ForkSessionIntoTargetDevice,
    private val product: Product,
    private val strongAuthenticatorsResolver: StrongAuthenticatorsResolver
) : BaseViewModel<OriginQrSignInAction, OriginQrSignInStateHolder>(
    initialAction = OriginQrSignInAction.Load,
    initialState = OriginQrSignInStateHolder(state = OriginQrSignInState.Loading)
) {

    override fun onAction(action: OriginQrSignInAction): Flow<OriginQrSignInStateHolder> = when (action) {
        is OriginQrSignInAction.Load -> onLoad()
        is OriginQrSignInAction.OnBiometricAuthResult -> onBiometricAuthResult(action.result)
        is OriginQrSignInAction.OnCameraPermissionGranted -> onCameraPermissionGranted()
        is OriginQrSignInAction.OnQrScanResult -> onQrScanResult(action.result)
        is OriginQrSignInAction.Start -> onStart()
    }

    override suspend fun FlowCollector<OriginQrSignInStateHolder>.onError(throwable: Throwable) {
        emit(
            idleWithEffect(
                OriginQrSignInEvent.ErrorMessage(
                    throwable.localizedMessage ?: context.getString(R.string.presentation_error_general)
                )
            )
        )
    }

    fun onScreenView(state: OriginQrSignInState) = when (state) {
        is OriginQrSignInState.Idle -> QrLoginScanScreenViewTotalScreenId.INSTRUCTIONS
        is OriginQrSignInState.Loading -> null
        is OriginQrSignInState.MissingCameraPermission -> QrLoginScanScreenViewTotalScreenId.CAMERA_ACCESS_NOT_ALLOWED
        is OriginQrSignInState.SignedInSuccessfully -> null
        is OriginQrSignInState.Verifying -> QrLoginScanScreenViewTotalScreenId.VERIFYING
    }?.let {
        qrLoginScanScreenTotal(it)
    }

    fun onFailureScreenView() {
        qrLoginScanScreenTotal(QrLoginScanScreenViewTotalScreenId.FAILURE)
    }

    private fun onLoad() = flow {
        emit(OriginQrSignInStateHolder(state = OriginQrSignInState.Idle))
    }

    private fun onStart() = flow {
        emit(loadingWithEffect(OriginQrSignInEvent.LaunchBiometricsCheck(strongAuthenticatorsResolver)))
    }

    private fun onBiometricAuthResult(result: BiometricAuthResult) = flow {
        val stateHolder = when (result) {
            is BiometricAuthResult.AuthError -> {
                if (result.code.shouldDisplayErrorMessage) {
                    idleWithEffect(OriginQrSignInEvent.ErrorMessage(result.message.toString()))
                } else {
                    OriginQrSignInStateHolder(state = OriginQrSignInState.Idle)
                }
            }

            is BiometricAuthResult.Success -> loadingWithEffect(OriginQrSignInEvent.LaunchQrScanner)
        }
        emit(stateHolder)
    }

    private fun onCameraPermissionGranted() = flow {
        emit(OriginQrSignInStateHolder(state = OriginQrSignInState.Idle))
    }

    private fun onQrScanResult(result: QrScanOutput<String>) = flow {
        when (result) {
            is QrScanOutput.MissingCameraPermission -> emit(
                OriginQrSignInStateHolder(
                    state = OriginQrSignInState.MissingCameraPermission(productName = product.displayName())
                )
            )

            is QrScanOutput.Cancelled -> emit(OriginQrSignInStateHolder(state = OriginQrSignInState.Idle))
            is QrScanOutput.ManualInputRequested -> emit(idleWithEffect(OriginQrSignInEvent.LaunchManualCodeInput))
            is QrScanOutput.Success -> emitAll(submitCode(result.contents))
        }
    }

    private fun submitCode(code: String) = flowWithResultContext {
        emit(OriginQrSignInStateHolder(state = OriginQrSignInState.Verifying))

        when (val result = forkSessionIntoTargetDevice(qrCode = code)) {
            is ForkSessionIntoTargetDevice.Result.Error -> emit(
                idleWithEffect(
                    OriginQrSignInEvent.ErrorMessage(
                        result.message,
                        onRetry = { perform(OriginQrSignInAction.Start) }
                    )
                )
            )

            is ForkSessionIntoTargetDevice.Result.Success -> emit(
                stateWithEffect(
                    OriginQrSignInState.SignedInSuccessfully,
                    OriginQrSignInEvent.SignedInSuccessfully
                )
            )
        }
    }

    private fun idleWithEffect(event: OriginQrSignInEvent): OriginQrSignInStateHolder =
        stateWithEffect(OriginQrSignInState.Idle, event)

    private fun loadingWithEffect(event: OriginQrSignInEvent): OriginQrSignInStateHolder =
        stateWithEffect(OriginQrSignInState.Loading, event)

    private fun stateWithEffect(state: OriginQrSignInState, event: OriginQrSignInEvent): OriginQrSignInStateHolder =
        OriginQrSignInStateHolder(Effect.of(event), state)
}

private val BiometricAuthErrorCode.shouldDisplayErrorMessage: Boolean
    get() = when (this) {
        BiometricAuthErrorCode.UserCanceled,
        BiometricAuthErrorCode.NegativeButton -> false

        else -> true
    }
