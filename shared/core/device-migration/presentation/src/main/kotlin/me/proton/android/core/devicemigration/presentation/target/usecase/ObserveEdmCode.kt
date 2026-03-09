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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import me.proton.android.core.devicemigration.presentation.R
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowGenerateSignInQrCodeResult
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class ObserveEdmCode @Inject constructor(@ApplicationContext private val context: Context) {

    @OptIn(FlowPreview::class)
    operator fun invoke(loginFlow: LoginFlow, autoRefreshDuration: Duration = 9.minutes): Flow<Result> = flow {
        when (val result = loginFlow.generateSignInQrCode(needEncryptionKey = true)) {
            is LoginFlowGenerateSignInQrCodeResult.Error -> emit(Result.Failure(getErrorMessage()))
            is LoginFlowGenerateSignInQrCodeResult.Ok -> emit(Result.Success(qrCode = result.v1))
        }
    }.timeout(autoRefreshDuration).retryWhen { cause, _ ->
        cause is TimeoutCancellationException
    }

    private fun getErrorMessage(): String = context.getString(R.string.presentation_error_general)

    sealed interface Result {
        data class Success(val qrCode: String) : Result
        data class Failure(val message: String) : Result
    }
}
