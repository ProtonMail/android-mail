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
package ch.protonmail.android.mailsnooze.data

import arrow.core.Either
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalNonDefaultWeekStart
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsnooze.data.mapper.toSnoozeError
import ch.protonmail.android.mailsnooze.data.mapper.toUnsnoozeError
import ch.protonmail.android.mailsnooze.domain.model.SnoozeError
import ch.protonmail.android.mailsnooze.domain.model.UnsnoozeError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.AvailableSnoozeActionsForConversationResult
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.SnoozeActions
import uniffi.mail_uniffi.SnoozeConversationsResult
import uniffi.mail_uniffi.UnsnoozeConversationsResult
import uniffi.mail_uniffi.availableSnoozeActionsForConversation
import uniffi.mail_uniffi.snoozeConversations
import uniffi.mail_uniffi.unsnoozeConversations
import javax.inject.Inject
import kotlin.time.Instant

class RustSnoozeDataSource @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun getAvailableSnoozeActionsForConversation(
        userId: UserId,
        weekStart: LocalNonDefaultWeekStart,
        conversationIds: List<LocalConversationId>
    ): Either<SnoozeError, SnoozeActions> = withContext(ioDispatcher) {
        executeWithUserSession(userId) { sessionWrapper ->
            when (
                val result = availableSnoozeActionsForConversation(
                    sessionWrapper.getRustUserSession(),
                    weekStart, conversationIds
                )
            ) {
                is AvailableSnoozeActionsForConversationResult.Error -> result.v1.toSnoozeError().left()
                is AvailableSnoozeActionsForConversationResult.Ok -> result.v1.right()
            }
        }.mapLeft { left ->
            return@withContext SnoozeError.Other().left()
        }.flatten()
    }

    suspend fun snoozeConversation(
        userId: UserId,
        labelId: Id,
        ids: List<LocalConversationId>,
        snoozeTime: Instant
    ) = withContext(ioDispatcher) {
        executeWithUserSession(userId) { sessionWrapper ->
            when (
                val result = snoozeConversations(
                    sessionWrapper.getRustUserSession(),
                    labelId = labelId,
                    ids = ids,
                    snoozeTime = snoozeTime.epochSeconds.toULong()
                )
            ) {
                is SnoozeConversationsResult.Error -> result.v1.toSnoozeError().left().apply {
                    Timber.d("rust-snooze: snoozeConversation ERROR: $ids  $this")
                }

                is SnoozeConversationsResult.Ok -> Unit.right()
            }
        }.mapLeft { left ->
            return@withContext SnoozeError.Other().left()
        }.flatten()
    }

    suspend fun unSnoozeConversation(
        userId: UserId,
        labelId: Id,
        ids: List<LocalConversationId>
    ): Either<UnsnoozeError, Unit> = withContext(ioDispatcher) {
        executeWithUserSession(userId) { sessionWrapper ->
            when (
                val result = unsnoozeConversations(
                    sessionWrapper.getRustUserSession(),
                    labelId = labelId,
                    ids = ids
                )
            ) {
                is UnsnoozeConversationsResult.Error -> result.v1.toUnsnoozeError().left().apply {
                    Timber.d("rust-snooze: unsnoozeConversation ERROR: $ids  $this")
                }

                is UnsnoozeConversationsResult.Ok -> Unit.right().apply {
                    Timber.d(
                        "rust-snooze: unsnoozeConversation SUCCESS: labelid $labelId conversationIDs $ids"
                    )
                }
            }
        }.mapLeft { left ->
            return@withContext UnsnoozeError.Other().left()
        }.flatten()
    }
}
