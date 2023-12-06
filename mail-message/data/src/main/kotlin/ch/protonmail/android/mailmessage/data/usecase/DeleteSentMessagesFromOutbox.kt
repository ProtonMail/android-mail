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

package ch.protonmail.android.mailmessage.data.usecase

import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.usecase.DeleteDraftState
import me.proton.core.domain.entity.UserId
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Delete sent messages from outbox. Currently, we identify outbox messages by the presence of the Sent draft sync state
 * in the DraftSyncEntity table.
 */
class DeleteSentMessagesFromOutbox @Inject constructor(
    private val deleteDraftState: DeleteDraftState
) {

    suspend operator fun invoke(userId: UserId, sentDraftItems: List<DraftState>) {

        // We need to keep Sent messages in Outbox until the next event loop iteration in order to prevent
        // pull to refresh updates overwriting the message label
        delay(EVENT_LOOP_PERIOD_MS)
        for (sentDraft in sentDraftItems) {
            deleteDraftState(userId, sentDraft.messageId)
        }
    }

    companion object {

        const val EVENT_LOOP_PERIOD_MS = 30_000L
    }
}

