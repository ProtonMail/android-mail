/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcommon.domain.repository

import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult

/**
 * A cursor over just the opened item, so the detail screen has nothing to swipe to.
 *
 * Used when there is no list to walk through, such as a recent search result.
 */
class SingleItemConversationCursor(anchorItemId: CursorId) : ConversationCursor {

    override val current: CursorResult = CursorResult.Cursor(anchorItemId.conversationId, anchorItemId.messageId)
    override val next: CursorResult = CursorResult.End
    override val previous: CursorResult = CursorResult.End

    override suspend fun moveForward() = Unit
    override suspend fun moveBackward() = Unit
    override fun close() = Unit
    override suspend fun invalidatePrevious() = Unit
}
