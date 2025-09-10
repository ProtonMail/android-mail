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

package ch.protonmail.android.mailconversation.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface RustConversationDetailQuery {
    suspend fun observeConversation(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId
    ): Flow<Either<ConversationError, LocalConversation>>

    suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId
    ): Flow<Either<ConversationError, LocalConversationMessages>>
}
