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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Observes unread counters for messages and conversations.
 *
 * The returned counters should be ready to use: considerations on which "counter" to
 * use for each "label" should be done here (ie. locations that always show Messages
 * such as `[All-]Draft` / `[All-]Sent` should use "Message counters" independently
 * from the view mode
 */
class ObserveUnreadCounters @Inject constructor(
    private val countersRepository: UnreadCountRepository
) {

    operator fun invoke(userId: UserId): Flow<List<UnreadCounter>> {
        return countersRepository.observeUnreadCount(userId).mapLatest {
            it.messagesUnreadCount
        }
    }

}
