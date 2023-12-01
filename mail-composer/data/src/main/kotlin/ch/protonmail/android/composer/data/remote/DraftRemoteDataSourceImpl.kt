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
import arrow.core.left
import ch.protonmail.android.composer.data.remote.resource.CreateDraftBody
import ch.protonmail.android.composer.data.remote.resource.DraftMessageResource
import ch.protonmail.android.composer.data.remote.resource.UpdateDraftBody
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
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
        val body = CreateDraftBody(
            messageWithBody.toDraftMessageResource(),
            action.getParentMessageId()?.id,
            action.toApiInt(),
            messageWithBody.buildAttachmentKeyPackets()
        )

        if (body.message.body.isBlank()) {
            /*
             * The API doesn't accept being called with an empty body.
             * Body is empty when the draft was just created and any data (subject, recipients..) was added
             * but the body wasn't. In order to avoid adding ad-hoc logic to create an encrypted empty-string
             * body, we block the draft creation (triggered by the automatic sync) from happening till a body was added
             */
            return DataError.Remote.CreateDraftRequestNotPerformed.left()
        }

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
            messageWithBody.buildAttachmentKeyPackets()
        )

        return apiProvider.get<DraftApi>(userId).invoke {
            updateDraft(messageId.id, body).message.toMessageWithBody(userId)
        }.toEither()
    }

    private fun MessageWithBody.buildAttachmentKeyPackets(): Map<String, String> =
        this.messageBody.attachments.filter { it.keyPackets != null }.associate {
            it.attachmentId.id to it.keyPackets!!
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
