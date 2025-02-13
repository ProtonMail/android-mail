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

package ch.protonmail.android.mailcomposer.presentation.facade

import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessageExpirationTime
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.time.Duration

class MessageAttributesFacade @Inject constructor(
    private val observeMessagePassword: ObserveMessagePassword,
    private val observeMessageExpiration: ObserveMessageExpirationTime,
    private val saveMessageExpirationTime: SaveMessageExpirationTime
) {

    suspend fun observeMessagePassword(userId: UserId, messageId: MessageId) =
        observeMessagePassword.invoke(userId, messageId)

    suspend fun observeMessageExpiration(userId: UserId, messageId: MessageId) =
        observeMessageExpiration.invoke(userId, messageId)

    suspend fun saveMessageExpiration(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail,
        expiration: Duration
    ) = saveMessageExpirationTime.invoke(userId, messageId, senderEmail, expiration)
}
