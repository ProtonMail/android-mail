package ch.protonmail.android.mailsession.data.keychain

import java.security.GeneralSecurityException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import org.junit.Test
import uniffi.mail_uniffi.OsKeyChainEntryKind
import uniffi.mail_uniffi.OsKeyChainException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AndroidKeyChainTest {

    private val keyChainLocalDataSource = mockk<KeyChainLocalDataSource>()
    private val keyStoreCrypto = mockk<KeyStoreCrypto>()

    private val keyChain = AndroidKeyChain(
        keyChainLocalDataSource,
        keyStoreCrypto
    )

    @Test
    fun `encrypts data before storing to insecure storage`() {
        // Given
        val type = OsKeyChainEntryKind.ENCRYPTION_KEY
        val secret = "secret-key"
        val encryptedSecret = "can't touch this"
        coEvery { keyStoreCrypto.encrypt(secret) } returns encryptedSecret
        coEvery { keyChainLocalDataSource.save(type, encryptedSecret) } returns Unit.right()

        // When
        keyChain.store(type, secret)

        // Then
        coVerify { keyChainLocalDataSource.save(type, encryptedSecret) }
    }

    @Test
    fun `decrypts data when reading from insecure storage`() {
        // Given
        val type = OsKeyChainEntryKind.ENCRYPTION_KEY
        val expected = "secret-key"
        val encryptedSecret = "can't touch this"
        coEvery { keyStoreCrypto.decrypt(encryptedSecret) } returns expected
        coEvery { keyChainLocalDataSource.get(type) } returns encryptedSecret.right()

        // When
        val actual = keyChain.load(type)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `load returns null when no value is available in local storage`() {
        // Given
        val type = OsKeyChainEntryKind.ENCRYPTION_KEY
        coEvery { keyChainLocalDataSource.get(type) } returns null.right()

        // When
        val actual = keyChain.load(type)

        // Then
        assertNull(actual)
    }

    @Test
    fun `throws error when load fails reading from disk`() {
        // Given
        val type = OsKeyChainEntryKind.ENCRYPTION_KEY
        coEvery { keyChainLocalDataSource.get(type) } returns PreferencesError.left()

        // When / Then
        assertFailsWith<OsKeyChainException> { keyChain.load(type) }
    }

    @Test
    fun `throws error when load fails decrypting the secret`() {
        // Given
        val type = OsKeyChainEntryKind.ENCRYPTION_KEY
        val encryptedSecret = "can't touch this"
        coEvery { keyStoreCrypto.decrypt(encryptedSecret) } throws GeneralSecurityException("test - failed")
        coEvery { keyChainLocalDataSource.get(type) } returns encryptedSecret.right()

        // When / Then
        assertFailsWith<OsKeyChainException> { keyChain.load(type) }
    }

    @Test
    fun `throws error when store fails writing to disk`() = runTest {
        // Given
        val type = OsKeyChainEntryKind.DEVICE_KEY
        val secret = "secret-key"
        val encryptedSecret = "can't touch this"
        coEvery { keyStoreCrypto.encrypt(secret) } returns encryptedSecret
        coEvery { keyChainLocalDataSource.save(type, encryptedSecret) } returns PreferencesError.left()

        // When / Then
        assertFailsWith<OsKeyChainException> { keyChain.store(type, secret) }
    }

    @Test
    fun `throws error when store fails encrypting the secret`() {
        // Given
        val type = OsKeyChainEntryKind.DEVICE_KEY
        val secret = "secret-key"
        coEvery { keyStoreCrypto.encrypt(secret) } throws GeneralSecurityException("test - failed")

        // When / Then
        assertFailsWith<OsKeyChainException> { keyChain.store(type, secret) }
    }

    @Test
    fun `throws error when delete fails`() {
        // Given
        val type = OsKeyChainEntryKind.DEVICE_KEY
        coEvery { keyChainLocalDataSource.remove(type) } returns PreferencesError.left()

        // When / Then
        assertFailsWith<OsKeyChainException> { keyChain.delete(type) }
    }

}
