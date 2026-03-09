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

package ch.protonmail.android.composer.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.mapper.toMessageSendingStatus
import ch.protonmail.android.composer.data.usecase.CreateRustDraftSendWatcher
import ch.protonmail.android.composer.data.usecase.RustDeleteDraftSendResult
import ch.protonmail.android.composer.data.usecase.RustMarkDraftSendResultAsSeen
import ch.protonmail.android.composer.data.usecase.RustQueryUnseenDraftSendResults
import ch.protonmail.android.mailcommon.data.mapper.LocalDraftSendResult
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.DraftSendResultCallback
import uniffi.mail_uniffi.DraftSendResultWatcher
import javax.inject.Inject

class RustSendingStatusDataSourceImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val createRustDraftSendWatcher: CreateRustDraftSendWatcher,
    private val rustQueryUnseenDraftSendResults: RustQueryUnseenDraftSendResults,
    private val rustDeleteDraftSendResult: RustDeleteDraftSendResult,
    private val rustMarkDraftSendResultAsSeen: RustMarkDraftSendResultAsSeen
) : RustSendingStatusDataSource {

    override suspend fun observeMessageSendingStatus(userId: UserId): Flow<MessageSendingStatus> = callbackFlow {
        val session = userSessionRepository.getUserSession(userId) ?: run {
            Timber.e("rust-draft: Trying to observe sending status; Failing.")
            close()
            return@callbackFlow
        }

        var watchHandle: DraftSendResultWatcher? = null
        val draftSendResultCallback = object : DraftSendResultCallback {
            override fun onNewSendResult(details: List<LocalDraftSendResult>) {
                for (result in details) {
                    trySend(result.toMessageSendingStatus())
                }
            }
        }

        createRustDraftSendWatcher(session, draftSendResultCallback)
            .onLeft {
                Timber.e("rust-draft: Failed to create draft send result watcher; Failing.")
                close()
                return@callbackFlow
            }
            .onRight {
                watchHandle = it
            }

        Timber.d("rust-draft: Draft send result watcher created.")

        awaitClose {
            watchHandle?.destroy()
            Timber.d("rust-draft: draft send result watcher destroyed")
        }

    }

    override suspend fun queryUnseenMessageSendingStatuses(
        userId: UserId
    ): Either<DataError, List<MessageSendingStatus>> = withValidUserSession(userId) { session ->
        rustQueryUnseenDraftSendResults(session).fold(
            ifLeft = { error ->
                Timber.e("rust-draft: Query unseen sending status failed with error: $error")
                error.left()
            },
            ifRight = { results ->
                results.map { it.toMessageSendingStatus() }.right()
            }
        )
    }

    override suspend fun deleteMessageSendingStatuses(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError, Unit> = withValidUserSession(userId) { session ->
        rustDeleteDraftSendResult(session, messageIds.map { it.toLocalMessageId() }).fold(
            ifLeft = { error ->
                Timber.e("rust-draft: Delete sending status failed with error: $error")
                error.left()
            },
            ifRight = { Unit.right() }
        )
    }

    override suspend fun markMessageSendingStatusesAsSeen(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError, Unit> = withValidUserSession(userId) { session ->
        rustMarkDraftSendResultAsSeen(session, messageIds.map { it.toLocalMessageId() }).fold(
            ifLeft = { error ->
                Timber.e("rust-draft: Mark sending status as seen failed with error: $error")
                error.left()
            },
            ifRight = { Unit.right() }
        )
    }

    private suspend fun <T> withValidUserSession(
        userId: UserId,
        closure: suspend (MailUserSessionWrapper) -> Either<DataError, T>
    ): Either<DataError, T> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-draft: updating sending status; Failing due to No Session.")
            return DataError.Local.NoUserSession.left()
        }
        return closure(session)
    }


}
