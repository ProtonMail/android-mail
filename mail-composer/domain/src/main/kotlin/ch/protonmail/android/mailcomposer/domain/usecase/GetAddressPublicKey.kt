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
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.AddressPublicKey
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.extension.primary
import me.proton.core.key.domain.fingerprint
import me.proton.core.key.domain.publicKey
import javax.inject.Inject

class GetAddressPublicKey @Inject constructor(
    private val resolveUserAddress: ResolveUserAddress,
    private val cryptoContext: CryptoContext,
    @IODispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(userId: UserId, email: SenderEmail): Either<Error, AddressPublicKey> =
        withContext(dispatcher) {
            either {
                val address = resolveUserAddress(userId, email.value)
                    .mapLeft { Error.AddressNotFound }
                    .bind()

                val publicKey = ensureNotNull(
                    runCatching { address.keys.primary()?.privateKey?.publicKey(cryptoContext) }.getOrNull()
                ) {
                    Error.PublicKeyNotFound
                }
                val fingerprint = ensureNotNull(runCatching { publicKey.fingerprint(cryptoContext) }.getOrNull()) {
                    Error.PublicKeyFingerprint
                }

                val fingerprintUppercase8Chars = fingerprint.substring(0, 8).uppercase()
                val fileName = "publickey - ${address.email} - 0x$fingerprintUppercase8Chars.asc"
                val mimeType = "application/pgp-keys"

                AddressPublicKey(
                    fileName,
                    mimeType,
                    publicKey.key.toByteArray()
                )
            }
        }

    sealed interface Error {
        data object AddressNotFound : Error
        data object PublicKeyNotFound : Error
        data object PublicKeyFingerprint : Error
    }
}
