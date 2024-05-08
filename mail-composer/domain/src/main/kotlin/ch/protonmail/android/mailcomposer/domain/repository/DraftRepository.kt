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

package ch.protonmail.android.mailcomposer.domain.repository

import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId

interface DraftRepository {

    /**
     * Schedules the upload of the message with the given messageId.
     * In case a job to upload this message is already ongoing or enqueued, nothing is done
     */
    suspend fun upload(userId: UserId, messageId: MessageId)

    /**
     * Schedules the upload of the message with the given messageId.
     * In case a job to upload this message is already ongoing or enqueued,
     * a new job will be chained to happen afterwards, independently of the outcome of the existing one.
     */
    suspend fun forceUpload(userId: UserId, messageId: MessageId)

    /**
     * Cancels the upload of the message with the given messageId.
     * In case a job to upload this message is already ongoing or enqueued, it will be cancelled.
     */
    fun cancelUploadDraft(messageId: MessageId)
}
