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

package ch.protonmail.android.composer.data.local.converters

import androidx.room.TypeConverter
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.SendingError
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize

class DraftStateConverters {

    @TypeConverter
    fun fromStringToDraftAction(value: String?): DraftAction? = value?.deserialize()

    @TypeConverter
    fun fromDraftActionToString(value: DraftAction?): String? = value?.serialize()

    @TypeConverter
    fun fromStateToInt(state: DraftSyncState?): Int? = state?.value

    @TypeConverter
    fun fromIntToDraftState(value: Int?): DraftSyncState? = value?.let { DraftSyncState.from(it) }

    @TypeConverter
    fun fromStringToSendingError(value: String?): SendingError? = value?.deserialize()

    @TypeConverter
    fun fromSendingErrorToString(value: SendingError?): String? = value?.serialize()

}
