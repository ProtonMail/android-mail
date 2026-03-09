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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import me.proton.android.core.account.domain.model.CoreUserId
import uniffi.mail_account_uniffi.LoginError
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowCheckHostDeviceConfirmationResult
import uniffi.mail_account_uniffi.LoginFlowUserIdResult
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class PollEdmSessionFork @Inject constructor() {

    operator fun invoke(loginFlow: LoginFlow, pollDuration: Duration = 5.seconds): Flow<Result> = flow {
        emit(Result.Loading)

        when (val result = loginFlow.checkHostDeviceConfirmation()) {
            is LoginFlowCheckHostDeviceConfirmationResult.Error -> throw PollError.Login(result.v1)
            is LoginFlowCheckHostDeviceConfirmationResult.Ok -> {
                when {
                    loginFlow.isAwaitingHostDeviceConfirmation() -> throw PollError.Awaiting()
                    loginFlow.isLoggedIn() -> emit(onLoggedIn(loginFlow))
                    else -> emit(Result.UnexpectedLoginState)
                }
            }
        }
    }.retryWhen { cause: Throwable, _: Long ->
        if (cause !is PollError) return@retryWhen false

        val fallbackResult = when (cause) {
            is PollError.Awaiting -> Result.Awaiting
            is PollError.Login -> {
                when (val loginError = cause.loginError) {
                    is LoginError.ApiError -> Result.ApiError(loginError.v1)
                    is LoginError.WithCodePollFlowFailed -> Result.ApiError(loginError.v1)
                    else -> null
                }
            }
        }

        if (fallbackResult != null) {
            delay(pollDuration)
            emit(fallbackResult)
        }

        // retry if:
        fallbackResult != null
    }

    private fun onLoggedIn(loginFlow: LoginFlow): Result.Success {
        val userId = requireNotNull((loginFlow.userId() as? LoginFlowUserIdResult.Ok)?.v1)
        return Result.Success(CoreUserId(userId))
    }

    sealed interface Result {
        data class ApiError(val message: String) : Result
        data object Awaiting : Result
        data object Loading : Result
        data class Success(val userId: CoreUserId) : Result
        data object UnexpectedLoginState : Result
    }

    sealed class PollError : Exception() {
        data class Login(val loginError: LoginError) : PollError()
        class Awaiting : PollError()
    }
}
