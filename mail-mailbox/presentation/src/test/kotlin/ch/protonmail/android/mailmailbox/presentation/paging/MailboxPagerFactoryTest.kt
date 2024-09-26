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

package ch.protonmail.android.mailmailbox.presentation.paging

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelInProgressSignal
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MailboxPagerFactoryTest {

    private val remoteMediatorFactory = mockk<MailboxItemRemoteMediatorFactory>()
    private val pagingSourceFactory = mockk<MailboxItemPagingSourceFactory>()
    private val selectedMailLabelId = MailLabelId.System.Starred
    private val readStatus = ReadStatus.All
    private val pageKey = PageKey(filter = PageFilter(labelId = selectedMailLabelId.labelId, read = readStatus))

    private val emptyLabelInProgressSignal = mockk<EmptyLabelInProgressSignal>()
    private val mailboxPagerFactory = MailboxPagerFactory(pagingSourceFactory, remoteMediatorFactory)

    @Test
    fun `pager content is returned from mailbox paging source factory`() = runTest {
        // Given
        val userIds = listOf(userId)
        val type = MailboxItemType.Message
        val mailboxPageKey = MailboxPageKey(userIds, pageKey)
        // Mediator mock needs to be relaxed to avoid exception "Failed to transform androidx/mediator"
        every {
            remoteMediatorFactory.create(mailboxPageKey, type, emptyLabelInProgressSignal)
        } returns mockk(relaxed = true)
        // The test works without the explicit mock for pagingSourceFactory.create method, but it should
        // not! We assume that the pager constructor somehow swallows the "mockk - missing method mock" exceptions
        every { pagingSourceFactory.create(mailboxPageKey, type) } returns mockk(relaxed = true)

        // When
        val pager = mailboxPagerFactory.create(
            userIds = userIds,
            selectedMailLabelId = selectedMailLabelId,
            filterUnread = false,
            type = type,
            searchQuery = "",
            emptyLabelInProgressSignal = emptyLabelInProgressSignal
        )

        // Then
        pager.flow.test {
            verify { remoteMediatorFactory.create(mailboxPageKey, type, emptyLabelInProgressSignal) }
            verify { pagingSourceFactory.create(mailboxPageKey, type) }
            cancelAndConsumeRemainingEvents()
        }
    }
}
