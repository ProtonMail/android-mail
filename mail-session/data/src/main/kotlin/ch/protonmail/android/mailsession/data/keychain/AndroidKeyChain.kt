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

package ch.protonmail.android.mailsession.data.keychain

import java.io.IOException
import kotlinx.coroutines.runBlocking
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import timber.log.Timber
import uniffi.mail_uniffi.OsKeyChain
import uniffi.mail_uniffi.OsKeyChainEntryKind
import uniffi.mail_uniffi.OsKeyChainException
import javax.inject.Inject

@SuppressWarnings("TooGenericExceptionCaught", "SwallowedException")
class AndroidKeyChain @Inject constructor(
    private val keyChainLocalDataSource: KeyChainLocalDataSource,
    private val keyStoreCrypto: KeyStoreCrypto
) : OsKeyChain {

    override fun delete(kind: OsKeyChainEntryKind) {
        runBlocking {
            try {
                keyChainLocalDataSource.remove(kind)
                    .onLeft { throw IOException("Failed to delete from data source. cause: $it") }
            } catch (exception: Exception) {
                Timber.e("android-keychain: failed to delete secret. Error: $exception")
                throw OsKeyChainException.Os(exception.message ?: exception.toString())
            }
        }
    }

    override fun load(kind: OsKeyChainEntryKind): String? = runBlocking {
        try {
            keyChainLocalDataSource.get(kind)
                .onLeft { throw IOException("Failed to read from data source. cause: $it") }
                .map { it?.let { keyStoreCrypto.decrypt(it) } }
                .getOrNull()

        } catch (exception: Exception) {
            Timber.e("android-keychain: failed to read or decrypt secret. Error: $exception")
            throw OsKeyChainException.Os(exception.message ?: exception.toString())
        }
    }

    override fun store(kind: OsKeyChainEntryKind, key: String) {
        runBlocking {
            try {
                val encryptedString = keyStoreCrypto.encrypt(key)
                keyChainLocalDataSource.save(kind, encryptedString)
                    .onLeft { throw IOException("Failed to write to data source. cause: $it") }
            } catch (exception: Exception) {
                Timber.e("android-keychain: failed to encrypt or write secret. Error: $exception")
                throw OsKeyChainException.Os(exception.message ?: exception.toString())
            }
        }
    }

}
