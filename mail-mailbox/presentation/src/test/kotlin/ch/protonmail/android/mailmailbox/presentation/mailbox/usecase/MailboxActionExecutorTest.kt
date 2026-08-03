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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class MailboxActionExecutorTest {

    private val markConversationsAsRead = mockk<MarkConversationsAsRead>()
    private val markMessagesAsRead = mockk<MarkMessagesAsRead>()
    private val markConversationsAsUnread = mockk<MarkConversationsAsUnread>()
    private val markMessagesAsUnread = mockk<MarkMessagesAsUnread>()
    private val moveConversations = mockk<MoveConversations>()
    private val moveMessages = mockk<MoveMessages>()
    private val deleteConversations = mockk<DeleteConversations>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val starConversations = mockk<StarConversations>()
    private val starMessages = mockk<StarMessages>()
    private val unStarConversations = mockk<UnStarConversations>()
    private val unStarMessages = mockk<UnStarMessages>()

    private val executor = MailboxActionExecutor(
        markConversationsAsRead = markConversationsAsRead,
        markMessagesAsRead = markMessagesAsRead,
        markConversationsAsUnread = markConversationsAsUnread,
        markMessagesAsUnread = markMessagesAsUnread,
        moveConversations = moveConversations,
        moveMessages = moveMessages,
        deleteConversations = deleteConversations,
        deleteMessages = deleteMessages,
        starConversations = starConversations,
        starMessages = starMessages,
        unStarConversations = unStarConversations,
        unStarMessages = unStarMessages
    )

    private val userId = UserId("user")
    private val labelId = LabelId("label")
    private val ids = listOf("1", "2")
    private val conversationIds = ids.map { ConversationId(it) }
    private val messageIds = ids.map { MessageId(it) }

    @Test
    fun `mark read routes to conversations with the label when grouping conversations`() = runTest {
        coEvery { markConversationsAsRead(userId, labelId, conversationIds) } returns Unit.right()

        executor.markRead(userId, ViewMode.ConversationGrouping, ids, labelId)

        coVerify(exactly = 1) { markConversationsAsRead(userId, labelId, conversationIds) }
    }

    @Test
    fun `mark read routes to messages when not grouping conversations`() = runTest {
        coEvery { markMessagesAsRead(userId, messageIds) } returns Unit.right()

        executor.markRead(userId, ViewMode.NoConversationGrouping, ids, labelId)

        coVerify(exactly = 1) { markMessagesAsRead(userId, messageIds) }
    }

    @Test
    fun `mark unread routes by view mode`() = runTest {
        coEvery { markConversationsAsUnread(userId, labelId, conversationIds) } returns Unit.right()
        coEvery { markMessagesAsUnread(userId, messageIds) } returns Unit.right()

        executor.markUnread(userId, ViewMode.ConversationGrouping, ids, labelId)
        executor.markUnread(userId, ViewMode.NoConversationGrouping, ids, labelId)

        coVerify(exactly = 1) { markConversationsAsUnread(userId, labelId, conversationIds) }
        coVerify(exactly = 1) { markMessagesAsUnread(userId, messageIds) }
    }

    @Test
    fun `move routes to the system-label overload by view mode`() = runTest {
        val dest = SystemLabelId.Archive
        coEvery { moveConversations(userId, conversationIds, systemLabelId = dest) } returns Unit.right()
        coEvery { moveMessages(userId, messageIds, systemLabelId = dest) } returns Unit.right()

        executor.move(userId, ViewMode.ConversationGrouping, ids, dest)
        executor.move(userId, ViewMode.NoConversationGrouping, ids, dest)

        coVerify(exactly = 1) { moveConversations(userId, conversationIds, systemLabelId = dest) }
        coVerify(exactly = 1) { moveMessages(userId, messageIds, systemLabelId = dest) }
    }

    @Test
    fun `delete passes the current label only for messages`() = runTest {
        coEvery { deleteConversations(userId, conversationIds) } returns Unit.right()
        coEvery { deleteMessages(userId, messageIds, labelId) } returns Unit.right()

        executor.delete(userId, ViewMode.ConversationGrouping, ids, labelId)
        executor.delete(userId, ViewMode.NoConversationGrouping, ids, labelId)

        coVerify(exactly = 1) { deleteConversations(userId, conversationIds) }
        coVerify(exactly = 1) { deleteMessages(userId, messageIds, labelId) }
    }

    @Test
    fun `star routes by view mode and discards the conversation result`() = runTest {
        coEvery { starConversations(userId, conversationIds) } returns emptyList<Conversation>().right()
        coEvery { starMessages(userId, messageIds) } returns Unit.right()

        executor.star(userId, ViewMode.ConversationGrouping, ids)
        executor.star(userId, ViewMode.NoConversationGrouping, ids)

        coVerify(exactly = 1) { starConversations(userId, conversationIds) }
        coVerify(exactly = 1) { starMessages(userId, messageIds) }
    }

    @Test
    fun `unstar routes by view mode and discards the conversation result`() = runTest {
        coEvery { unStarConversations(userId, conversationIds) } returns emptyList<Conversation>().right()
        coEvery { unStarMessages(userId, messageIds) } returns Unit.right()

        executor.unStar(userId, ViewMode.ConversationGrouping, ids)
        executor.unStar(userId, ViewMode.NoConversationGrouping, ids)

        coVerify(exactly = 1) { unStarConversations(userId, conversationIds) }
        coVerify(exactly = 1) { unStarMessages(userId, messageIds) }
    }
}
