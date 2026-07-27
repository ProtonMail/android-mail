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
package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import io.mockk.mockk
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals

class SettledViewLatchTest {

    private val latch = SettledViewLatch()

    private val pagingItems = mockk<LazyPagingItems<MailboxItemUiModel>>()
    private val listData = MailboxScreenState.Data(pagingItems)

    private val inbox = MailboxLocation(
        userId = UserId("user-1"),
        labelId = "0",
        categoryId = "category-primary",
        isInSearch = false
    )

    @Test
    fun `should show the skeleton while the pager reloads for a location never settled before`() {
        // When
        val result = latch.update(MailboxScreenState.Loading, inbox, isPagerReloading = true)

        // Then
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should show the raw view once it stops loading`() {
        // Given
        latch.update(MailboxScreenState.Loading, inbox, isPagerReloading = true)

        // When
        val result = latch.update(MailboxScreenState.Empty, inbox, isPagerReloading = false)

        // Then
        assertEquals(MailboxScreenState.Empty, result)
    }

    @Test
    fun `should keep the empty state instead of flashing a skeleton on an in-place reload`() {
        // Given an empty mailbox that has settled
        settle(MailboxScreenState.Empty, inbox)

        // When the unread filter is toggled and the pager reloads in place
        val result = latch.update(MailboxScreenState.Loading, inbox, isPagerReloading = true)

        // Then
        assertEquals(MailboxScreenState.Empty, result)
    }

    @Test
    fun `should keep the no-results state instead of flashing a skeleton on an in-place reload`() {
        // Given
        settle(MailboxScreenState.SearchNoData, inbox.copy(isInSearch = true))

        // When
        val result = latch.update(
            MailboxScreenState.Loading,
            inbox.copy(isInSearch = true),
            isPagerReloading = true
        )

        // Then
        assertEquals(MailboxScreenState.SearchNoData, result)
    }

    @Test
    fun `should show the skeleton on a category switch even though the location changes before the pager`() {
        // Given a settled category
        settle(listData, inbox)

        // When the category tab flips: the location changes a frame before the pager reacts, so the raw
        // view still renders the previous category's items. See ET-6553.
        val social = inbox.copy(categoryId = "category-social")
        assertEquals(listData, latch.update(listData, social, isPagerReloading = false))

        // Then the skeleton is shown as soon as the pager starts reloading, not suppressed as an
        // in-place reload
        val result = latch.update(MailboxScreenState.Loading, social, isPagerReloading = true)
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should show the skeleton on a category switch between two empty categories`() {
        // Given a settled empty category
        settle(MailboxScreenState.Empty, inbox)

        // When switching to another category, whose emptiness is not known yet
        val social = inbox.copy(categoryId = "category-social")
        latch.update(MailboxScreenState.Empty, social, isPagerReloading = false)

        // Then
        val result = latch.update(MailboxScreenState.Loading, social, isPagerReloading = true)
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should show the skeleton when the account changes on the same label and category`() {
        // Given
        settle(MailboxScreenState.Empty, inbox)

        // When
        val otherAccount = inbox.copy(userId = UserId("user-2"))
        val result = latch.update(MailboxScreenState.Loading, otherAccount, isPagerReloading = true)

        // Then
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should show the skeleton when entering search on the same label and category`() {
        // Given
        settle(MailboxScreenState.Empty, inbox)

        // When
        val searching = inbox.copy(isInSearch = true)
        val result = latch.update(MailboxScreenState.Loading, searching, isPagerReloading = true)

        // Then
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should not hold a view that renders live list content`() {
        // Given a settled non-empty mailbox
        settle(listData, inbox)

        // When a reload drops the presented items, so the raw view becomes Loading
        val result = latch.update(MailboxScreenState.Loading, inbox, isPagerReloading = true)

        // Then the skeleton is shown rather than an emptied list
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should never hold anything while no location is known`() {
        // When the list state is not Data yet
        val result = latch.update(MailboxScreenState.Loading, location = null, isPagerReloading = false)

        // Then
        assertEquals(MailboxScreenState.Loading, result)
    }

    @Test
    fun `should show errors through instead of holding the settled view`() {
        // Given
        settle(MailboxScreenState.Empty, inbox)

        // When
        val result = latch.update(MailboxScreenState.Offline, inbox, isPagerReloading = false)

        // Then
        assertEquals(MailboxScreenState.Offline, result)
    }

    @Test
    fun `should stop holding the previous view once the new one settles`() {
        // Given an empty mailbox held across a reload
        settle(MailboxScreenState.Empty, inbox)
        assertEquals(
            MailboxScreenState.Empty,
            latch.update(MailboxScreenState.Loading, inbox, isPagerReloading = true)
        )

        // When the reload brings in items
        val result = latch.update(listData, inbox, isPagerReloading = false)

        // Then
        assertEquals(listData, result)
    }

    /** Drives the latch through a full reload cycle so [view] becomes the settled view of [location]. */
    private fun settle(view: MailboxScreenState, location: MailboxLocation) {
        latch.update(MailboxScreenState.Loading, location, isPagerReloading = true)
        latch.update(view, location, isPagerReloading = false)
    }
}
