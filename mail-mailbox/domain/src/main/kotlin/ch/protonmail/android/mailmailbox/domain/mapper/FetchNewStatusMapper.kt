/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.domain.mapper

import ch.protonmail.android.mailconversation.domain.model.ConversationScrollerFetchNewStatus
import ch.protonmail.android.mailmailbox.domain.model.MailboxFetchNewStatus
import ch.protonmail.android.mailmailbox.domain.model.MailboxFirstPageLoadingStatus
import ch.protonmail.android.mailmailbox.domain.model.ScrollerType
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus

fun ConversationScrollerFetchNewStatus.toMailboxFetchNewStatus(): MailboxFetchNewStatus? {
    return when (this) {
        is ConversationScrollerFetchNewStatus.FetchNewStarted ->
            MailboxFetchNewStatus.Started(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Conversation
            )

        is ConversationScrollerFetchNewStatus.FetchNewEnded ->
            MailboxFetchNewStatus.Ended(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Conversation
            )

        is ConversationScrollerFetchNewStatus.FirstPageLoadingStarted,
        is ConversationScrollerFetchNewStatus.FirstPageLoadingEnded -> null
    }
}

fun MessageScrollerFetchNewStatus.toMailboxFetchNewStatus(): MailboxFetchNewStatus? {
    return when (this) {
        is MessageScrollerFetchNewStatus.FetchNewStarted ->
            MailboxFetchNewStatus.Started(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Message
            )

        is MessageScrollerFetchNewStatus.FetchNewEnded ->
            MailboxFetchNewStatus.Ended(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Message
            )

        is MessageScrollerFetchNewStatus.FirstPageLoadingStarted,
        is MessageScrollerFetchNewStatus.FirstPageLoadingEnded -> null
    }
}

fun ConversationScrollerFetchNewStatus.toMailboxFirstPageLoadingStatus(): MailboxFirstPageLoadingStatus? {
    return when (this) {
        is ConversationScrollerFetchNewStatus.FirstPageLoadingStarted ->
            MailboxFirstPageLoadingStatus.Started(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Conversation
            )

        is ConversationScrollerFetchNewStatus.FirstPageLoadingEnded ->
            MailboxFirstPageLoadingStatus.Ended(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Conversation
            )

        is ConversationScrollerFetchNewStatus.FetchNewStarted,
        is ConversationScrollerFetchNewStatus.FetchNewEnded -> null
    }
}

fun MessageScrollerFetchNewStatus.toMailboxFirstPageLoadingStatus(): MailboxFirstPageLoadingStatus? {
    return when (this) {
        is MessageScrollerFetchNewStatus.FirstPageLoadingStarted ->
            MailboxFirstPageLoadingStatus.Started(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Message
            )

        is MessageScrollerFetchNewStatus.FirstPageLoadingEnded ->
            MailboxFirstPageLoadingStatus.Ended(
                timestampMs = this.timestampMs,
                scrollerType = ScrollerType.Message
            )

        is MessageScrollerFetchNewStatus.FetchNewStarted,
        is MessageScrollerFetchNewStatus.FetchNewEnded -> null
    }
}
