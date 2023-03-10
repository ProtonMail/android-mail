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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.entity.MessageBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptMimeMessage
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserAddressManager
import timber.log.Timber
import javax.inject.Inject

class GetDecryptedMessageBody @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val messageRepository: MessageRepository,
    private val userAddressManager: UserAddressManager
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId
    ): Either<GetDecryptedMessageBodyError, DecryptedMessageBody> {
        return messageRepository.getMessageWithBody(userId, messageId)
            .mapLeft { GetDecryptedMessageBodyError.Data(it) }
            .flatMap { messageWithBody ->
                val addressId = messageWithBody.message.addressId
                val userAddress = userAddressManager.getAddress(userId, addressId)
                val messageBody = messageWithBody.messageBody

                return@flatMap if (userAddress != null) {
                    try {
                        userAddress.useKeys(cryptoContext) {
                            decryptMessageBody(messageBody)
                        }.right()
                    } catch (cryptoException: CryptoException) {
                        Timber.e(cryptoException, "Error decrypting message")
                        GetDecryptedMessageBodyError.Decryption(messageBody.body).left()
                    }
                } else {
                    Timber.e("Decryption error. Could not get UserAddress for user id ${userId.id}")
                    GetDecryptedMessageBodyError.Decryption(messageBody.body).left()
                }
            }
    }

    private fun KeyHolderContext.decryptMessageBody(messageBody: MessageBody): DecryptedMessageBody {
        return if (messageBody.mimeType == MimeType.MultipartMixed) {
            decryptMimeMessage(messageBody.body).run {
                DecryptedMessageBody(body.content, messageBody.mimeType)
            }
        } else {
            decryptText(messageBody.body).run {
                DecryptedMessageBody(this, messageBody.mimeType)
            }
        }
    }
}
