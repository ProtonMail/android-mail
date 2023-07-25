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
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.util.kotlin.toInt

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

fun MessageWithBody.toDraftMessageResource() = DraftMessageResource(
    subject = this.message.subject,
    this.message.unread.toInt(),
    with(this.message.sender) { RecipientResource(address, name) },
    this.message.toList.map { RecipientResource(it.address, it.name) },
    this.message.ccList.map { RecipientResource(it.address, it.name) },
    this.message.bccList.map { RecipientResource(it.address, it.name) },
    this.message.externalId,
    this.message.flags,
    this.messageBody.body,
    this.messageBody.mimeType.value
)
