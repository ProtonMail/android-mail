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

package ch.protonmail.android.mailmessage.data.remote.resource

import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttachmentsInfoResource(
    @SerialName("text/calendar")
    val ics: AttachmentCountsResource? = null,
    @SerialName("application/ics")
    val applicationIcs: AttachmentCountsResource? = null
)

/**
 * Represents the count of attachments for the parent mime type.
 * For images, differentiates between 'inline' and 'attached' disposition.
 */
@Serializable
data class AttachmentCountsResource(
    @SerialName("attachment")
    val attachedCount: Int? = 0,
    @SerialName("inline")
    val inlineCount: Int? = 0
)

fun AttachmentsInfoResource?.toAttachmentsCount(): AttachmentCount {
    val icsCount = this?.ics?.attachedCount ?: 0
    val appIcsCount = this?.applicationIcs?.attachedCount ?: 0
    return AttachmentCount(calendar = icsCount + appIcsCount)
}
