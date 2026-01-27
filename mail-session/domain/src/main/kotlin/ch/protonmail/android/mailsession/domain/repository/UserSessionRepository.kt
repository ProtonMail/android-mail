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

package ch.protonmail.android.mailsession.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailcommon.domain.model.autolock.SetAutoLockPinError
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.model.Fork
import ch.protonmail.android.mailsession.domain.model.SessionError
import ch.protonmail.android.mailsession.domain.model.User
import ch.protonmail.android.mailsession.domain.model.UserSettings
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import uniffi.proton_mail_uniffi.MailUserSession

interface UserSessionRepository {

    fun observePrimaryAccount(): Flow<Account?>

    fun observeAccounts(): Flow<List<Account>>

    fun observePrimaryUserId(): Flow<UserId?>

    fun observeUser(userId: UserId): Flow<Either<DataError, User>>

    suspend fun getAccount(userId: UserId): Account?

    suspend fun getUserId(sessionId: SessionId): UserId?

    suspend fun deleteAccount(userId: UserId)

    suspend fun disableAccount(userId: UserId)

    suspend fun getUserSession(userId: UserId): MailUserSessionWrapper?

    suspend fun getUserSettings(userId: UserId): UserSettings?

    suspend fun forkSession(userId: UserId): Either<SessionError, Fork>

    suspend fun setPrimaryAccount(userId: UserId)

    suspend fun setAutoLockPinCode(autoLockPin: AutoLockPin): Either<SetAutoLockPinError, Unit>

    suspend fun setBiometricAppProtection(): Either<DataError, Unit>

    fun observeUserSessionAvailable(userId: UserId): Flow<UserId?>

    suspend fun overrideFeatureFlag(
        userId: UserId,
        flagName: String,
        newValue: Boolean
    ): Either<DataError, Unit>
}

fun UserSessionRepository.onAccountState(state: AccountState, initialState: Boolean = true): Flow<Account> =
    observeAccounts()
        .transform { accounts -> accounts.forEach { emit(it) } }
        .filter { it.state == state }
        .drop(if (initialState) 0 else 1)
        .distinctUntilChanged()

suspend fun UserSessionRepository.getPrimarySession(): MailUserSession? {
    val primaryUserId = observePrimaryUserId().firstOrNull() ?: return null
    val userSession = getUserSession(primaryUserId) ?: return null
    return userSession.getRustUserSession()
}
