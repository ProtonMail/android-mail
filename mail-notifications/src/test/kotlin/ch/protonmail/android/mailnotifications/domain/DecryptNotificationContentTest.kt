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

package ch.protonmail.android.mailnotifications.domain

import java.util.UUID
import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationAction
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotification
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationData
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationSender
import ch.protonmail.android.mailnotifications.domain.usecase.content.DecryptNotificationContent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.entity.UserKey
import org.junit.Test
import kotlin.test.assertEquals

internal class DecryptNotificationContentTest {

    private val armoredPrivateKey = "armoredPrivateKey"
    private val armoredPublicKey = "armoredPublicKey"
    private val encryptedPassphrase = EncryptedByteArray("encryptedPassphrase".encodeToByteArray())
    private val decryptedPassphrase = PlainByteArray("decryptedPassPhrase".encodeToByteArray())
    private val unlockedPrivateKey = "unlockedPrivateKey".encodeToByteArray()

    private val key: UserKey = mockk {
        coEvery { privateKey } returns PrivateKey(
            key = armoredPrivateKey,
            isPrimary = true,
            isActive = true,
            canEncrypt = true,
            canVerify = true,
            passphrase = encryptedPassphrase
        )
    }
    private val user: User = mockk {
        coEvery { keys } returns listOf(key)
    }
    private val pgpCryptoMock = mockk<PGPCrypto> {
        every { getPublicKey(armoredPrivateKey) } returns armoredPublicKey
        every { unlock(armoredPrivateKey, decryptedPassphrase.array) } returns mockk(relaxUnitFun = true) {
            every { value } returns unlockedPrivateKey
        }
    }
    private val cryptoContext = mockk<CryptoContext> {
        every { pgpCrypto } returns pgpCryptoMock
        every { keyStoreCrypto } returns mockk {
            every { decrypt(encryptedPassphrase) } returns decryptedPassphrase
        }
    }
    private val userManager: UserManager = mockk {
        coEvery { getUser(any()) } returns user
    }

    @Test
    fun `Should return decryption error when the notification can not be decrypted`() = runTest {
        every { pgpCryptoMock.decryptMimeMessage(any(), any()) } throws CryptoException("")
        // Given
        val useCase = DecryptNotificationContent(cryptoContext, userManager)

        // When
        val result = useCase.invoke(UserId(UUID.randomUUID().toString()), "encrypted notification")

        // Then
        assert(result.isLeft())
    }

    @Test
    fun `Should return the decrypted notification when the notification can be decrypted`() = runTest {
        val serialised = """
            {"data":{"title":"ProtonMail","subtitle":"","body":"Notification","sender":
            {"Name":"Sender","Address":"SenderEmail","Group":""},"vibrate":1,"sound":1,"largeIcon":"large_icon",
            "smallIcon":"small_icon","badge":1,"messageId":"aMessageId","customId":"aCustomId",
            "action":"message_created"},
            "type":"email","version":2}
        """.trimIndent()
        val expected = PushNotification(
            type = "email",
            version = 2,
            data = PushNotificationData(
                title = "ProtonMail",
                subtitle = "",
                body = "Notification",
                sender = PushNotificationSender("SenderEmail", "Sender", ""),
                vibrate = 1,
                sound = 1,
                largeIcon = "large_icon",
                smallIcon = "small_icon",
                badge = 1,
                messageId = "aMessageId",
                customId = "aCustomId",
                action = NotificationAction.CREATED
            )
        )

        every { pgpCryptoMock.decryptText(any(), any()) } returns serialised
        // Given
        val useCase = DecryptNotificationContent(cryptoContext, userManager)

        // When
        val result = useCase.invoke(mockk(), UUID.randomUUID().toString())

        // Then
        assertEquals(expected, result.getOrNull()?.value)
    }
}
