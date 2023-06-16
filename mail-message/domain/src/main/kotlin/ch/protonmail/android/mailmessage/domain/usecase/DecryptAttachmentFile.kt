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

import java.io.File
import java.io.IOException
import android.content.Context
import android.media.MediaCodec.CryptoException
import android.os.ParcelFileDescriptor
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.system.ParcelFileDescriptorProvider
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachment
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.key.domain.decryptFile
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecryptAttachmentFile @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attachmentRepository: AttachmentRepository,
    private val messageRepository: MessageRepository,
    private val cryptoContext: CryptoContext,
    private val userAddressManager: UserAddressManager,
    private val parcelFileDescriptorProvider: ParcelFileDescriptorProvider,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    @Throws(IOException::class, IllegalArgumentException::class, CryptoException::class)
    suspend operator fun invoke(attachmentHash: String): ParcelFileDescriptor = withContext(ioDispatcher) {
        val attachmentDetails = getAttachmentDetails(attachmentHash)
        val message = getMessage(attachmentDetails)
        val userAddress = getUserAddress(attachmentDetails, message)
        val messageAttachment = getMessageAttachment(attachmentDetails, message)
        val decryptedFile = getDecryptedFile(attachmentDetails, userAddress, messageAttachment)

        parcelFileDescriptorProvider.provideParcelFileDescriptor(decryptedFile)
    }

    private suspend fun getAttachmentDetails(attachmentHash: String): MessageAttachmentMetadata {
        return attachmentRepository.getAttachmentMetadataByHash(attachmentHash)
            .getOrElse { throw IllegalArgumentException("Attachment with hash $attachmentHash not found") }
    }

    private suspend fun getMessage(attachmentDetails: MessageAttachmentMetadata): MessageWithBody {
        return messageRepository.getMessageWithBody(
            attachmentDetails.userId, attachmentDetails.messageId
        ).getOrNull() ?: throw IllegalArgumentException("Message with body not found")
    }

    private suspend fun getUserAddress(
        attachmentDetails: MessageAttachmentMetadata,
        message: MessageWithBody
    ): UserAddress {
        return userAddressManager.getAddress(attachmentDetails.userId, message.message.addressId)
            ?: throw IllegalArgumentException("User address not found")
    }

    private fun getMessageAttachment(
        attachmentDetails: MessageAttachmentMetadata,
        message: MessageWithBody
    ): MessageAttachment {
        return message.messageBody.attachments.firstOrNull { it.attachmentId == attachmentDetails.attachmentId }
            ?: throw IllegalArgumentException("Message attachment not found")
    }

    private fun getDecryptedFile(
        attachmentDetails: MessageAttachmentMetadata,
        userAddress: UserAddress,
        messageAttachment: MessageAttachment
    ): File {
        requireNotNull(attachmentDetails.path) { "Attachment path not found" }
        requireNotNull(messageAttachment.keyPackets) { "Key packets not found" }

        val fileExtension = messageAttachment.name.split(".").last()
        val encryptedFile = File(attachmentDetails.path)
        val decryptedFile = runCatching {
            File.createTempFile(attachmentDetails.attachmentId.id, fileExtension, context.cacheDir)
        }.getOrNull() ?: throw IOException("Decrypted temporary file could not be created")

        userAddress.useKeys(cryptoContext) {
            this.decryptFile(
                source = encryptedFile,
                destination = decryptedFile,
                keyPacket = cryptoContext.pgpCrypto.getBase64Decoded(messageAttachment.keyPackets)
            )
        }
        return decryptedFile
    }

}
