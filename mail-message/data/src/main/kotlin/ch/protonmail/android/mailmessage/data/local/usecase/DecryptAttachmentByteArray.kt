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

package ch.protonmail.android.mailmessage.data.local.usecase

import java.io.IOException
import android.media.MediaCodec.CryptoException
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachment
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptData
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecryptAttachmentByteArray @Inject constructor(
    private val messageRepository: MessageRepository,
    private val cryptoContext: CryptoContext,
    private val userAddressManager: UserAddressManager,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    @Throws(IOException::class, IllegalArgumentException::class, CryptoException::class)
    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedByteArray: ByteArray
    ): ByteArray = withContext(ioDispatcher) {
        val message = getMessageBody(userId, messageId)
        val userAddress = getUserAddress(userId, message)
        val messageAttachment = getMessageAttachment(message, attachmentId)
        getDecryptedFile(userAddress, messageAttachment, encryptedByteArray)
    }

    private suspend fun getMessageBody(userId: UserId, messageId: MessageId): MessageWithBody {
        return messageRepository.getLocalMessageWithBody(userId, messageId)
            ?: throw IllegalArgumentException("Message with body not found")
    }

    private suspend fun getUserAddress(userId: UserId, message: MessageWithBody): UserAddress {
        return userAddressManager.getAddress(userId, message.message.addressId)
            ?: throw IllegalArgumentException("User address not found")
    }

    private fun getMessageAttachment(message: MessageWithBody, attachmentId: AttachmentId): MessageAttachment {
        return message.messageBody.attachments.firstOrNull { it.attachmentId == attachmentId }
            ?: throw IllegalArgumentException("Message attachment not found")
    }

    private fun getDecryptedFile(
        userAddress: UserAddress,
        messageAttachment: MessageAttachment,
        encryptedByteArray: ByteArray
    ): ByteArray {
        val keyPackets = requireNotNull(messageAttachment.keyPackets) { "Key packets not found" }
        return userAddress.useKeys(cryptoContext) {
            this.decryptData(encryptedByteArray, cryptoContext.pgpCrypto.getBase64Decoded(keyPackets))
        }
    }

}
