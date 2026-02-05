/*
 * Copyright (c) 2025 Proton Technologies AG
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

package me.proton.android.core.devicemigration.presentation.target.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.devicemigration.presentation.R
import uniffi.proton_account_uniffi.LoginFlow
import uniffi.proton_mail_uniffi.MailSession
import uniffi.proton_mail_uniffi.MailSessionNewLoginFlowResult
import uniffi.proton_mail_uniffi.OtherErrorReason
import uniffi.proton_mail_uniffi.ProtonError
import javax.inject.Inject

internal class ObserveForkFromOriginDevice @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeEdmCode: ObserveEdmCode,
    private val pollEdmSessionFork: PollEdmSessionFork,
    private val session: MailSession
) {

    operator fun invoke(): Flow<Result> = flow {
        when (val result = session.newLoginFlow()) {
            is MailSessionNewLoginFlowResult.Error -> emit(Result.Error(result.v1.getMessage(context)))
            is MailSessionNewLoginFlowResult.Ok -> emitAll(generateQrCodeAndPoll(result.v1))
        }
    }

    private fun generateQrCodeAndPoll(loginFlow: LoginFlow): Flow<Result> =
        observeEdmCode(loginFlow).transformLatest { result ->
            when (result) {
                is ObserveEdmCode.Result.Failure -> emit(Result.Error(result.message))
                is ObserveEdmCode.Result.Success -> emitAll(onQrCode(loginFlow, result.qrCode))
            }
        }

    private fun onQrCode(loginFlow: LoginFlow, qrCode: String): Flow<Result> =
        pollEdmSessionFork(loginFlow).map { pollResult ->
            when (pollResult) {
                is PollEdmSessionFork.Result.Success ->
                    Result.SuccessfullySignedIn(pollResult.userId)

                is PollEdmSessionFork.Result.ApiError -> Result.Idle(
                    errorMessage = pollResult.message,
                    qrCode = qrCode
                )

                is PollEdmSessionFork.Result.Awaiting,
                is PollEdmSessionFork.Result.Loading -> Result.Idle(
                    errorMessage = null,
                    qrCode = qrCode
                )

                is PollEdmSessionFork.Result.UnexpectedLoginState ->
                    Result.Error(message = null)
            }
        }

    sealed interface Result {
        data class Error(val message: String?) : Result
        data class Idle(val errorMessage: String?, val qrCode: String) : Result
        data class SuccessfullySignedIn(val userId: CoreUserId) : Result
    }
}

private fun ProtonError.getMessage(context: Context): String = when (this) {
    is ProtonError.Network -> context.getString(R.string.presentation_general_connection_error)
    is ProtonError.NonProcessableActions -> context.getString(R.string.proton_error_non_processable_actions)
    is ProtonError.OtherReason -> when (val reason = this.v1) {
        is OtherErrorReason.InvalidParameter,
        is OtherErrorReason.TaskCancelled -> context.getString(R.string.presentation_error_general)

        is OtherErrorReason.Other -> reason.v1
    }

    is ProtonError.ServerError -> context.getString(R.string.presentation_error_general)
    is ProtonError.Unexpected -> v1.name
}
