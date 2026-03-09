/*
 * Copyright (c) 2024 Proton Technologies AG
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

package me.proton.android.core.account.data.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.proton.android.core.account.data.model.toCoreSession
import me.proton.android.core.account.data.qualifier.QueryWatcherCoroutineScope
import me.proton.android.core.account.domain.model.CoreSession
import me.proton.android.core.account.domain.usecase.ObserveCoreSessions
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetSessionsResult
import uniffi.mail_uniffi.MailSessionWatchSessionsResult
import uniffi.mail_uniffi.StoredSession
import javax.inject.Inject

class ObserveCoreSessionsImpl @Inject constructor(
    @QueryWatcherCoroutineScope private val coroutineScope: CoroutineScope,
    private val mailSession: MailSession
) : ObserveCoreSessions {

    private val storedSessionsFlow = callbackFlow<List<StoredSession>> {
        val watchedSessions = mailSession.watchSessions(
            object : LiveQueryCallback {
                override fun onUpdate() {
                    launch {
                        when (val sessionsResult = mailSession.getSessions()) {
                            is MailSessionGetSessionsResult.Error -> send(emptyList())
                            is MailSessionGetSessionsResult.Ok -> send(sessionsResult.v1)
                        }
                    }
                }
            }
        )

        when (watchedSessions) {
            is MailSessionWatchSessionsResult.Error -> {
                send(emptyList())
                close()
            }
            is MailSessionWatchSessionsResult.Ok -> {
                send(watchedSessions.v1.sessions)

                awaitClose {
                    watchedSessions.v1.handle.disconnect()
                    watchedSessions.destroy()
                }
            }
        }
    }.shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun invoke(): Flow<List<CoreSession>> = storedSessionsFlow
        .map { list -> list.map { it.toCoreSession() } }
}
