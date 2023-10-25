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

package ch.protonmail.android.composer.data.usecase

import java.io.File
import arrow.core.Either
import arrow.core.raise.either
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.SignaturePacket
import me.proton.core.key.domain.encryptFile
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.generateNewSessionKey
import me.proton.core.key.domain.getUnarmored
import me.proton.core.key.domain.signFile
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.entity.UserAddress
import javax.inject.Inject

class EncryptAndSignAttachment @Inject constructor(
    private val cryptoContext: CryptoContext
) {

    suspend operator fun invoke(
        senderAddress: UserAddress,
        attachment: File
    ): Either<AttachmentEncryptionError, EncryptedAttachmentResult> = either {
        senderAddress.useKeys(cryptoContext) {
            val sessionKey = runCatching {
                generateNewSessionKey()
            }.fold(
                onSuccess = { it },
                onFailure = { shift(AttachmentEncryptionError.FailedToGenerateSessionKey(it)) }
            )

            val keyPacket = runCatching { encryptSessionKey(sessionKey) }.fold(
                onSuccess = { it },
                onFailure = { shift(AttachmentEncryptionError.FailedToEncryptSessionKey(it)) }
            )

            val encryptedAttachment =
                runCatching { encryptFile(attachment.name, attachment.inputStream(), keyPacket) }.fold(
                    onSuccess = { it },
                    onFailure = { shift(AttachmentEncryptionError.FailedToEncryptAttachment(it)) }
                )

            val signature = runCatching { getUnarmored(signFile(attachment)) }.fold(
                onSuccess = { it },
                onFailure = { shift(AttachmentEncryptionError.FailedToSignAttachment(it)) }
            )

            EncryptedAttachmentResult(
                keyPacket = keyPacket,
                encryptedAttachment = encryptedAttachment,
                signature = signature
            )
        }
    }
}

data class EncryptedAttachmentResult(
    val keyPacket: KeyPacket,
    val encryptedAttachment: File,
    val signature: SignaturePacket
)

sealed class AttachmentEncryptionError(open val exception: Throwable) {
    data class FailedToGenerateSessionKey(override val exception: Throwable) :
        AttachmentEncryptionError(exception)

    data class FailedToEncryptSessionKey(override val exception: Throwable) :
        AttachmentEncryptionError(exception)

    data class FailedToEncryptAttachment(override val exception: Throwable) :
        AttachmentEncryptionError(exception)

    data class FailedToSignAttachment(override val exception: Throwable) :
        AttachmentEncryptionError(exception)
}
