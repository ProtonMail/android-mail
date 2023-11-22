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

package ch.protonmail.android.mailsettings.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.Either
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailsettings.domain.model.ClearDataAction
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber

@HiltWorker
internal class ClearLocalDataWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val accountManager: AccountManager,
    private val messageLocalDataSource: MessageLocalDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource,
    private val attachmentsLocalDataSource: AttachmentLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val clearLocalCache = inputData.getBoolean(KeyClearLocalCache, false)
        val clearMessageData = inputData.getBoolean(KeyClearMessagesData, false)
        val clearAttachments = inputData.getBoolean(KeyClearAttachments, false)

        val userIds = accountManager.getAccounts().map {
            it.map { user -> user.userId }
        }.firstOrNull() ?: return Result.failure(
            workDataOf(KeyClearDataError to "Unable to fetch accounts list.")
        )

        userIds.forEach { userId ->
            doCleanUp(userId, clearLocalCache, clearMessageData, clearAttachments)
        }

        return Result.success()
    }

    private suspend fun doCleanUp(
        userId: UserId,
        clearLocalCache: Boolean,
        clearMessageData: Boolean,
        clearAttachments: Boolean
    ) {
        if (clearLocalCache) doClearLocalCache().onLeft {
            Timber.e("Unable to clear local data cache - ${it.errorMessage}")
        }

        if (clearMessageData) doClearMessageData(userId).onLeft {
            Timber.e("Unable to clear local message data - ${it.errorMessage}")
        }

        if (clearAttachments) doClearAttachmentData(userId).onLeft {
            Timber.e("Unable to clear local attachment data - ${it.errorMessage}")
        }
    }

    private suspend fun doClearLocalCache(): Either<ClearLocalDataError, Unit> = withContext(Dispatchers.IO) {
        Either.catch {
            require(context.externalCacheDir?.deleteRecursively() ?: false)
            require(context.codeCacheDir.deleteRecursively())
        }.mapLeft {
            ClearLocalDataError.LocalCache(it.message)
        }
    }

    private suspend fun doClearMessageData(userId: UserId): Either<ClearLocalDataError, Unit> =
        withContext(Dispatchers.IO) {
            Either.catch {
                messageLocalDataSource.deleteAllMessages(userId)
                conversationLocalDataSource.deleteAllConversations(userId)
            }.mapLeft {
                ClearLocalDataError.MessagesData(it.message)
            }
        }

    private suspend fun doClearAttachmentData(userId: UserId): Either<ClearLocalDataError, Unit> =
        withContext(Dispatchers.IO) {
            Either.catch {
                require(attachmentsLocalDataSource.deleteAttachments(userId))
            }.mapLeft {
                ClearLocalDataError.Attachments(it.message)
            }
        }

    private sealed class ClearLocalDataError(val errorMessage: String?) {
        class LocalCache(error: String?) : ClearLocalDataError(error)
        class MessagesData(error: String?) : ClearLocalDataError(error)
        class Attachments(error: String?) : ClearLocalDataError(error)
    }

    companion object {

        const val KeyClearLocalCache = "clearLocalCache"
        const val KeyClearMessagesData = "clearMessagesData"
        const val KeyClearAttachments = "clearAttachments"
        const val KeyClearDataError = "clearDataError"

        fun params(params: ClearDataAction) = mapOf(
            KeyClearLocalCache to params.clearLocalCache,
            KeyClearMessagesData to params.clearMessagesData,
            KeyClearAttachments to params.clearAttachmentsData
        )

        fun id(clearDataAction: ClearDataAction): String = "ClearLocalDataWorker-${clearDataAction.hashCode()}"
    }
}
