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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.model.AddressPublicKey
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetAddressPublicKeyTest {

    private val resolveUserAddress = mockk<ResolveUserAddress>()

    private val mockedSessionKey = SessionKey("mockedSessionKey".encodeToByteArray())
    private val mockedKeyPacket = "mockedKeyPacket".encodeToByteArray()

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()

    private val userAddressKey = mockk<UserAddressKey> {
        every { privateKey } returns PrivateKey(
            key = armoredPrivateKey,
            isPrimary = true,
            isActive = true,
            canEncrypt = true,
            canVerify = true,
            passphrase = encryptedPassphrase
        )
    }

    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { generateNewSessionKey() } returns mockedSessionKey
        every { encryptSessionKey(mockedSessionKey, armoredPublicKey) } returns mockedKeyPacket
        every { decryptSessionKey(mockedKeyPacket, unlockedPrivateKey) } returns mockedSessionKey
    }

    private val cryptoContext = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }

    private val testDispatcher: TestDispatcher by lazy {
        StandardTestDispatcher()
    }

    private val getAddressPublicKey = GetAddressPublicKey(
        resolveUserAddress,
        cryptoContext,
        testDispatcher
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should return AddressNotFound when the user address fails to be resolved`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(UserId, SenderEmail.value)
        } returns ResolveUserAddress.Error.UserAddressNotFound.left()

        // When
        val result = getAddressPublicKey(UserId, SenderEmail)

        // Then
        assertEquals(GetAddressPublicKey.Error.AddressNotFound.left(), result)
    }

    @Test
    fun `should return PublicKeyNotFound when the key can't be obtained from the crypto context`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(UserId, SenderEmail.value)
        } returns UserAddressSample.AliasAddress.copy(keys = listOf()).right()

        // When
        val result = getAddressPublicKey(UserId, SenderEmail)

        // Then
        assertEquals(GetAddressPublicKey.Error.PublicKeyNotFound.left(), result)
    }

    @Test
    fun `should return PublicKeyNotFound when the PK can't be obtained`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(UserId, SenderEmail.value)
        } returns UserAddressSample.AliasAddress.copy(keys = listOf(userAddressKey)).right()

        every { cryptoContext.pgpCrypto.getPublicKey(armoredPrivateKey) } throws Exception()
        every { cryptoContext.pgpCrypto.getFingerprint(armoredPublicKey) }

        // When
        val result = getAddressPublicKey(UserId, SenderEmail)

        // Then
        assertEquals(GetAddressPublicKey.Error.PublicKeyNotFound.left(), result)
    }

    @Test
    fun `should return PublicKeyNotFound when the PK fingerprint can't be obtained`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(UserId, SenderEmail.value)
        } returns UserAddressSample.AliasAddress.copy(keys = listOf(userAddressKey)).right()

        every { cryptoContext.pgpCrypto.getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { cryptoContext.pgpCrypto.getFingerprint(armoredPublicKey) } throws Exception()

        // When
        val result = getAddressPublicKey(UserId, SenderEmail)

        // Then
        assertEquals(GetAddressPublicKey.Error.PublicKeyFingerprint.left(), result)
    }

    @Test
    fun `should return a valid AddressPublicKey when invoked`() = runTest {
        // Given
        coEvery {
            resolveUserAddress(UserId, SenderEmail.value)
        } returns UserAddressSample.AliasAddress.copy(keys = listOf(userAddressKey)).right()

        every { cryptoContext.pgpCrypto.getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { cryptoContext.pgpCrypto.getFingerprint(armoredPublicKey) } returns "01A2b3C4D5E"

        val expectedAddressPublicKey = AddressPublicKey(
            fileName = "publickey - alias@protonmail.ch - 0x01A2B3C4.asc",
            mimeType = "application/pgp-keys",
            bytes = armoredPublicKey.toByteArray()
        )

        // When
        val result = getAddressPublicKey(UserId, SenderEmail).getOrNull()

        // Then
        assertEquals(expectedAddressPublicKey, result)
    }

    private companion object {

        val UserId = UserIdSample.Primary
        val SenderEmail = SenderEmail(UserAddressSample.PrimaryAddress.email)
    }
}
