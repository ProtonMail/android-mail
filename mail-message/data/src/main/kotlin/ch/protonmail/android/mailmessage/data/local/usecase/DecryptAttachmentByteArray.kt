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

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptData
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecryptAttachmentByteArray @Inject constructor(
    private val messageRepository: MessageRepository,
    private val cryptoContext: CryptoContext,
    private val userAddressManager: UserAddressManager,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedByteArray: ByteArray
    ): Either<AttachmentDecryptionError, ByteArray> = withContext(ioDispatcher) {
        val message = getMessageBody(userId, messageId).getOrElse { return@withContext it.left() }
        val userAddress = getUserAddress(userId, message).getOrElse { return@withContext it.left() }
        val messageAttachment = getMessageAttachment(message, attachmentId).getOrElse { return@withContext it.left() }
        getDecryptedFile(userAddress, messageAttachment, encryptedByteArray)
    }

    private suspend fun getMessageBody(
        userId: UserId,
        messageId: MessageId
    ): Either<AttachmentDecryptionError, MessageWithBody> {
        return messageRepository.getLocalMessageWithBody(userId, messageId)?.right()
            ?: AttachmentDecryptionError.MessageBodyNotFound.left()
    }

    private suspend fun getUserAddress(
        userId: UserId,
        message: MessageWithBody
    ): Either<AttachmentDecryptionError, UserAddress> {
        return userAddressManager.getAddress(userId, message.message.addressId)?.right()
            ?: AttachmentDecryptionError.UserAddressNotFound.left()
    }

    private fun getMessageAttachment(
        message: MessageWithBody,
        attachmentId: AttachmentId
    ): Either<AttachmentDecryptionError, MessageAttachment> {
        return message.messageBody.attachments.firstOrNull { it.attachmentId == attachmentId }?.right()
            ?: AttachmentDecryptionError.MessageAttachmentNotFound.left()
    }

    private fun getDecryptedFile(
        userAddress: UserAddress,
        messageAttachment: MessageAttachment,
        encryptedByteArray: ByteArray
    ): Either<AttachmentDecryptionError, ByteArray> {
        val keyPackets = messageAttachment.keyPackets ?: return AttachmentDecryptionError.KeyPacketsNotFound.left()
        return try {
            userAddress.useKeys(cryptoContext) {
                this.decryptData(encryptedByteArray, cryptoContext.pgpCrypto.getBase64Decoded(keyPackets)).right()
            }
        } catch (cryptoException: CryptoException) {
            Timber.d(cryptoException, "Failed to decrypt attachment")
            AttachmentDecryptionError.DecryptionFailed.left()
        }
    }
}

sealed interface AttachmentDecryptionError {
    object MessageBodyNotFound : AttachmentDecryptionError
    object MessageAttachmentNotFound : AttachmentDecryptionError
    object UserAddressNotFound : AttachmentDecryptionError
    object KeyPacketsNotFound : AttachmentDecryptionError
    object DecryptionFailed : AttachmentDecryptionError
}
