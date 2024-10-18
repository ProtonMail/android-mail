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

package ch.protonmail.android.mailmessage.domain.model

import ch.protonmail.android.mailmessage.domain.model.attachments.header.HeaderValue
import me.proton.core.util.kotlin.equalsNoCase

data class AttachmentId(val id: String)

data class MessageAttachment(
    val attachmentId: AttachmentId,
    val name: String,
    val size: Long,
    val mimeType: String,
    val disposition: String?,
    val keyPackets: String?,
    val signature: String?,
    val encSignature: String?,
    val headers: Map<String, HeaderValue>
) {
    fun isCalendarAttachment(): Boolean = mimeType.lowercase().split(";").any { it.contains("text/calendar") }

    /**
     * If Content-Type is binary and we can guess the proper mimeType from file extension,
     * we apply it to [MessageAttachment].
     */
    fun fixBinaryContentTypes(): MessageAttachment {

        val extension = this.name.split(".").last()

        return if (mimeType.equalsNoCase("application/octet-stream")) {
            if (extension.equalsNoCase("pdf")) {
                this.copy(
                    mimeType = "application/pdf"
                )
            } else this
        } else this
    }
}
