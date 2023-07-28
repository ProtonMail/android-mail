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

package ch.protonmail.android.composer.data.remote

import arrow.core.Either
import ch.protonmail.android.composer.data.remote.resource.CreateDraftBody
import ch.protonmail.android.composer.data.remote.resource.DraftMessageResource
import ch.protonmail.android.composer.data.remote.resource.UpdateDraftBody
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.util.kotlin.toInt
import javax.inject.Inject

class DraftRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider
) : DraftRemoteDataSource {

    override suspend fun create(
        userId: UserId,
        messageWithBody: MessageWithBody,
        action: DraftAction
    ): Either<DataError.Remote, MessageWithBody> {
        val parentId = when (action) {
            is DraftAction.Compose -> null
            is DraftAction.Reply -> action.parentId
            is DraftAction.ReplyAll -> action.parentId
            is DraftAction.Forward -> action.parentId
        }
        val body = CreateDraftBody(
            messageWithBody.toDraftMessageResource(),
            parentId?.id,
            action.toApiInt(),
            emptyList()
        )

        return apiProvider.get<DraftApi>(userId).invoke {
            createDraft(body).message.toMessageWithBody(userId)
        }.toEither()
    }

    override suspend fun update(
        userId: UserId,
        messageWithBody: MessageWithBody
    ): Either<DataError.Remote, MessageWithBody> {
        val messageId = messageWithBody.message.messageId
        val body = UpdateDraftBody(
            messageWithBody.toDraftMessageResource(),
            emptyList()
        )

        return apiProvider.get<DraftApi>(userId).invoke {
            updateDraft(messageId.id, body).message.toMessageWithBody(userId)
        }.toEither()
    }

    private fun MessageWithBody.toDraftMessageResource() = DraftMessageResource(
        subject = this.message.subject,
        this.message.unread.toInt(),
        with(this.message.sender) { RecipientResource(address, name) },
        this.message.toList.map { RecipientResource(it.address, it.name) },
        this.message.ccList.map { RecipientResource(it.address, it.name) },
        this.message.bccList.map { RecipientResource(it.address, it.name) },
        this.message.externalId,
        this.message.flags,
        this.messageBody.body,
        this.messageBody.mimeType.value
    )
}
