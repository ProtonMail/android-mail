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

package ch.protonmail.android.mailmessage.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Dao
interface OutboxDao {

    @Query(
        """
        SELECT MLE.messageId 
        FROM MessageLabelEntity MLE 
        LEFT JOIN DraftStateEntity DSE 
        ON MLE.userId = DSE.userId AND MLE.messageId = DSE.apiMessageId 
        WHERE MLE.userId = :userId AND MLE.labelId = (:outboxLabelId) AND 
        DSE.state IN (:outboxDraftSyncStates) 
        """
    )
    fun getMessagesInOutbox(
        userId: UserId,
        outboxLabelId: LabelId,
        outboxDraftSyncStates: List<DraftSyncState>
    ): Flow<List<MessageId>>

}
