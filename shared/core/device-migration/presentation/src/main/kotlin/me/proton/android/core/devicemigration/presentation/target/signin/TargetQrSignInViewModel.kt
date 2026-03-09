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

package me.proton.android.core.devicemigration.presentation.target.signin

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.android.core.devicemigration.presentation.R
import me.proton.android.core.devicemigration.presentation.origin.qr.QrBitmapGenerator
import me.proton.android.core.devicemigration.presentation.target.usecase.ObserveForkFromOriginDevice
import me.proton.core.compose.effect.Effect
import me.proton.core.compose.viewmodel.BaseViewModel
import uniffi.mail_account_uniffi.QrLoginShowQrCodeScreenViewTotalScreenId
import uniffi.mail_account_uniffi.qrLoginShowQrScreenTotal
import javax.inject.Inject

@HiltViewModel
internal class TargetQrSignInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeForkFromOriginDevice: ObserveForkFromOriginDevice,
    private val qrBitmapGenerator: QrBitmapGenerator
) : BaseViewModel<TargetQrSignInAction, TargetQrSignInState>(
    initialAction = TargetQrSignInAction.Load(),
    initialState = TargetQrSignInState.Loading
) {

    override fun onAction(action: TargetQrSignInAction): Flow<TargetQrSignInState> = when (action) {
        is TargetQrSignInAction.Load -> onLoad()
    }

    override suspend fun FlowCollector<TargetQrSignInState>.onError(throwable: Throwable) {
        emit(
            stateWithUnrecoverableError(
                message = throwable.localizedMessage,
                onRetry = { perform(TargetQrSignInAction.Load()) }
            )
        )
    }

    fun onFailureScreenView() {
        qrLoginShowQrScreenTotal(QrLoginShowQrCodeScreenViewTotalScreenId.FAILURE)
    }

    fun onInstructionsScreenView() {
        qrLoginShowQrScreenTotal(QrLoginShowQrCodeScreenViewTotalScreenId.INSTRUCTIONS)
    }

    private fun onLoad(): Flow<TargetQrSignInState> = flow {
        emit(TargetQrSignInState.Loading)

        observeForkFromOriginDevice().map { result ->
            when (result) {
                is ObserveForkFromOriginDevice.Result.Error -> stateWithUnrecoverableError(
                    message = result.message,
                    onRetry = { perform(TargetQrSignInAction.Load()) }
                )

                is ObserveForkFromOriginDevice.Result.Idle -> TargetQrSignInState.Idle(
                    errorMessage = result.errorMessage,
                    qrCode = result.qrCode,
                    generateBitmap = qrBitmapGenerator::invoke
                )

                is ObserveForkFromOriginDevice.Result.SuccessfullySignedIn -> TargetQrSignInState.SuccessfullySignedIn(
                    effect = Effect.of(TargetQrSignInEvent.SignedIn(result.userId))
                )
            }
        }.collect {
            emit(it)
        }
    }

    private fun stateWithUnrecoverableError(
        message: String? = null,
        onRetry: (() -> Unit)? = { perform(TargetQrSignInAction.Load()) }
    ) = TargetQrSignInState.Failure(
        message = message ?: context.getString(R.string.target_sign_in_retryable_error),
        onRetry = onRetry
    )
}
