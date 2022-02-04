/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data.local

import androidx.room.TypeConverter
import ch.protonmail.android.mailpagination.domain.entity.OrderBy
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import kotlinx.serialization.json.JsonElement
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize

class MessageConverters {

    @TypeConverter
    fun fromMessageIdToString(value: MessageId?): String? = value?.id

    @TypeConverter
    fun fromStringToMessageId(value: String?): MessageId? = value?.let { MessageId(value) }

    @TypeConverter
    fun fromAttachmentIdToString(value: AttachmentId?): String? = value?.id

    @TypeConverter
    fun fromStringToAttachmentId(value: String?): AttachmentId? = value?.let { AttachmentId(value) }

    @TypeConverter
    fun fromListRecipientToString(value: List<Recipient>?) = value?.serialize()

    @TypeConverter
    fun fromStringToListRecipient(value: String?): List<Recipient>? = value?.deserialize()

    @TypeConverter
    fun fromMapStringJsonElementToString(value: Map<String, JsonElement>?) = value?.serialize()

    @TypeConverter
    fun fromStringToMapStringJsonElement(value: String?): Map<String, JsonElement>? = value?.deserialize()

    @TypeConverter
    fun fromOrderByToString(value: OrderBy?): String? = value?.name

    @TypeConverter
    fun fromStringToOrderBy(value: String?): OrderBy? = value?.let { OrderBy.valueOf(it) }

    @TypeConverter
    fun fromReadStatusToString(value: ReadStatus?): String? = value?.name

    @TypeConverter
    fun fromStringToReadStatus(value: String?): ReadStatus? = value?.let { ReadStatus.valueOf(it) }
}
