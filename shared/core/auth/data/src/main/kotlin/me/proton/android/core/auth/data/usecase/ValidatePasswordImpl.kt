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

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import me.proton.android.core.auth.data.passvalidator.PasswordValidatorServiceHolder
import me.proton.core.domain.entity.UserId
import me.proton.core.passvalidator.domain.entity.PasswordValidationType
import me.proton.core.passvalidator.domain.usecase.ValidatePassword
import uniffi.mail_account_uniffi.PasswordType
import javax.inject.Inject

@ActivityRetainedScoped
class ValidatePasswordImpl @Inject constructor(
    private val passwordValidatorServiceHolder: PasswordValidatorServiceHolder
) : ValidatePassword {

    override fun invoke(
        passwordValidationType: PasswordValidationType,
        password: String,
        userId: UserId?
    ): Flow<ValidatePassword.Result> = callbackFlow {
        val callback = PasswordValidatorServiceCallbackImpl(producerScope = this)
        val handle = passwordValidatorServiceHolder.get().validate(
            plainPassword = password,
            callback = callback,
            passwordType = passwordValidationType.toPasswordType()
        )
        awaitClose { handle.cancel() }
    }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

internal fun PasswordValidationType.toPasswordType() = when (this) {
    PasswordValidationType.Main -> PasswordType.MAIN
    PasswordValidationType.Secondary -> PasswordType.SECONDARY
}
