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

package me.proton.android.core.auth.data.usecase

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import me.proton.android.core.auth.data.entity.PasswordValidatorTokenWrapper
import me.proton.core.passvalidator.domain.entity.PasswordValidatorResult
import me.proton.core.passvalidator.domain.usecase.ValidatePassword
import uniffi.mail_account_uniffi.PasswordValidatorServiceCallback
import uniffi.mail_account_uniffi.PasswordValidatorServiceResult
import uniffi.mail_account_uniffi.PasswordValidatorServiceToken

internal class PasswordValidatorServiceCallbackImpl(
    private val producerScope: ProducerScope<ValidatePassword.Result>
) : PasswordValidatorServiceCallback {

    override fun onResults(results: List<PasswordValidatorServiceResult>, token: PasswordValidatorServiceToken?) {
        producerScope.trySendBlocking(
            ValidatePassword.Result(
                results = results
                    .map(PasswordValidatorServiceResult::toKotlin)
                    .filter { !it.isOptional },
                token = token?.let { PasswordValidatorTokenWrapper(it) }
            )
        )
    }
}

private fun PasswordValidatorServiceResult.toKotlin() = PasswordValidatorResult(
    errorMessage = errorMessage,
    hideIfValid = hideIfValid,
    isOptional = isOptional,
    isValid = isValid,
    requirementMessage = requirementMessage
)
