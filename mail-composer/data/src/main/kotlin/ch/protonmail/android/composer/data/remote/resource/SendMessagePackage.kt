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

package ch.protonmail.android.composer.data.remote.resource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsettings.domain.entity.PackageType

@Serializable
data class SendMessagePackage(
    @SerialName("Addresses")
    val addresses: Map<Email, Address>,
    @SerialName("MIMEType")
    val mimeType: String,
    @SerialName("Body")
    val body: String,
    @SerialName("Type")
    val type: Int, // the package global type is a logical OR of the types of all the addresses for this package
    @SerialName("BodyKey")
    val bodyKey: Key? = null, // include only if there are cleartext recipients
    @SerialName("AttachmentKeys") // a map of an attachment id and a session key
    val attachmentKeys: Map<String, Key>? = null // include only if there are cleartext recipients
) {

    /**
     * Structure representing payload for each recipient, depending on its type.
     */
    @Serializable
    sealed class Address(
        @SerialName("Type")
        val type: Int
    ) {

        @Serializable
        data class Internal(
            @SerialName("Signature")
            val signature: Int,
            @SerialName("BodyKeyPacket")
            val bodyKeyPacket: String,
            @SerialName("AttachmentKeyPackets")
            val attachmentKeyPackets: Map<String, String>
        ) : Address(PackageType.ProtonMail.type)

        @Serializable
        data class ExternalEncrypted(
            @SerialName("Signature")
            val signature: Int,
            @SerialName("BodyKeyPacket")
            val bodyKeyPacket: String
        ) : Address(PackageType.PgpMime.type)

        @Serializable
        data class ExternalSigned(
            @SerialName("Signature")
            val signature: Int
        ) : Address(PackageType.ClearMime.type)

        @Serializable
        data class ExternalCleartext(
            @SerialName("Signature")
            val signature: Int
        ) : Address(PackageType.Cleartext.type)

        @Serializable
        data class EncryptedOutside(
            @SerialName("BodyKeyPacket")
            val bodyKeyPacket: String,
            @SerialName("AttachmentKeyPackets")
            val attachmentKeyPackets: Map<String, String>,
            @SerialName("Token")
            val token: String,
            @SerialName("EncToken")
            val encToken: String,
            @SerialName("Auth")
            val auth: Auth,
            @SerialName("PasswordHint")
            val passwordHint: String?,
            @SerialName("Signature")
            val signature: Int
        ) : Address(PackageType.EncryptedOutside.type)
    }

    @Serializable
    data class Key(
        @SerialName("Key")
        val key: String,
        @SerialName("Algorithm")
        val algorithm: String
    )

    @Serializable
    data class Auth(
        @SerialName("ModulusID")
        val modulusId: String,
        @SerialName("Version")
        val version: Int,
        @SerialName("Salt")
        val salt: String,
        @SerialName("Verifier")
        val verifier: String
    )

}
