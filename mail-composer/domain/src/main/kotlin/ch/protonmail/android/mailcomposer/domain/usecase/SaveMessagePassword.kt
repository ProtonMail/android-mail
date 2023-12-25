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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class SaveMessagePassword @Inject constructor(
    private val keyStoreCrypto: KeyStoreCrypto,
    private val messagePasswordRepository: MessagePasswordRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        password: String,
        passwordHint: String?
    ): Either<DataError.Local, Unit> {
        val encryptedPassword = runCatching { keyStoreCrypto.encrypt(password) }.fold(
            onSuccess = { it },
            onFailure = { return DataError.Local.EncryptionError.left() }
        )
        return messagePasswordRepository.saveMessagePassword(
            MessagePassword(userId, messageId, encryptedPassword, passwordHint)
        )
    }
}
