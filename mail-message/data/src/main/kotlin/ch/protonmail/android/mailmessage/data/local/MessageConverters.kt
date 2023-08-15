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

package ch.protonmail.android.mailmessage.data.local

import androidx.room.TypeConverter
import ch.protonmail.android.mailmessage.data.local.entity.AttachmentCountEntity
import ch.protonmail.android.mailmessage.data.local.entity.MimeTypeEntity
import ch.protonmail.android.mailmessage.data.local.entity.UnsubscribeMethodsEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
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
    fun fromOrderByToString(value: OrderBy?): String? = value?.name

    @TypeConverter
    fun fromStringToOrderBy(value: String?): OrderBy? = value?.let { OrderBy.valueOf(it) }

    @TypeConverter
    fun fromReadStatusToString(value: ReadStatus?): String? = value?.name

    @TypeConverter
    fun fromStringToReadStatus(value: String?): ReadStatus? = value?.let { ReadStatus.valueOf(it) }

    @TypeConverter
    fun fromAttachmentCountToString(value: AttachmentCountEntity?) = value?.serialize()

    @TypeConverter
    fun fromStringToAttachmentCount(value: String?): AttachmentCountEntity? = value?.deserialize()

    @TypeConverter
    fun fromParticipantToString(value: Participant?): String? = value?.serialize()

    @TypeConverter
    fun fromStringToParticipant(value: String?): Participant? = value?.deserialize()

    @TypeConverter
    fun fromUnsubscribeMethodsToString(value: UnsubscribeMethodsEntity?): String? = value?.serialize()

    @TypeConverter
    fun fromStringToUnsubscribeMethods(value: String?): UnsubscribeMethodsEntity? = value?.deserialize()

    @TypeConverter
    fun fromMimeTypeToString(value: MimeTypeEntity?): String? = value?.value

    @TypeConverter
    fun fromStringToMimeType(value: String?): MimeTypeEntity? = value?.let { MimeTypeEntity.from(it) }
}
