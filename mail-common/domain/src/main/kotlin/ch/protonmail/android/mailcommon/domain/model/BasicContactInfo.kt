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
data class BasicContactInfo(val contactName: String?, val contactEmail: String, val encoded: Boolean = false)

/**
 * Encodes the current [BasicContactInfo] instance's fields to [Base64.UrlSafe] strings.
 */
fun BasicContactInfo.encode(): BasicContactInfo {
    return BasicContactInfo(
        contactName = contactName?.toUrlSafeBase64String(),
        contactEmail = contactEmail.toUrlSafeBase64String(),
        encoded = true
    )
}

/**
 * Decodes the current [BasicContactInfo] instance's fields by using [Base64.UrlSafe] decoding.
 */
fun BasicContactInfo.decode(): BasicContactInfo {
    return BasicContactInfo(
        contactName = contactName?.fromUrlSafeBase64String(),
        contactEmail = contactEmail.fromUrlSafeBase64String(),
        encoded = false
    )
}
