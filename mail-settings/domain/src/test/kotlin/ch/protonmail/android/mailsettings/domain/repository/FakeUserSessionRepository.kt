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

package ch.protonmail.android.mailsettings.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailcommon.domain.model.autolock.SetAutoLockPinError
import ch.protonmail.android.mailsession.domain.model.Account
import ch.protonmail.android.mailsession.domain.model.Fork
import ch.protonmail.android.mailsession.domain.model.SessionError
import ch.protonmail.android.mailsession.domain.model.User
import ch.protonmail.android.mailsession.domain.model.UserSettings
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId

@Suppress("NotImplementedDeclaration")
internal class FakeUserSessionRepository(
    private val primaryUserId: UserId? = UserId("user-id"),
    private val userList: List<User> = emptyList()
) : UserSessionRepository {

    override fun observePrimaryAccount(): Flow<Account?> {
        TODO("Not yet implemented")
    }

    override fun observeAccounts(): Flow<List<Account>> {
        TODO("Not yet implemented")
    }

    override fun observePrimaryUserId(): Flow<UserId?> = flowOf(primaryUserId)

    override fun observeUser(userId: UserId): Flow<Either<DataError, User>> = flowOf(
        userList.find { it.userId == userId }?.let { user ->
            Either.Right(user)
        } ?: Either.Left(DataError.Local.NoDataCached)
    )

    override suspend fun getAccount(userId: UserId): Account? {
        TODO("Not yet implemented")
    }

    override suspend fun getUserId(sessionId: SessionId): UserId? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun disableAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun getUserSession(userId: UserId): MailUserSessionWrapper? {
        TODO("Not yet implemented")
    }

    override suspend fun getUserSettings(userId: UserId): UserSettings? {
        TODO("Not yet implemented")
    }

    override suspend fun forkSession(userId: UserId): Either<SessionError, Fork> {
        TODO("Not yet implemented")
    }

    override suspend fun setPrimaryAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun setAutoLockPinCode(autoLockPin: AutoLockPin): Either<SetAutoLockPinError, Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun setBiometricAppProtection(): Either<DataError, Unit> {
        TODO("Not yet implemented")
    }

    override fun observeUserSessionAvailable(userId: UserId): Flow<UserId> = observePrimaryUserId().filterNotNull()
    override suspend fun overrideFeatureFlag(
        userId: UserId,
        flagName: String,
        newValue: Boolean
    ): Either<DataError, Unit> {
        TODO("Not yet implemented")
    }
}
