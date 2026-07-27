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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.mapper.toMailboxFirstPageLoadingStatus
import ch.protonmail.android.mailmailbox.domain.model.MailboxFirstPageLoadingStatus
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class ObserveMailboxFirstPageLoadingStatus @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    operator fun invoke(): Flow<MailboxFirstPageLoadingStatus> {
        val conversationFlow: Flow<MailboxFirstPageLoadingStatus> =
            conversationRepository.observeScrollerFetchNewStatus()
                .mapNotNull { status -> status.toMailboxFirstPageLoadingStatus() }

        val messageFlow: Flow<MailboxFirstPageLoadingStatus> =
            messageRepository.observeScrollerFetchNewStatus()
                .mapNotNull { status -> status.toMailboxFirstPageLoadingStatus() }

        return merge(conversationFlow, messageFlow)
    }
}
