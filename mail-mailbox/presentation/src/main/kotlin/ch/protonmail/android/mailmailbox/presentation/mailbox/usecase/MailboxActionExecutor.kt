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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Runs a bulk/single-item action on a set of mailbox item ids, picking the conversation or message
 * use-case based on [ViewMode]. Centralises the otherwise-duplicated "conversation vs message" fork that
 * every action handler in [ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxViewModel] used
 * to repeat, and owns the matched conversation/message use-case pairs so the view model doesn't have to.
 */
class MailboxActionExecutor @Inject constructor(
    private val markConversationsAsRead: MarkConversationsAsRead,
    private val markMessagesAsRead: MarkMessagesAsRead,
    private val markConversationsAsUnread: MarkConversationsAsUnread,
    private val markMessagesAsUnread: MarkMessagesAsUnread,
    private val moveConversations: MoveConversations,
    private val moveMessages: MoveMessages,
    private val deleteConversations: DeleteConversations,
    private val deleteMessages: DeleteMessages,
    private val starConversations: StarConversations,
    private val starMessages: StarMessages,
    private val unStarConversations: UnStarConversations,
    private val unStarMessages: UnStarMessages
) {

    /** [labelId] scopes the conversation branch only — marking messages read is not location-scoped. */
    suspend fun markRead(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>,
        labelId: LabelId
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping -> markConversationsAsRead(userId, labelId, itemIds.toConversationIds())
        ViewMode.NoConversationGrouping -> markMessagesAsRead(userId, itemIds.toMessageIds())
    }

    /** [labelId] scopes the conversation branch only — marking messages unread is not location-scoped. */
    suspend fun markUnread(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>,
        labelId: LabelId
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping -> markConversationsAsUnread(userId, labelId, itemIds.toConversationIds())
        ViewMode.NoConversationGrouping -> markMessagesAsUnread(userId, itemIds.toMessageIds())
    }

    suspend fun move(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>,
        systemLabelId: SystemLabelId
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping ->
            moveConversations(userId, itemIds.toConversationIds(), systemLabelId = systemLabelId)
        ViewMode.NoConversationGrouping ->
            moveMessages(userId, itemIds.toMessageIds(), systemLabelId = systemLabelId)
    }

    /** [currentLabelId] applies to the message branch only — deleting conversations is not location-scoped. */
    suspend fun delete(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>,
        currentLabelId: LabelId
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping -> deleteConversations(userId, itemIds.toConversationIds())
        ViewMode.NoConversationGrouping -> deleteMessages(userId, itemIds.toMessageIds(), currentLabelId)
    }

    suspend fun star(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping -> starConversations(userId, itemIds.toConversationIds()).map { }
        ViewMode.NoConversationGrouping -> starMessages(userId, itemIds.toMessageIds())
    }

    suspend fun unStar(
        userId: UserId,
        viewMode: ViewMode,
        itemIds: List<String>
    ): Either<DataError, Unit> = when (viewMode) {
        ViewMode.ConversationGrouping -> unStarConversations(userId, itemIds.toConversationIds()).map { }
        ViewMode.NoConversationGrouping -> unStarMessages(userId, itemIds.toMessageIds())
    }

    private fun List<String>.toConversationIds() = map { ConversationId(it) }
    private fun List<String>.toMessageIds() = map { MessageId(it) }
}
