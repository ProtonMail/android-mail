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

package me.proton.android.core.auth.presentation.secondfactor.fido.keys

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.account.domain.usecase.ObservePrimaryCoreAccount
import me.proton.android.core.auth.presentation.secondfactor.getAccountById
import me.proton.android.core.auth.presentation.secondfactor.getSessionsForAccount
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailUserSessionUserSettingsResult
import uniffi.mail_uniffi.StoredSession
import javax.inject.Inject

class ObserveSecurityKeys @Inject constructor(
    private val sessionInterface: MailSession,
    private val observePrimaryCoreAccount: ObservePrimaryCoreAccount
) {

    operator fun invoke(): Flow<List<Fido2RegisteredKey>> = observePrimaryCoreAccount()
        .filterNotNull()
        .flatMapLatest { account ->
            observeSecurityKeysForAccount(account.userId)
        }
        .distinctUntilChanged()

    private fun observeSecurityKeysForAccount(userId: CoreUserId): Flow<List<Fido2RegisteredKey>> = flow {
        val session = getActiveSession(userId.id)
        if (session == null) {
            emit(emptyList())
            return@flow
        }

        val userSession = when (val result = sessionInterface.userSessionFromStoredSession(session)) {
            is MailSessionUserSessionFromStoredSessionResult.Error -> null
            is MailSessionUserSessionFromStoredSessionResult.Ok -> result.v1
        }
        if (userSession == null) {
            emit(emptyList())
            return@flow
        }

        val securityKeys = when (val userSettingsResult = userSession.userSettings()) {
            is MailUserSessionUserSettingsResult.Error -> emptyList()
            is MailUserSessionUserSettingsResult.Ok -> userSettingsResult.v1.twoFactorAuth.registeredKeys
        }.toFido2SecurityKeys()

        emit(securityKeys)
    }

    private suspend fun getActiveSession(userId: String): StoredSession? {
        return sessionInterface.getSessionsForAccount(
            sessionInterface.getAccountById(userId)
        )?.firstOrNull()
    }
}
