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

package ch.protonmail.android.mailconversation.data.local.converters

import androidx.room.TypeConverter
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.domain.model.Participant
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
    fun fromParticipantListToString(value: List<Participant>?) = value?.serialize()

    @TypeConverter
    fun fromStringToParticipantList(value: String?): List<Participant>? = value?.deserialize()
}
