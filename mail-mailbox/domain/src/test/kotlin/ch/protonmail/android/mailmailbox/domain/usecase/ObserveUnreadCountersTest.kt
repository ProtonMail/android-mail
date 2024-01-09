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

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounters
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountersRepository
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.ViewMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveUnreadCountersTest {

    private val repository = mockk<UnreadCountersRepository>()
    private val observeCurrentViewMode = mockk<ObserveCurrentViewMode>()

    private val observeUnreadCounters = ObserveUnreadCounters(repository, observeCurrentViewMode)

    @Test
    fun `when view mode is message mode return the messages counters`() = runTest {
        // Given
        every { repository.observeUnreadCounters(userId) } returns flowOf(
            UnreadCounters(emptyList(), messageUnreadCounters)
        )
        every { observeCurrentViewMode(userId) } returns flowOf(ViewMode.NoConversationGrouping)

        // When
        observeUnreadCounters(userId).test {
            // Then
            val actual = awaitItem()
            assertTrue(messageUnreadCounters.containsAll(actual))
            assertEquals(messageUnreadCounters.count(), actual.count())
            awaitComplete()
        }
    }

    @Test
    fun `when view mode is conversation mode return the conversation counters except for message-only labels`() =
        runTest {
            // Given
            val expected = conversationUnreadCounters.toMutableList()
            expected.removeAll { it.labelId in listOf(SystemLabelId.Sent.labelId, SystemLabelId.Drafts.labelId) }
            expected.add(UnreadCounter(SystemLabelId.Drafts.labelId, 0))
            expected.add(UnreadCounter(SystemLabelId.Sent.labelId, 0))
            every { repository.observeUnreadCounters(userId) } returns flowOf(
                UnreadCounters(conversationUnreadCounters, messageUnreadCounters)
            )
            every { observeCurrentViewMode(userId) } returns flowOf(ViewMode.ConversationGrouping)

            // When
            observeUnreadCounters(userId).test {
                // Then
                val actual = awaitItem()
                assertTrue(actual.containsAll(expected))
                assertEquals(actual.count(), expected.count())
                awaitComplete()
            }
        }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val messageUnreadCounters = listOf(
            UnreadCounter(SystemLabelId.Inbox.labelId, 2),
            UnreadCounter(SystemLabelId.Archive.labelId, 7),
            UnreadCounter(SystemLabelId.Drafts.labelId, 0),
            UnreadCounter(SystemLabelId.Sent.labelId, 0),
            UnreadCounter(SystemLabelId.Trash.labelId, 1),
            UnreadCounter(SystemLabelId.AllMail.labelId, 10),
            UnreadCounter(LabelId("custom-label"), 0),
            UnreadCounter(LabelId("custom-folder"), 0)
        )

        val conversationUnreadCounters = listOf(
            UnreadCounter(SystemLabelId.Inbox.labelId, 1),
            UnreadCounter(SystemLabelId.Archive.labelId, 2),
            UnreadCounter(SystemLabelId.Drafts.labelId, 1),
            UnreadCounter(SystemLabelId.Sent.labelId, 0),
            UnreadCounter(SystemLabelId.Trash.labelId, 1),
            UnreadCounter(SystemLabelId.AllMail.labelId, 6),
            UnreadCounter(LabelId("custom-label"), 0),
            UnreadCounter(LabelId("custom-folder"), 1)
        )
    }
}
