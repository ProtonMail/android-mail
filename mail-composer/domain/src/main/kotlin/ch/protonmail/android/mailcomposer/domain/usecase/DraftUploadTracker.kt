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

package ch.protonmail.android.mailcomposer.domain.usecase

import java.util.concurrent.ConcurrentHashMap
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftUploadTracker @Inject constructor(
    private val findLocalDraft: FindLocalDraft,
    private val draftStateRepository: DraftStateRepository
) {

    private val lastUploadedDrafts: MutableMap<MessageId, MessageWithBody> = ConcurrentHashMap()

    suspend fun uploadRequired(userId: UserId, messageId: MessageId): Boolean {

        // Upload may be skipped only in Synchronised state
        draftStateRepository.observe(userId, messageId).firstOrNull()
            ?.takeIf { draftState ->
                draftState.fold(ifRight = { it.state != DraftSyncState.Synchronized }, ifLeft = { true })
            }
            ?.let { return true }

        val lastUploadedDraft = lastUploadedDrafts[messageId]

        return if (lastUploadedDraft != null) {
            val localDraft = findLocalDraft(userId, messageId)

            localDraft?.let { it != lastUploadedDraft } ?: true
        } else {
            true
        }
    }

    fun notifyUploadedDraft(messageId: MessageId, messageWithBody: MessageWithBody) {
        lastUploadedDrafts[messageId] = messageWithBody
    }

    fun notifySentMessages(sentMessageList: Set<MessageId>) {
        lastUploadedDrafts.keys.removeAll(sentMessageList.toSet())
    }
}
