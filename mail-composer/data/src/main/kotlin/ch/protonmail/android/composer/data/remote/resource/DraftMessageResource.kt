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

import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DraftMessageResource(
    @SerialName("Subject")
    val subject: String,
    @SerialName("Unread")
    val unread: Int,
    @SerialName("Sender")
    val sender: RecipientResource,
    @SerialName("ToList")
    val toList: List<RecipientResource>,
    @SerialName("CCList")
    val ccList: List<RecipientResource>,
    @SerialName("BCCList")
    val bccList: List<RecipientResource>,
    @SerialName("ExternalID")
    val externalId: String?,
    @SerialName("Flags")
    val flags: Long,
    @SerialName("Body")
    val body: String,
    @SerialName("MIMEType")
    val mimeType: String
)
