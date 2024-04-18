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

import ch.protonmail.android.mailcommon.domain.util.fromUrlSafeBase64String
import ch.protonmail.android.mailcommon.domain.util.toUrlSafeBase64String
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

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
 * Encodes the current [IntentShareInfo] instance's fields to [Base64.UrlSafe] strings.
 *
 * The current implementation relies on [Base64.UrlSafe] to avoid issues when passing these values as navigation
 * arguments, as if standard [Base64] encoding were to add forward slashes, navigation would make the app crash.
 */
fun IntentShareInfo.encode(): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = attachmentUris.map { it.toUrlSafeBase64String() },
        emailSubject = emailSubject?.toUrlSafeBase64String(),
        emailRecipientTo = emailRecipientTo.map { it.toUrlSafeBase64String() },
        emailRecipientCc = emailRecipientCc.map { it.toUrlSafeBase64String() },
        emailRecipientBcc = emailRecipientBcc.map { it.toUrlSafeBase64String() },
        emailBody = emailBody?.toUrlSafeBase64String(),
        encoded = true
    )
}

/**
 * Decodes the current [IntentShareInfo] instance's fields by using [Base64.UrlSafe] decoding.
 *
 * See [IntentShareInfo.encode] for further details.
 */
fun IntentShareInfo.decode(): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = attachmentUris.map { it.fromUrlSafeBase64String() },
        emailSubject = emailSubject?.fromUrlSafeBase64String(),
        emailRecipientTo = emailRecipientTo.map { it.fromUrlSafeBase64String() },
        emailRecipientCc = emailRecipientCc.map { it.fromUrlSafeBase64String() },
        emailRecipientBcc = emailRecipientBcc.map { it.fromUrlSafeBase64String() },
        emailBody = emailBody?.fromUrlSafeBase64String(),
        encoded = false
    )
}
