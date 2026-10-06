/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalSearchMatchPosition
import ch.protonmail.android.mailmessage.domain.model.SearchMatchField
import ch.protonmail.android.mailmessage.domain.model.SearchMatchPosition

/** Null for fields outside the message metadata, such as the body. */
fun LocalSearchMatchPosition.toSearchMatchPosition(): SearchMatchPosition? {
    val field = when (attribute) {
        "subject" -> SearchMatchField.Subject
        "from" -> SearchMatchField.From
        "to" -> SearchMatchField.To
        "cc" -> SearchMatchField.Cc
        "bcc" -> SearchMatchField.Bcc
        else -> return null
    }
    return SearchMatchPosition(
        field = field,
        valueIndex = valueIndex.toInt(),
        start = start.toInt(),
        end = end.toInt()
    )
}
