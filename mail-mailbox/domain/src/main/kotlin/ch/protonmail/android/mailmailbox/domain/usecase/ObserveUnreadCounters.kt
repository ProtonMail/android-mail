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

import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounters
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
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
    private val countersRepository: UnreadCountRepository,
    private val observeMailFeature: ObserveMailFeature,
    private val observeCurrentViewMode: ObserveCurrentViewMode
) {

    suspend operator fun invoke(userId: UserId): Flow<List<UnreadCounter>> {
        val showCountersFeature = observeMailFeature(userId, MailFeatureId.ShowUnreadCounters).firstOrNull()
        if (showCountersFeature?.value == false) {
            return flowOf(emptyList())
        }

        return observeCurrentViewMode(userId)
            .flatMapLatest { viewMode ->
                countersRepository.observeUnreadCount(userId).mapLatest { unreadCounters ->
                    val counters = getCountersForViewMode(viewMode, unreadCounters)
                    replaceCountersForMessageOnlyLocations(counters, unreadCounters)
                }
            }
    }

    private fun replaceCountersForMessageOnlyLocations(
        counters: List<UnreadCounter>,
        unreadCounters: UnreadCounters
    ): List<UnreadCounter> = counters.toMutableList().run {
        removeAll { it.labelId in MessageOnlyLabelIds.messagesOnlyLabelsIds }
        val messageOnlyCounters = unreadCounters.messagesUnreadCount.filter {
            it.labelId in MessageOnlyLabelIds.messagesOnlyLabelsIds
        }
        addAll(messageOnlyCounters)
        this
    }

    private fun getCountersForViewMode(viewMode: ViewMode, unreadCounters: UnreadCounters) =
        if (viewMode == ViewMode.ConversationGrouping) {
            unreadCounters.conversationsUnreadCount
        } else {
            unreadCounters.messagesUnreadCount
        }

}
