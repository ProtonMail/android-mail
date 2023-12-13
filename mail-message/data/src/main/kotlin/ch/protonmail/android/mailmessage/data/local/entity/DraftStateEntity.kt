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

package ch.protonmail.android.mailmessage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "messageId"],
    indices = [
        Index("userId"),
        Index("userId", "messageId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
        /* No Foreign Key on messageId as this table should preserve the local `messageId` also after
         * the MessageEntity was updated with the api-assigned one.
         */
    ]
)
data class DraftStateEntity(
    val userId: UserId,
    val messageId: MessageId,
    val apiMessageId: MessageId?,
    val state: DraftSyncState,
    val action: DraftAction,
    val sendingError: SendingError?,
    val sendingStatusConfirmed: Boolean = false
) {
    fun toDraftState() = DraftState(
        userId = userId,
        messageId = messageId,
        apiMessageId = apiMessageId,
        state = state,
        action = action,
        sendingError = sendingError,
        sendingStatusConfirmed = sendingStatusConfirmed
    )
}

fun DraftState.toDraftStateEntity() = DraftStateEntity(
    userId = this.userId,
    messageId = this.messageId,
    apiMessageId = this.apiMessageId,
    state = this.state,
    action = this.action,
    sendingError = this.sendingError,
    sendingStatusConfirmed = this.sendingStatusConfirmed
)
