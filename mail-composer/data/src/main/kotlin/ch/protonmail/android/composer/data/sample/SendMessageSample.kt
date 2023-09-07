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

package ch.protonmail.android.composer.data.sample

import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.composer.data.usecase.GenerateSendMessagePackage
import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PackageType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object SendMessageSample {

    const val RecipientEmail = "recipient@email.com"

    val BodySessionKey = SessionKey("BodySessionKey".toByteArray())
    val MimeBodySessionKey = SessionKey("MimeBodySessionKey".toByteArray())
    val EncryptedBodyDataPacket = "EncryptedBodyDataPacket".toByteArray() as DataPacket
    val EncryptedMimeBodyDataPacket = "EncryptedMimeBodyDataPacket".toByteArray() as DataPacket
    val RecipientBodyKeyPacket = "RecipientBodyKeyPacket".toByteArray() as KeyPacket
    val SignedEncryptedMimeBody = Pair(
        "SignedEncryptedMimeBody KeyPacket".toByteArray() as KeyPacket,
        "SignedEncryptedMimeBody DataPacket".toByteArray() as DataPacket
    )
    val CleartextBodyKey = SendMessagePackage.Key(
        Base64.encode(BodySessionKey.key),
        GenerateSendMessagePackage.SessionKeyAlgorithm
    )
    val CleartextMimeBodyKey = SendMessagePackage.Key(
        Base64.encode(MimeBodySessionKey.key),
        GenerateSendMessagePackage.SessionKeyAlgorithm
    )

    val PublicKey = PublicKey("SendPreferences PublicKey", true, true, true, true)

    object SendPreferences {

        val ProtonMail = SendPreferences(
            encrypt = true,
            sign = true,
            PackageType.ProtonMail,
            mimeType = MimeType.PlainText,
            PublicKey
        )

        val ProtonMailWithEmptyPublicKey = ProtonMail.copy(publicKey = null)

        val PgpMime = ProtonMail.copy(pgpScheme = PackageType.PgpMime)

        val ClearMime = ProtonMail.copy(pgpScheme = PackageType.ClearMime, encrypt = false, sign = true)

        val Cleartext = ProtonMail.copy(pgpScheme = PackageType.Cleartext, encrypt = false, sign = false)
    }


}
