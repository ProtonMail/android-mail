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

package ch.protonmail.android.composer.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import ch.protonmail.android.composer.data.remote.UploadAttachmentsWorker
import ch.protonmail.android.composer.data.remote.UploadDraftWorker
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploadTracker
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val enqueuer: Enqueuer,
    private val workerManager: WorkManager,
    private val draftUploadTracker: DraftUploadTracker
) : DraftRepository {

    override suspend fun upload(userId: UserId, messageId: MessageId) {
        if (draftUploadTracker.uploadRequired(userId, messageId)) {
            val uniqueWorkId = UploadDraftWorker.id(messageId)

            enqueuer.enqueueUniqueWork<UploadDraftWorker>(
                userId = userId,
                workerId = uniqueWorkId,
                params = UploadDraftWorker.params(userId, messageId),
                existingWorkPolicy = ExistingWorkPolicy.KEEP
            )
        } else {
            Timber.v("Draft: Upload skipped for $messageId")
        }
    }

    override suspend fun forceUpload(userId: UserId, messageId: MessageId) {
        Timber.d("Draft force upload: Adding work to upload $messageId")
        val uniqueWorkId = UploadDraftWorker.id(messageId)

        enqueuer.enqueueInChain<UploadDraftWorker, UploadAttachmentsWorker>(
            userId = userId,
            uniqueWorkId = uniqueWorkId,
            params1 = UploadDraftWorker.params(userId, messageId),
            params2 = UploadAttachmentsWorker.params(userId, messageId),
            existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
        )
    }

    override fun cancelUploadDraft(messageId: MessageId) {
        val uniqueWorkId = UploadDraftWorker.id(messageId)
        workerManager.cancelUniqueWork(uniqueWorkId)
    }
}
