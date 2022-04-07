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

package ch.protonmail.android.mailconversation.data.local

import androidx.room.TypeConverter
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize

class ConversationConverters {

    @TypeConverter
    fun fromConversationIdToString(value: ConversationId?): String? = value?.id

    @TypeConverter
    fun fromStringToConversationId(value: String?): ConversationId? = value?.let {
        ConversationId(value)
    }

    @TypeConverter
    fun fromListRecipientToString(value: List<Recipient>?) = value?.serialize()

    @TypeConverter
    fun fromStringToListRecipient(value: String?): List<Recipient>? = value?.deserialize()
}
