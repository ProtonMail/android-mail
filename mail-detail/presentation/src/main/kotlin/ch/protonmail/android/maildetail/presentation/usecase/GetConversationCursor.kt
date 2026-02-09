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

package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailcommon.domain.model.EphemeralMailboxCursor
import ch.protonmail.android.mailcommon.domain.repository.EphemeralMailboxCursorRepository
import ch.protonmail.android.mailmailbox.domain.usecase.SetEphemeralMailboxCursor
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Relies on the owner of a scroller such as Mailbox having called [SetEphemeralMailboxCursor] before its lifecycle
 * was completed.  The new screen can use this command to access the cursor.  If no Cursor has been set (in the case
 * of notifications) then the command will try to self recover by retriving and setting a cursor itself which should not
 * be problematic in the case of notifications since we do not need scroller state such as next pages
 */
class GetConversationCursor @Inject constructor(
    private var cursorRepository: EphemeralMailboxCursorRepository,
    private var setEphemeralMailboxCursor: SetEphemeralMailboxCursor
) {

    suspend operator fun invoke(
        userId: UserId,
        singleMessageModePreferred: Boolean,
        conversationId: ConversationId,
        messageId: String?,
        locationViewModeIsConversation: Boolean
    ) = cursorRepository.observeCursor()
        .map { state ->
            val shouldInitializeCursor = state == null ||
                state == EphemeralMailboxCursor.NotInitalised ||
                isCursorForDifferentConversation(state, conversationId)

            if (shouldInitializeCursor) {
                setEphemeralMailboxCursor(
                    userId, locationViewModeIsConversation,
                    CursorId(conversationId, messageId)
                )
                EphemeralMailboxCursor.Initialising
            } else {
                state
            }
        }

    private fun isCursorForDifferentConversation(
        state: EphemeralMailboxCursor,
        newConversationId: ConversationId
    ): Boolean {
        return state is EphemeralMailboxCursor.Data &&
            state.cursor.current is CursorResult.Cursor &&
            (state.cursor.current as CursorResult.Cursor).conversationId != newConversationId
    }
}
