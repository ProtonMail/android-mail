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

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetLocalDraft @Inject constructor(
    private val createEmptyDraft: CreateEmptyDraft,
    private val findLocalDraft: FindLocalDraft,
    private val resolveUserAddress: ResolveUserAddress
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        senderEmail: SenderEmail
    ): Either<Error, MessageWithBody> = either {
        val senderAddress = resolveUserAddress(userId, senderEmail.value)
            .mapLeft { Error.ResolveUserAddressError }
            .bind()

        return@either findLocalDraft(userId, messageId)
            ?: createEmptyDraft(messageId, userId, senderAddress)
    }

    sealed interface Error {
        object ResolveUserAddressError : Error
    }
}
