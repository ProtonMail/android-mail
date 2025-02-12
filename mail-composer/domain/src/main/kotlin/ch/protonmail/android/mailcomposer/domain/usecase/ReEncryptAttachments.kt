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
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcommon.domain.util.mapFalse
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptSessionKey
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.entity.AddressId
import timber.log.Timber
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class ReEncryptAttachments @Inject constructor(
    private val getLocalDraft: GetLocalDraft,
    private val messageRepository: MessageRepository,
    private val resolveUserAddress: ResolveUserAddress,
    private val cryptoContext: CryptoContext,
    private val transactor: Transactor
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        previousSender: SenderEmail,
        newSenderEmail: SenderEmail
    ): Either<AttachmentReEncryptionError, Unit> = transactor.performTransaction {
        Timber.d("Re encrypting attachments - $previousSender -> $newSenderEmail")
        either {
            val draft = getLocalDraft(userId, messageId, newSenderEmail)
                .mapLeft { AttachmentReEncryptionError.DraftNotFound }
                .bind()

            draft.messageBody.attachments
                .mapNotNull {
                    if (it.keyPackets.isNullOrEmpty().not())
                        AttachmentKeyPacket(it.attachmentId, it.keyPackets!!)
                    else
                        null
                }
                .takeIf { it.isNotEmpty() }
                ?.let { decryptKeyPackets(userId, previousSender, it).bind() }
                ?.let { encryptKeyPackets(userId, draft.message.addressId, it).bind() }
                ?.let { updateAttachmentKeyPackets(userId, draft, it).bind() }
        }
    }

    private suspend fun updateAttachmentKeyPackets(
        userId: UserId,
        message: MessageWithBody,
        reEncryptedKeyPackets: List<AttachmentKeyPacket>
    ): Either<AttachmentReEncryptionError, Boolean> {
        val updatedAttachments = message.messageBody.attachments
            .map { oldAttachment ->
                val reEncryptedKeyPackets = reEncryptedKeyPackets.firstOrNull {
                    it.attachmentId == oldAttachment.attachmentId
                }?.keyPacket
                    ?: return AttachmentReEncryptionError.FailedToUpdateAttachmentKeyPackets.left()

                oldAttachment.copy(keyPackets = reEncryptedKeyPackets)
            }

        val updatedMessage = message.copy(
            messageBody = message.messageBody.copy(
                attachments = updatedAttachments
            )
        )

        return messageRepository.upsertMessageWithBody(userId, updatedMessage)
            .mapFalse { AttachmentReEncryptionError.FailedToUpdateAttachmentKeyPackets }
            .fold(
                ifLeft = { AttachmentReEncryptionError.FailedToUpdateAttachmentKeyPackets.left() },
                ifRight = { true.right() }
            )
    }

    private suspend fun encryptKeyPackets(
        userId: UserId,
        newAddressId: AddressId,
        attachmentIdWithSessionKey: List<AttachmentSessionKey>
    ): Either<AttachmentReEncryptionError, List<AttachmentKeyPacket>> = resolveUserAddress(userId, newAddressId)
        .mapLeft { AttachmentReEncryptionError.FailedToResolveNewUserAddress }
        .map { userAddress ->
            userAddress.useKeys(cryptoContext) {
                return@map attachmentIdWithSessionKey.map {
                    try {
                        AttachmentKeyPacket(it.attachmentId, Base64.encode(encryptSessionKey(it.sessionKey)))
                    } catch (e: CryptoException) {
                        Timber.e(e, "Failed to encrypt attachment key packet")
                        return AttachmentReEncryptionError.FailedToEncryptAttachmentKeyPackets.left()
                    }
                }
            }
        }

    private suspend fun decryptKeyPackets(
        userId: UserId,
        oldAddressId: SenderEmail,
        attachmentIdWithKeyPackets: List<AttachmentKeyPacket>
    ): Either<AttachmentReEncryptionError, List<AttachmentSessionKey>> = resolveUserAddress(userId, oldAddressId.value)
        .mapLeft { AttachmentReEncryptionError.FailedToResolvePreviousUserAddress }
        .map { userAddress ->
            userAddress.useKeys(cryptoContext) {
                return@map attachmentIdWithKeyPackets.map {
                    try {
                        AttachmentSessionKey(it.attachmentId, decryptSessionKey(Base64.decode(it.keyPacket)))
                    } catch (e: CryptoException) {
                        Timber.e(e, "Failed to decrypt attachment key packet")
                        return AttachmentReEncryptionError.FailedToDecryptAttachmentKeyPackets.left()
                    }
                }
            }
        }

    private data class AttachmentKeyPacket(val attachmentId: AttachmentId, val keyPacket: String)
    private data class AttachmentSessionKey(val attachmentId: AttachmentId, val sessionKey: SessionKey)
}

sealed interface AttachmentReEncryptionError {
    object DraftNotFound : AttachmentReEncryptionError
    object FailedToResolvePreviousUserAddress : AttachmentReEncryptionError
    object FailedToResolveNewUserAddress : AttachmentReEncryptionError
    object FailedToDecryptAttachmentKeyPackets : AttachmentReEncryptionError
    object FailedToEncryptAttachmentKeyPackets : AttachmentReEncryptionError
    object FailedToUpdateAttachmentKeyPackets : AttachmentReEncryptionError
}
