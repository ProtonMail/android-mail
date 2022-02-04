/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.domain.repository

import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import me.proton.core.domain.entity.UserId

interface MessageRemoteDataSource {
    /**
     * Get all [Message] for [userId].
     */
    suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey,
    ): List<Message>

    /**
     * Get a [Message] for [userId], by [messageId].
     */
    suspend fun getMessage(
        userId: UserId,
        messageId: MessageId,
    ): MessageWithBody
}
