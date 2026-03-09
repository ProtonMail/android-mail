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

package me.proton.android.core.auth.presentation.passmanagement

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.account.domain.usecase.ObservePrimaryCoreAccount
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.secondfactor.getAccountById
import me.proton.android.core.auth.presentation.secondfactor.getSessionsForAccount
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailUserSessionUserSettingsResult
import uniffi.mail_uniffi.StoredSession
import javax.inject.Inject

class ObservePasswordConfig @Inject constructor(
    private val sessionInterface: MailSession,
    private val observePrimaryCoreAccount: ObservePrimaryCoreAccount
) {

    operator fun invoke(userId: CoreUserId?): Flow<PasswordConfig> =
        userId?.let(::observePasswordModeForAccount)?.distinctUntilChanged()
            ?: observePrimaryCoreAccount()
                .filterNotNull()
                .flatMapLatest { account ->
                    observePasswordModeForAccount(account.userId)
                }
                .distinctUntilChanged()

    private fun observePasswordModeForAccount(userId: CoreUserId): Flow<PasswordConfig> = flow {
        val session = getActiveSession(userId.id)
            ?: throw FlowManager.SessionException("No active session found for user: $userId")

        val userSession = when (val result = sessionInterface.userSessionFromStoredSession(session)) {
            is MailSessionUserSessionFromStoredSessionResult.Error -> null
            is MailSessionUserSessionFromStoredSessionResult.Ok -> result.v1
        }

        val password = when (val userSettingsResult = userSession?.userSettings()) {
            null,
            is MailUserSessionUserSettingsResult.Error -> null // default

            is MailUserSessionUserSettingsResult.Ok -> userSettingsResult.v1.password
        }

        emit(PasswordConfig(userId, password?.mode?.toInt() ?: PasswordMode.ONE.value))
    }

    private suspend fun getActiveSession(userId: String): StoredSession? {
        return sessionInterface.getSessionsForAccount(
            sessionInterface.getAccountById(userId)
        )?.firstOrNull()
    }
}

data class PasswordConfig(
    val userId: CoreUserId,
    val passwordMode: Int
)

enum class PasswordMode(val value: Int) {
    ONE(1),
    TWO(2);

    companion object {

        val map = entries.associateBy { it.value }
    }
}
