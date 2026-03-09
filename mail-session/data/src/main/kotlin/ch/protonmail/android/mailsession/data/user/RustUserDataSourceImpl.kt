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

package ch.protonmail.android.mailsession.data.user

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import uniffi.mail_uniffi.MailUserSessionWatchUserStreamResult
import uniffi.mail_uniffi.User
import uniffi.mail_uniffi.WatchUserStreamNextAsyncResult
import javax.inject.Inject

class RustUserDataSourceImpl @Inject constructor() : RustUserDataSource {

    override fun observeUser(mailUserSession: MailUserSessionWrapper): Flow<Either<DataError, User>> = callbackFlow {
        Timber.d("rust-user: Starting user observation")

        mailUserSession.getUser().onLeft { error ->
            Timber.e("rust-user: Failed to get user: $error")
            send(error.left())
            close()
            return@callbackFlow
        }.onRight { user ->
            send(user.right())

            Timber.d("rust-user: Got user, creating watcher")
            when (val userStream = mailUserSession.watchUserStream()) {

                is MailUserSessionWatchUserStreamResult.Error -> {
                    Timber.e("rust-user: Failed to create stream watcher: ${userStream.v1}")
                    send(userStream.v1.toDataError().left())
                    close()
                    return@callbackFlow
                }

                is MailUserSessionWatchUserStreamResult.Ok -> {
                    Timber.d("rust-user: Created user watcher")
                    while (true) {
                        when (userStream.v1.nextAsync()) {
                            is WatchUserStreamNextAsyncResult.Error -> {
                                Timber.w("rust-user: received new watcher error")
                                break
                            }

                            WatchUserStreamNextAsyncResult.Ok -> {
                                val update = mailUserSession.getUser()
                                Timber.d("rust-user: received new watcher event $update")
                                send(update)
                            }
                        }
                    }

                }
            }
        }

        awaitClose {
            Timber.d("rust-user: Closing watcher")
        }
    }
}
