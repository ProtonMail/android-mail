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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.DecryptedMimeAttachment
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptMimeMessage
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

class GetDecryptedMessageBody @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val cryptoContext: CryptoContext,
    private val messageRepository: MessageRepository,
    private val parseMimeAttachmentHeaders: ParseMimeAttachmentHeaders,
    private val provideNewAttachmentId: ProvideNewAttachmentId,
    private val userAddressManager: UserAddressManager,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId
    ): Either<GetDecryptedMessageBodyError, DecryptedMessageBody> {
        return withContext(dispatcher) {
            messageRepository.getMessageWithBody(userId, messageId)
                .mapLeft { GetDecryptedMessageBodyError.Data(it) }
                .flatMap { messageWithBody ->
                    val addressId = messageWithBody.message.addressId
                    val userAddress = userAddressManager.getAddress(userId, addressId)
                    val messageBody = messageWithBody.messageBody

                    return@flatMap if (userAddress != null) {
                        try {
                            userAddress.useKeys(cryptoContext) {
                                decryptMessageBody(userAddress, messageBody)
                            }.right()
                        } catch (cryptoException: CryptoException) {
                            Timber.e(cryptoException, "Error decrypting message")
                            GetDecryptedMessageBodyError.Decryption(messageId, messageBody.body).left()
                        }
                    } else {
                        Timber.e("Decryption error. Could not get UserAddress for user id ${userId.id}")
                        GetDecryptedMessageBodyError.Decryption(messageId, messageBody.body).left()
                    }
                }
        }
    }

    private suspend fun KeyHolderContext.decryptMessageBody(
        userAddress: UserAddress,
        messageBody: MessageBody
    ): DecryptedMessageBody {
        return if (messageBody.mimeType == MimeType.MultipartMixed) {
            decryptMimeMessage(messageBody.body).run {
                val attachments = this.attachments.map {
                    val attachmentId = provideNewAttachmentId()
                    it.saveAttachmentToCache(messageBody.userId, messageBody.messageId, attachmentId)
                }
                DecryptedMessageBody(
                    messageId = messageBody.messageId,
                    value = body.content,
                    mimeType = MimeType.from(body.mimeType),
                    attachments = attachments,
                    userAddress = userAddress
                )
            }
        } else {
            decryptText(messageBody.body).run {
                DecryptedMessageBody(
                    messageId = messageBody.messageId,
                    value = this,
                    mimeType = messageBody.mimeType,
                    attachments = messageBody.attachments,
                    userAddress = userAddress
                )
            }
        }
    }

    private suspend fun DecryptedMimeAttachment.saveAttachmentToCache(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): MessageAttachment = toMessageAttachment(attachmentId).also {
        attachmentRepository.saveMimeAttachment(userId, messageId, attachmentId, content, it)
    }

    private fun DecryptedMimeAttachment.toMessageAttachment(attachmentId: AttachmentId): MessageAttachment {
        val size = content.size.toLong()
        return parseMimeAttachmentHeaders(headers).run {
            val name = getOrElse(FilenameKey) {
                getOrDefault(NameKey, JsonPrimitive(EMPTY_STRING))
            }.jsonPrimitive.content
            val mimeType = getOrDefault(ContentTypeKey, JsonPrimitive(EMPTY_STRING)).jsonPrimitive.content
            val disposition = getOrDefault(ContentDispositionKey, JsonPrimitive(EMPTY_STRING)).jsonPrimitive.content
            MessageAttachment(
                attachmentId = attachmentId,
                name = name,
                size = size,
                mimeType = mimeType,
                disposition = disposition,
                keyPackets = null,
                signature = null,
                encSignature = null,
                headers = emptyMap()
            )
        }
    }

    companion object {

        private const val FilenameKey = "filename"
        private const val NameKey = "name"
        private const val ContentTypeKey = "Content-Type"
        private const val ContentDispositionKey = "Content-Disposition"
    }
}
