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
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.key.domain.encryptAndSignText
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.entity.UserAddress
import timber.log.Timber
import javax.inject.Inject

class EncryptDraftBody @Inject constructor(
    private val cryptoContext: CryptoContext,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(draftBody: DraftBody, senderAddress: UserAddress): Either<Unit, DraftBody> {
        return withContext(defaultDispatcher) {
            senderAddress.useKeys(cryptoContext) {
                try {
                    DraftBody(encryptAndSignText(draftBody.value)).right()
                } catch (cryptoException: CryptoException) {
                    Timber.e("Failed to encrypt the message body", cryptoException)
                    Unit.left()
                }
            }
        }
    }
}
