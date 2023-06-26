package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.AddressIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.entity.UserAddressKey
import org.junit.Test
import kotlin.test.assertEquals

class EncryptDraftBodyTest {

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()
    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
    }
    private val cryptoContextMock = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }
    private val userAddressKey = UserAddressKey(
        addressId = AddressIdSample.Primary,
        version = 0,
        flags = 0,
        active = true,
        keyId = KeyId("KeyId"),
        privateKey = PrivateKey(
            key = armoredPrivateKey,
            isPrimary = true,
            isActive = true,
            canEncrypt = true,
            canVerify = true,
            passphrase = encryptedPassphrase
        )
    )
    private val encryptDraftBody = EncryptDraftBody(cryptoContextMock, UnconfinedTestDispatcher())

    @Test
    fun `should return error when body encryption fails`() = runTest {
        // Given
        val draftBody = DraftBody("Message body")
        val senderAddress = UserAddressSample.build().copy(keys = listOf(userAddressKey))
        givenBodyEncryptionFailsFor(draftBody)

        // When
        val encryptionResultEither = encryptDraftBody(draftBody, senderAddress)

        // Then
        assertEquals(Unit.left(), encryptionResultEither)
    }

    @Test
    fun `should return encrypted draft body when encryption succeeds`() = runTest {
        // Given
        val draftBody = DraftBody("Message body")
        val expectedEncryptedDraftBody = DraftBody("Encrypted message body")
        val senderAddress = UserAddressSample.build().copy(keys = listOf(userAddressKey))
        givenBodyEncryptionSucceedsFor(draftBody, encryptedDraftBody = expectedEncryptedDraftBody)

        // When
        val encryptionResultEither = encryptDraftBody(draftBody, senderAddress)

        // Then
        assertEquals(expectedEncryptedDraftBody.right(), encryptionResultEither)
    }

    private fun givenBodyEncryptionFailsFor(draftBody: DraftBody) {
        every { pgpCryptoMock.encryptAndSignText(draftBody.value, any(), any()) } throws CryptoException()
    }

    private fun givenBodyEncryptionSucceedsFor(draftBody: DraftBody, encryptedDraftBody: DraftBody) {
        every {
            pgpCryptoMock.encryptAndSignText(draftBody.value, any(), any())
        } returns encryptedDraftBody.value
    }
}
