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

import java.io.StringWriter
import arrow.core.Either
import arrow.core.continuations.either
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSource
import ch.protonmail.android.composer.data.remote.resource.SendMessageBody
import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import com.github.mangstadt.vinnie.io.FoldedLineWriter
import kotlinx.coroutines.flow.first
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.UnlockedKey
import me.proton.core.crypto.common.pgp.dataPacket
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.crypto.common.pgp.keyPacket
import me.proton.core.crypto.common.pgp.split
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptMimeMessage
import me.proton.core.key.domain.decryptSessionKey
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.encryptAndSignText
import me.proton.core.key.domain.useKeys
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.filterNullValues
import me.proton.core.util.kotlin.filterValues
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

internal class SendMessage @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val resolveUserAddress: ResolveUserAddress,
    private val cryptoContext: CryptoContext,
    private val findLocalDraft: FindLocalDraft,
    private val obtainSendPreferences: ObtainSendPreferences,
    private val generateSendMessagePackage: GenerateSendMessagePackage,
    private val observeMailSettings: ObserveMailSettings
) {

    /**
     * Because we ignore versioning conflicts between different clients, we assume that by the time this is called,
     * local draft has been correctly uploaded to backend and we will get the final version from DB here. Draft
     * should also be locked for editing by now.
     */
    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {

        val localDraft = findLocalDraft(userId, messageId) ?: shift(DataError.Local.NoDataCached)

        val senderAddress = resolveUserAddress(userId, localDraft.message.addressId)
            .mapLeft { DataError.Local.NoDataCached }
            .bind()

        val autoSaveContacts = observeMailSettings(userId).first()?.autoSaveContacts ?: false

        val recipients = localDraft.message.toList + localDraft.message.ccList + localDraft.message.bccList

        val sendPreferences = obtainSendPreferences(userId, recipients.map { it.address })
            .filterValues(ObtainSendPreferences.Result.Success::class.java)
            .mapValues { it.value.sendPreferences }

        val messagePackages = generateMessagePackages(senderAddress, localDraft, sendPreferences)

        val response = messageRemoteDataSource.send(
            userId,
            localDraft.message.messageId.id,
            SendMessageBody(
                autoSaveContacts = autoSaveContacts.toInt(),
                packages = messagePackages
            )
        )

        response.onLeft {
            Timber.e("API error sending message ID: $messageId", it)
        }.onRight {
            Timber.d("Success sending message ID: $messageId")
        }
    }

    private fun generateMessagePackages(
        senderAddress: UserAddress,
        localDraft: MessageWithBody,
        sendPreferences: Map<Email, SendPreferences>
    ): List<SendMessagePackage> {
        lateinit var decryptedPlaintextBodySessionKey: SessionKey
        lateinit var encryptedPlaintextBodyDataPacket: ByteArray

        lateinit var decryptedMimeBodySessionKey: SessionKey
        lateinit var encryptedMimeBodyDataPacket: ByteArray

        // Map<Email, Pair<KeyPacket, DataPacket>>
        lateinit var signedAndEncryptedMimeBodyForRecipients: Map<Email, Pair<ByteArray, ByteArray>>

        senderAddress.useKeys(cryptoContext) {

            val encryptedBodyPgpMessage = localDraft.messageBody.body

            // Decrypt body's session key to send it for plaintext recipients.
            val encryptedPlaintextBodySplit = encryptedBodyPgpMessage.split(cryptoContext.pgpCrypto)
            decryptedPlaintextBodySessionKey = decryptSessionKey(encryptedPlaintextBodySplit.keyPacket())
            encryptedPlaintextBodyDataPacket = encryptedPlaintextBodySplit.dataPacket()

            // generate MIME version of the email
            val decryptedBody = if (localDraft.messageBody.mimeType == MimeType.MultipartMixed) {
                decryptMimeMessage(encryptedBodyPgpMessage).body.content
            } else {
                decryptText(encryptedBodyPgpMessage)
            }

            val plaintextMimeBody = generateMimeBody(decryptedBody)

            // Encrypt and sign, then decrypt MIME body's session key to send it for plaintext recipients.
            val encryptedMimeBodySplit = encryptAndSignText(plaintextMimeBody).split(cryptoContext.pgpCrypto)
            decryptedMimeBodySessionKey = decryptSessionKey(encryptedMimeBodySplit.keyPacket())
            encryptedMimeBodyDataPacket = encryptedMimeBodySplit.dataPacket()

            this.privateKeyRing.unlockedPrimaryKey.unlockedKey.use { unlockedPrimaryKey ->
                signedAndEncryptedMimeBodyForRecipients = sendPreferences.mapValues { entry ->
                    signAndEncryptMimeBody(entry, plaintextMimeBody, unlockedPrimaryKey)
                }.filterNullValues()
            }
        }

        return sendPreferences.map { entry ->
            generateSendMessagePackage(
                entry.key,
                entry.value,
                decryptedPlaintextBodySessionKey,
                encryptedPlaintextBodyDataPacket,
                decryptedMimeBodySessionKey,
                encryptedMimeBodyDataPacket,
                signedAndEncryptedMimeBodyForRecipients[entry.key],
                emptyList() // waiting for attachments
            )
        }.filterNotNull()
    }

    /**
     * @return nullable Pair<KeyPacket, DataPacket>
     */
    private fun signAndEncryptMimeBody(
        entry: Map.Entry<Email, SendPreferences>,
        plaintextMimeBody: String,
        unlockedPrimaryKey: UnlockedKey
    ): Pair<ByteArray, ByteArray>? {
        return with(entry.value) {
            if (encrypt && pgpScheme != PackageType.ProtonMail && publicKey != null) {
                publicKey?.let {
                    try {
                        val split = cryptoContext.pgpCrypto.encryptAndSignText(
                            plaintextMimeBody,
                            it.key,
                            unlockedPrimaryKey.value
                        ).split(cryptoContext.pgpCrypto)
                        Pair(split.keyPacket(), split.dataPacket())
                    } catch (e: CryptoException) {
                        Timber.e("Exception encrypting and signing MIME body for recipient", e)
                        null
                    }
                }
            } else null
        }
    }

    /**
     * Correctly encodes and formats plaintext Message body in multipart/mixed content type.
     */
    @Suppress("ImplicitDefaultLocale")
    private fun generateMimeBody(body: String): String {

        val bytes = ByteArray(16)
        Random.nextBytes(bytes)
        val boundaryHex = bytes.joinToString("") {
            String.format("%02x", it)
        }

        val boundary = "---------------------$boundaryHex"

        val stringWriter = StringWriter()
        FoldedLineWriter(stringWriter).use {
            it.write(body, true, Charsets.UTF_8)
        }
        val quotedPrintableBody = stringWriter.toString()

        return """
            Content-Type: multipart/mixed; boundary=${boundary.substring(2)}
            
            $boundary
            Content-Transfer-Encoding: quoted-printable
            Content-Type: text/plain; charset=utf-8
            
            $quotedPrintableBody
            $boundary--
        """.trimIndent()
    }
}
