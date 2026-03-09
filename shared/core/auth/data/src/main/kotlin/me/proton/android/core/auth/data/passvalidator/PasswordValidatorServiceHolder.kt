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

package me.proton.android.core.auth.data.passvalidator

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.core.auth.data.LogTag
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_account_uniffi.PasswordValidatorService
import javax.inject.Inject

@ActivityRetainedScoped
class PasswordValidatorServiceHolder @Inject constructor() {

    private val mutex = Mutex()
    private var passwordValidatorService: PasswordValidatorService? = null

    suspend fun bind(provider: suspend () -> PasswordValidatorService) {
        mutex.withLock {
            passwordValidatorService = passwordValidatorService ?: runCatching {
                provider()
            }.onFailure {
                CoreLogger.e(LogTag.DEFAULT, it, "Failed to initialize password validator service.")
            }.getOrNull()
        }
    }

    suspend fun get(): PasswordValidatorService = mutex.withLock {
        requireNotNull(passwordValidatorService) {
            "Password validator service is not initialized (call bind() first)."
        }
    }
}
