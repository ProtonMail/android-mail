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

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface MessagePasswordRepository {

    suspend fun saveMessagePassword(messagePassword: MessagePassword): Either<DataError.Local, Unit>

    suspend fun updateMessagePassword(
        userId: UserId,
        messageId: MessageId,
        password: String,
        passwordHint: String?
    ): Either<DataError.Local, Unit>

    suspend fun observeMessagePassword(userId: UserId, messageId: MessageId): Flow<MessagePassword?>

    suspend fun deleteMessagePassword(userId: UserId, messageId: MessageId)
}
