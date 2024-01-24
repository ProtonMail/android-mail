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

package ch.protonmail.android.mailcommon.domain.model

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class IntentShareInfo(
    val attachmentUris: List<String>,
    val emailSubject: String?,
    val emailRecipientTo: List<String>,
    val emailRecipientCc: List<String>,
    val emailRecipientBcc: List<String>,
    val emailBody: String?,
    val encoded: Boolean = false
) {

    companion object {

        val Empty = IntentShareInfo(
            attachmentUris = emptyList(),
            emailSubject = null,
            emailRecipientTo = emptyList(),
            emailRecipientCc = emptyList(),
            emailRecipientBcc = emptyList(),
            emailBody = null,
            encoded = false
        )
    }
}

fun IntentShareInfo.isNotEmpty(): Boolean = this != IntentShareInfo.Empty
fun IntentShareInfo.hasEmailData(): Boolean = emailSubject != null ||
    emailBody != null ||
    emailRecipientTo.isNotEmpty() ||
    emailRecipientCc.isNotEmpty() ||
    emailRecipientBcc.isNotEmpty()

/**
 * Without Base64 encoding, navigation graph fails to resolve the destination with the serialized form of this class.
 */
@OptIn(ExperimentalEncodingApi::class)
fun IntentShareInfo.encode(): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = attachmentUris.map { Base64.encode(it.toByteArray()) },
        emailSubject = emailSubject?.let { Base64.encode(it.toByteArray()) },
        emailRecipientTo = emailRecipientTo.map { Base64.encode(it.toByteArray()) },
        emailRecipientCc = emailRecipientCc.map { Base64.encode(it.toByteArray()) },
        emailRecipientBcc = emailRecipientBcc.map { Base64.encode(it.toByteArray()) },
        emailBody = emailBody?.let { Base64.encode(it.toByteArray()) },
        encoded = true
    )
}

@OptIn(ExperimentalEncodingApi::class)
fun IntentShareInfo.decode(): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = attachmentUris.map { String(Base64.decode(it)) },
        emailSubject = emailSubject?.let { String(Base64.decode(it)) },
        emailRecipientTo = emailRecipientTo.map { String(Base64.decode(it)) },
        emailRecipientCc = emailRecipientCc.map { String(Base64.decode(it)) },
        emailRecipientBcc = emailRecipientBcc.map { String(Base64.decode(it)) },
        emailBody = emailBody?.let { String(Base64.decode(it)) },
        encoded = false
    )
}
