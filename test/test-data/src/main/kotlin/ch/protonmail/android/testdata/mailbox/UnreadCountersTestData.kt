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

package ch.protonmail.android.testdata.mailbox

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import me.proton.core.label.domain.entity.LabelId

object UnreadCountersTestData {

    val labelToCounterMap = hashMapOf(
        SystemLabelId.Inbox.labelId to 1,
        SystemLabelId.Drafts.labelId to 2,
        SystemLabelId.Sent.labelId to 3,
        SystemLabelId.Starred.labelId to 4,
        SystemLabelId.Archive.labelId to 5,
        SystemLabelId.Spam.labelId to 6,
        SystemLabelId.Trash.labelId to 7,
        SystemLabelId.AllMail.labelId to 8
    )

    val systemUnreadCounters = labelToCounterMap.map { UnreadCounter(it.key, it.value) }

    fun List<UnreadCounter>.update(labelId: LabelId, count: Int) = map { unreadCounter ->
        if (unreadCounter.labelId == labelId) {
            unreadCounter.copy(count = count)
        } else {
            unreadCounter
        }
    }
}
