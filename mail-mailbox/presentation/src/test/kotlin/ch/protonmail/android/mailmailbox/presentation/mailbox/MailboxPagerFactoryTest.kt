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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import app.cash.turbine.test
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MailboxPagerFactoryTest {

    private val pagingSourceFactory = mockk<MailboxItemPagingSourceFactory>()

    private val mailboxPagerFactory = MailboxPagerFactory(pagingSourceFactory)

    @Test
    fun `pager content is returned from mailbox paging source factory`() = runTest {
        // Given
        val userIds = listOf(userId)
        val selectedMailLabelId = MailLabelId.System.Starred
        val filterUnread = false
        val type = MailboxItemType.Message
        val pager = mailboxPagerFactory.create(
            userIds,
            selectedMailLabelId,
            filterUnread,
            type
        )

        // When
        pager.flow.test {

            // Then
            verify {
                pagingSourceFactory.create(
                    userIds,
                    selectedMailLabelId,
                    filterUnread,
                    type
                )
            }
            cancelAndConsumeRemainingEvents()
        }
    }
}
